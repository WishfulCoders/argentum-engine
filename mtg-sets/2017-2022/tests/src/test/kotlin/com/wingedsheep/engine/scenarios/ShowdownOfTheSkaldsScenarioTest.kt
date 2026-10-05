package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khm.cards.ShowdownOfTheSkalds
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Showdown of the Skalds (KHM #229).
 *
 *   I — Exile the top four cards of your library. Until the end of your next turn, you may play
 *       those cards.
 *   II, III — Whenever you cast a spell this turn, put a +1/+1 counter on target creature you control.
 */
class ShowdownOfTheSkaldsScenarioTest : FunSpec({

    val bear = card("Test Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    val shock = card("Test Shock") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell { effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent)) }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ShowdownOfTheSkalds, bear, shock))
        return driver
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard < 50) {
            if (state.pendingDecision != null) autoResolveDecision() else bothPass()
            guard++
        }
    }

    /** Precombat main of the starting player's [nth] turn (turn `2n - 1` in a duel). */
    fun GameTestDriver.advanceToMain(nth: Int) {
        val targetTurn = nth * 2 - 1
        var guard = 0
        while (!(state.turnNumber == targetTurn && state.step == Step.PRECOMBAT_MAIN) && guard < 500) {
            if (state.gameOver) throw AssertionError("Game ended while advancing to turn $targetTurn")
            when {
                state.pendingDecision != null -> autoResolveDecision()
                state.priorityPlayerId != null -> {
                    autoSubmitCombatDeclarationIfNeeded()
                    passPriority(state.priorityPlayerId!!)
                }
            }
            guard++
        }
    }

    fun GameTestDriver.castSaga(controller: EntityId) {
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        giveMana(controller, Color.RED, 1)
        giveMana(controller, Color.WHITE, 1)
        giveColorlessMana(controller, 2)
        val saga = putCardInHand(controller, "Showdown of the Skalds")
        castSpell(controller, saga)
        resolveStack() // saga enters (lore 1 → chapter I) and chapter I resolves
    }

    fun GameTestDriver.castShockTargeting(controller: EntityId, creature: EntityId) {
        giveMana(controller, Color.RED, 1)
        val card = putCardInHand(controller, "Test Shock")
        submit(CastSpell(playerId = controller, cardId = card))
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard < 50) {
            val decision = state.pendingDecision
            when {
                decision is ChooseTargetsDecision -> submitTargetSelection(controller, listOf(creature))
                decision != null -> autoResolveDecision()
                else -> bothPass()
            }
            guard++
        }
    }

    fun GameTestDriver.plusCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("chapter I exiles four cards playable through the end of your next turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val controller = driver.activePlayer!!
        val libraryBefore = driver.state.getLibrary(controller).size

        driver.castSaga(controller)

        driver.state.getLibrary(controller).size shouldBe libraryBefore - 4
        val exiled = driver.state.getExile(controller)
        exiled.size shouldBe 4
        fun permitted() = driver.state.mayPlayPermissions.any { p -> exiled.all { it in p.cardIds } }
        permitted() shouldBe true

        driver.advanceToMain(2) // your next turn: still playable
        driver.resolveStack()
        permitted() shouldBe true

        driver.advanceToMain(3) // the turn after: permission gone, cards stay exiled
        driver.resolveStack()
        permitted() shouldBe false
        driver.state.getExile(controller).containsAll(exiled) shouldBe true
    }

    test("chapter II puts a +1/+1 counter on a target creature you control for each spell you cast that turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val controller = driver.activePlayer!!

        driver.castSaga(controller)
        val bearId = driver.putCreatureOnBattlefield(controller, "Test Bear")

        // A spell cast on the chapter I turn does nothing: no delayed trigger yet.
        driver.castShockTargeting(controller, bearId)
        driver.plusCounters(bearId) shouldBe 0

        driver.advanceToMain(2) // chapter II
        driver.resolveStack()
        driver.state.delayedTriggers.size shouldBe 1

        driver.castShockTargeting(controller, bearId)
        driver.castShockTargeting(controller, bearId)
        driver.plusCounters(bearId) shouldBe 2

        // Chapter II's trigger ends with the turn; chapter III installs a fresh one.
        driver.advanceToMain(3)
        driver.resolveStack()
        driver.state.delayedTriggers.size shouldBe 1
        driver.castShockTargeting(controller, bearId)
        driver.plusCounters(bearId) shouldBe 3
    }
})
