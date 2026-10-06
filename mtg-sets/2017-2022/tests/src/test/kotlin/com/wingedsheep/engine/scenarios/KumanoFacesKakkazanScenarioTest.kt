package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.KumanoFacesKakkazan
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Kumano Faces Kakkazan // Etching of Kumano — {R} Saga (NEO).
 *
 *   I   — 1 damage to each opponent and each planeswalker they control.
 *   II  — your next creature spell this turn enters with an additional +1/+1 counter.
 *   III — exile it, return it transformed: Etching of Kumano, 2/2 haste, "If a creature dealt
 *         damage this turn by a source you controlled would die, exile it instead."
 *
 * Walks the Saga through all three chapters and then pins Etching's replacement — the
 * `wasDealtDamageBySourceYouControlledThisTurn` predicate — against a creature killed by your burn
 * spell (exiled) and one killed by an opponent's (dies normally).
 */
class KumanoFacesKakkazanScenarioTest : FunSpec({

    val walker = card("Kumano Test Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 3
        oracleText = ""
    }

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(KumanoFacesKakkazan, walker))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.counters(id: EntityId, type: CounterType): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard++ < 50) {
            if (state.pendingDecision != null) autoResolveDecision() else bothPass()
        }
    }

    fun GameTestDriver.advanceUntil(maxSteps: Int = 2000, predicate: GameTestDriver.() -> Boolean) {
        var guard = 0
        while (guard++ < maxSteps && !predicate()) {
            when {
                state.pendingDecision != null -> autoResolveDecision()
                state.priorityPlayerId != null -> {
                    autoSubmitCombatDeclarationIfNeeded()
                    passPriority(state.priorityPlayerId!!)
                }
            }
        }
    }

    fun GameTestDriver.castSaga(me: EntityId) {
        val saga = putCardInHand(me, "Kumano Faces Kakkazan")
        giveMana(me, Color.RED, 1)
        castSpell(me, saga)
        resolveAll()
    }

    /** Cast the Saga and run it to its back face, stopping in your precombat main with priority. */
    fun GameTestDriver.reachEtching(me: EntityId): EntityId {
        castSaga(me)
        advanceUntil { findPermanent(me, "Etching of Kumano") != null }
        resolveAll()
        return findPermanent(me, "Etching of Kumano")!!
    }

    test("chapter I deals 1 damage to each opponent and each planeswalker they control") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val pw = driver.putPermanentOnBattlefield(opponent, "Kumano Test Walker")
        driver.replaceState(driver.state.updateEntity(pw) {
            it.with(CountersComponent(mapOf(CounterType.LOYALTY to 3)))
        })

        driver.castSaga(me)

        driver.getLifeTotal(opponent) shouldBe 19
        driver.getLifeTotal(me) shouldBe 20
        driver.counters(pw, CounterType.LOYALTY) shouldBe 2
    }

    test("chapter II gives your next creature spell this turn an extra +1/+1 counter") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.castSaga(me)
        driver.advanceUntil {
            val saga = findPermanent(me, "Kumano Faces Kakkazan") ?: return@advanceUntil true
            counters(saga, CounterType.LORE) >= 2
        }
        driver.resolveAll()

        val courser = driver.putCardInHand(me, "Centaur Courser")
        driver.giveColorlessMana(me, 2)
        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpell(me, courser)
        driver.resolveAll()

        val perm = driver.findPermanent(me, "Centaur Courser")!!
        driver.counters(perm, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1

        withClue("only the next creature spell") {
            val second = driver.putCardInHand(me, "Centaur Courser")
            driver.giveColorlessMana(me, 2)
            driver.giveMana(me, Color.GREEN, 1)
            driver.castSpell(me, second)
            driver.resolveAll()
            val courses = driver.state.getBattlefield(me).filter { driver.getCardName(it) == "Centaur Courser" }
            courses.sumOf { driver.counters(it, CounterType.PLUS_ONE_PLUS_ONE) } shouldBe 1
        }
    }

    test("chapter III returns it transformed: Etching of Kumano, a 2/2 with haste") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val etching = driver.reachEtching(me)

        driver.findPermanent(me, "Kumano Faces Kakkazan") shouldBe null
        val projected = projector.project(driver.state)
        projected.getPower(etching) shouldBe 2
        projected.getToughness(etching) shouldBe 2
        projected.hasKeyword(etching, Keyword.HASTE) shouldBe true
    }

    test("Etching exiles a creature your burn spell killed") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.reachEtching(me)
        val courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, listOf(courser))
        driver.resolveAll()

        driver.findPermanent(opponent, "Centaur Courser") shouldBe null
        driver.getExileCardNames(opponent) shouldBe listOf("Centaur Courser")
        driver.getGraveyardCardNames(opponent).contains("Centaur Courser") shouldBe false
    }

    test("a creature killed by an opponent's source dies normally under Etching") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.reachEtching(me)
        val courser = driver.putCreatureOnBattlefield(me, "Centaur Courser")

        driver.passPriority(me)
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.castSpell(opponent, bolt, listOf(courser))
        driver.resolveAll()

        driver.findPermanent(me, "Centaur Courser") shouldBe null
        driver.getGraveyardCardNames(me).contains("Centaur Courser") shouldBe true
        driver.getExileCardNames(me).contains("Centaur Courser") shouldBe false
        driver.findPermanent(me, "Etching of Kumano") shouldNotBe null
    }
})
