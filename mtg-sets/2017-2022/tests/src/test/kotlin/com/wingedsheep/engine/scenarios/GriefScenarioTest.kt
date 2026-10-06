package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.mtg.sets.definitions.mh2.cards.Grief
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Grief {2}{B}{B} — Menace; enters: target opponent reveals their hand, you choose a nonland card
 * from it, that player discards it. Evoke—Exile a black card from your hand.
 *
 * The turn-one play the card is known for: pitch a black card, take the opponent's best nonland
 * card, and lose Grief to its own evoke sacrifice trigger — all without spending mana.
 */
class GriefScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(Grief))
        initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
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

    test("evoked by pitching a black card, it strips a nonland card and is sacrificed") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val bears = driver.putCardInHand(opponent, "Grizzly Bears")
        val pitched = driver.putCardInHand(you, "Doom Blade")
        val grief = driver.putCardInHand(you, "Grief")

        driver.evoke(you, grief, pitched).error shouldBe null
        driver.getExileCardNames(you) shouldBe listOf("Doom Blade")

        var choseCard = false
        driver.settle { decision ->
            when (decision) {
                is ChooseTargetsDecision ->
                    driver.submitMultiTargetSelection(you, mapOf(0 to listOf(opponent))).error shouldBe null
                is SelectCardsDecision -> {
                    decision.playerId shouldBe you
                    driver.submitCardSelection(you, listOf(bears)).error shouldBe null
                    choseCard = true
                }
                else -> error("unexpected decision: $decision")
            }
        }

        choseCard shouldBe true
        driver.getGraveyardCardNames(opponent) shouldContain "Grizzly Bears"
        driver.getHand(opponent) shouldNotContain bears
        driver.findPermanent(you, "Grief") shouldBe null
        driver.getGraveyardCardNames(you) shouldContain "Grief"
    }
})
