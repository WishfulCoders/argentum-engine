package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bro.cards.PortalToPhyrexia
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Portal to Phyrexia — the enters edict makes each opponent sacrifice three creatures of their
 * choice, and at the beginning of your upkeep a creature card from *any* graveyard comes back under
 * your control as a Phyrexian in addition to its other types.
 */
class PortalToPhyrexiaScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(PortalToPhyrexia)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("enters: each opponent sacrifices three creatures of their choice") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val oppCreatures = listOf("Savannah Lions", "Goblin Guide", "Centaur Courser", "Llanowar Elves")
            .map { driver.putCreatureOnBattlefield(opp, it) }
        val mine = driver.putCreatureOnBattlefield(me, "Savannah Lions")

        val portal = driver.putCardInHand(me, "Portal to Phyrexia")
        driver.giveColorlessMana(me, 9)
        driver.castSpell(me, portal).outcome shouldBe Outcome.Done
        driver.bothPass() // Portal resolves; its enters trigger goes on the stack
        driver.bothPass() // the trigger resolves

        val choice = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        choice.playerId shouldBe opp
        val kept = oppCreatures.last()
        driver.submitCardSelection(opp, oppCreatures.dropLast(1)).outcome shouldBe Outcome.Done

        driver.getCreatures(opp) shouldBe listOf(kept)
        driver.getCreatures(me) shouldBe listOf(mine)
    }

    test("your upkeep: a creature card from an opponent's graveyard returns under your control as a Phyrexian") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.putPermanentOnBattlefield(me, "Portal to Phyrexia")
        val lions = driver.putCardInGraveyard(opp, "Savannah Lions")
        driver.putCardInGraveyard(me, "Goblin Guide")

        // The opponent's upkeep doesn't trigger it; only mine does.
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe opp
        driver.passPriorityUntil(Step.DRAW)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe me

        val targeting = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        targeting.playerId shouldBe me
        driver.submitTargetSelection(me, listOf(lions)).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.findPermanent(me, "Savannah Lions") shouldBe lions
        driver.getController(lions) shouldBe me
        driver.state.projectedState.hasSubtype(lions, "Phyrexian") shouldBe true
        driver.state.projectedState.hasSubtype(lions, "Cat") shouldBe true
    }
})
