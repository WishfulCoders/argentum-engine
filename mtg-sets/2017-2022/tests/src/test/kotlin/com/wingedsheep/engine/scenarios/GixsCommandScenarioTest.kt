package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bro.cards.GixsCommand
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Gix's Command (BRO #97) — {3}{B}{B} Sorcery. Choose two —
 * • Put two +1/+1 counters on up to one creature. It gains lifelink until end of turn.
 * • Destroy each creature with power 2 or less.
 * • Return up to two creature cards from your graveyard to your hand.
 * • Each opponent sacrifices a creature with the greatest power among creatures they control.
 *
 * No mode targets, so every choice is made on resolution. Modes resolve in printed order: a 2-power
 * creature that gets the counters is a 4/4 by the time the "power 2 or less" sweep looks at it.
 */
class GixsCommandScenarioTest : FunSpec({

    val counters = 0
    val sweep = 1
    val regrowth = 2
    val edict = 3

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GixsCommand))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castCommand(caster: EntityId, modes: List<Int>) {
        giveMana(caster, Color.BLACK, 5)
        val spell = putCardInHand(caster, "Gix's Command")
        submit(CastSpell(playerId = caster, cardId = spell, chosenModes = modes)).error shouldBe null
        bothPass()
    }

    test("counters + sweep: the chosen 2-power creature grows out of the sweep and gains lifelink") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val lions = driver.putCreatureOnBattlefield(opp, "Savannah Lions")
        val courser = driver.putCreatureOnBattlefield(opp, "Centaur Courser")

        driver.castCommand(me, listOf(counters, sweep))

        val pick = driver.pendingDecision
        pick.shouldBeInstanceOf<SelectCardsDecision>()
        pick.playerId shouldBe me
        pick.options.shouldContainExactlyInAnyOrder(bears, lions, courser)
        driver.submitCardSelection(me, listOf(bears)).error shouldBe null
        driver.pendingDecision shouldBe null

        driver.state.getEntity(bears)!!.get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>()!!
            .getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        driver.state.projectedState.getPower(bears) shouldBe 4
        driver.state.projectedState.hasKeyword(bears, Keyword.LIFELINK) shouldBe true

        driver.getCreatures(me) shouldContain bears
        driver.getCreatures(opp) shouldBe listOf(courser)
        driver.getGraveyard(opp) shouldContain lions
    }

    test("counters on nothing is legal; the sweep still resolves") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.putCreatureOnBattlefield(opp, "Centaur Courser")

        driver.castCommand(me, listOf(counters, sweep))
        driver.submitCardSelection(me, emptyList()).error shouldBe null

        driver.getCreatures(me) shouldNotContain bears
        driver.getGraveyard(me) shouldContain bears
        driver.getCreatures(opp).size shouldBe 1
    }

    test("regrowth + edict: up to two creature cards return; opponent loses its greatest-power creature") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val bearsCard = driver.putCardInGraveyard(me, "Grizzly Bears")
        driver.putCardInGraveyard(me, "Savannah Lions")
        val courserCard = driver.putCardInGraveyard(me, "Centaur Courser")
        val swampCard = driver.putCardInGraveyard(me, "Swamp")
        val force = driver.putCreatureOnBattlefield(opp, "Force of Nature")
        val oppCourser = driver.putCreatureOnBattlefield(opp, "Centaur Courser")
        // The caster's own big creature is not affected by "each opponent".
        val myForce = driver.putCreatureOnBattlefield(me, "Force of Nature")

        driver.castCommand(me, listOf(regrowth, edict))

        val pick = driver.pendingDecision
        pick.shouldBeInstanceOf<SelectCardsDecision>()
        pick.playerId shouldBe me
        pick.options.size shouldBe 3
        pick.options shouldNotContain swampCard
        driver.submitCardSelection(me, listOf(bearsCard, courserCard)).error shouldBe null
        driver.pendingDecision shouldBe null

        driver.getHand(me) shouldContain bearsCard
        driver.getHand(me) shouldContain courserCard
        // Left behind: the unchosen creature, the non-creature card, and the resolved Command.
        driver.getGraveyard(me).map { driver.getCardName(it) }.shouldContainExactlyInAnyOrder(
            "Savannah Lions", "Swamp", "Gix's Command"
        )

        driver.getCreatures(opp) shouldBe listOf(oppCourser)
        driver.getGraveyard(opp) shouldContain force
        driver.getCreatures(me) shouldContain myForce
    }

    test("edict with tied greatest power lets the opponent choose among the tied creatures only") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val courserA = driver.putCreatureOnBattlefield(opp, "Centaur Courser")
        val courserB = driver.putCreatureOnBattlefield(opp, "Centaur Courser")
        val bears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")

        driver.castCommand(me, listOf(sweep, edict))

        // The sweep killed the Bears first; the opponent picks which tied Courser to sacrifice.
        val choice = driver.pendingDecision
        choice.shouldBeInstanceOf<SelectCardsDecision>()
        choice.playerId shouldBe opp
        choice.options.shouldContainExactlyInAnyOrder(courserA, courserB)
        driver.submitCardSelection(opp, listOf(courserB)).error shouldBe null

        driver.getCreatures(opp) shouldBe listOf(courserA)
        driver.getGraveyard(opp).shouldContainExactlyInAnyOrder(bears, courserB)
    }
})
