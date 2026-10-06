package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.mtg.sets.definitions.mh2.cards.Subtlety
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Subtlety {2}{U}{U} — Flash, flying; enters: choose up to one target creature spell or
 * planeswalker spell, its owner puts it on the top or bottom of their library.
 * Evoke—Exile a blue card from your hand.
 *
 * The flash evoke in response: the opponent casts a creature, you pitch a blue card to flash
 * Subtlety in for no mana, and the creature spell goes to the top of its owner's library (the
 * owner's choice) instead of resolving. Subtlety is sacrificed to its evoke trigger.
 */
class SubtletyScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(Subtlety))
        // Player 2 starts, so "you" act in response on the opponent's turn.
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20, startingPlayer = 1)
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

    test("flashed in by pitching a blue card, it puts the opponent's creature spell on top of their library") {
        val driver = createDriver()
        val opponent = driver.activePlayer!!
        val you = driver.getOpponent(opponent)

        val courser = driver.putCardInHand(opponent, "Centaur Courser")
        driver.giveMana(opponent, Color.GREEN, 3)
        driver.castSpell(opponent, courser).error shouldBe null
        driver.passPriority(opponent)

        val pitched = driver.putCardInHand(you, "Counterspell")
        val subtlety = driver.putCardInHand(you, "Subtlety")
        driver.evoke(you, subtlety, pitched).error shouldBe null
        driver.getExileCardNames(you) shouldBe listOf("Counterspell")

        var ownerChose = false
        driver.settle { decision ->
            when (decision) {
                is ChooseTargetsDecision ->
                    driver.submitMultiTargetSelection(you, mapOf(0 to listOf(courser))).error shouldBe null
                is ChooseOptionDecision -> {
                    decision.playerId shouldBe opponent
                    driver.submitDecision(opponent, OptionChosenResponse(decision.id, 0)).error shouldBe null // top
                    ownerChose = true
                }
                else -> error("unexpected decision: $decision")
            }
        }

        ownerChose shouldBe true
        driver.findPermanent(opponent, "Centaur Courser") shouldBe null
        driver.getCardName(driver.state.getZone(ZoneKey(opponent, Zone.LIBRARY)).first()) shouldBe "Centaur Courser"
        driver.findPermanent(you, "Subtlety") shouldBe null
        driver.getGraveyardCardNames(you) shouldContain "Subtlety"
    }
})
