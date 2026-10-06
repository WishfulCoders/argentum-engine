package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.mtg.sets.definitions.mh2.cards.Fury
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Fury {3}{R}{R} — Double strike; enters: 4 damage divided as you choose among any number of
 * target creatures and/or planeswalkers. Evoke—Exile a red card from your hand.
 *
 * Pitch a red card, split the 4 damage 2/2 across two bears as the trigger goes on the stack, and
 * lose Fury to its evoke sacrifice trigger — no mana spent.
 */
class FuryScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(Fury))
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Pass priority until the stack is empty, ordering simultaneous triggers as presented. */
    fun GameTestDriver.settle(answer: (PendingDecision) -> Unit) {
        repeat(40) {
            val decision = pendingDecision
            when {
                decision is OrderObjectsDecision ->
                    submitDecision(decision.playerId, OrderedResponse(decision.id, decision.objects)).error shouldBe null
                decision != null -> answer(decision)
                stackSize > 0 -> bothPass()
                else -> return
            }
        }
        error("the game did not settle")
    }

    fun GameTestDriver.evoke(player: EntityId, cardId: EntityId, pitched: EntityId) = submit(
        CastSpell(
            playerId = player,
            cardId = cardId,
            useAlternativeCost = true,
            alternativeCostType = AlternativeCostType.EVOKE,
            additionalCostPayment = AdditionalCostPayment(exiledCards = listOf(pitched))
        )
    )

    test("evoked by pitching a red card, it divides 4 damage between two creatures and is sacrificed") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val bearA = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val bearB = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val pitched = driver.putCardInHand(you, "Lightning Bolt")
        val fury = driver.putCardInHand(you, "Fury")

        driver.evoke(you, fury, pitched).error shouldBe null
        driver.getExileCardNames(you) shouldBe listOf("Lightning Bolt")

        var divided = false
        driver.settle { decision ->
            when (decision) {
                is ChooseTargetsDecision ->
                    driver.submitMultiTargetSelection(you, mapOf(0 to listOf(bearA, bearB))).error shouldBe null
                is DistributeDecision -> {
                    decision.totalAmount shouldBe 4
                    driver.submitDecision(you, DistributionResponse(decision.id, mapOf(bearA to 2, bearB to 2)))
                        .error shouldBe null
                    divided = true
                }
                else -> error("unexpected decision: $decision")
            }
        }

        divided shouldBe true
        driver.getCreatures(opponent).none { it == bearA || it == bearB } shouldBe true
        driver.getGraveyardCardNames(opponent).count { it == "Grizzly Bears" } shouldBe 2
        driver.findPermanent(you, "Fury") shouldBe null
        driver.getGraveyardCardNames(you) shouldContain "Fury"
    }
})
