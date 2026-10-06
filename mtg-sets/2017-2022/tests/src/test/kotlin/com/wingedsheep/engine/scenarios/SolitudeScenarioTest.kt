package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh2.cards.Solitude
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Solitude {3}{W}{W} — Flash, lifelink; enters: exile up to one other target creature, its
 * controller gains life equal to its power. Evoke—Exile a white card from your hand.
 *
 * Proves the non-mana evoke cost end to end through a real card: pitching a white card casts it
 * for no mana, the enters trigger resolves (Swords to Plowshares on a body), and the evoke
 * sacrifice trigger puts Solitude in the graveyard. Without another white card in hand there is no
 * evoke cast at all — Solitude is on the stack while its cost is paid, so it can't pitch itself.
 */
class SolitudeScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(Solitude))
        initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
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

    fun GameTestDriver.evokeActions(player: EntityId, cardId: EntityId) = legalActions(player).filter {
        val cast = it.action as? CastSpell
        cast != null && cast.cardId == cardId && cast.alternativeCostType == AlternativeCostType.EVOKE
    }

    test("evoked by pitching a white card, it exiles a creature, its controller gains life, and Solitude is sacrificed") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3
        val lions = driver.putCardInHand(you, "Savannah Lions")
        val solitude = driver.putCardInHand(you, "Solitude")

        driver.evokeActions(you, solitude).size shouldBe 1

        driver.submit(
            CastSpell(
                playerId = you,
                cardId = solitude,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.EVOKE,
                additionalCostPayment = AdditionalCostPayment(exiledCards = listOf(lions))
            )
        ).error shouldBe null
        driver.getExileCardNames(you) shouldBe listOf("Savannah Lions")

        driver.settle { decision ->
            decision as ChooseTargetsDecision
            driver.submitMultiTargetSelection(you, mapOf(0 to listOf(courser))).error shouldBe null
        }

        driver.getExileCardNames(opponent) shouldContain "Centaur Courser"
        driver.getLifeTotal(opponent) shouldBe 23
        driver.findPermanent(you, "Solitude") shouldBe null
        driver.getGraveyardCardNames(you) shouldContain "Solitude"
    }

    test("with no other white card in hand, Solitude can't be evoked") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.putCardInHand(you, "Grizzly Bears")
        val solitude = driver.putCardInHand(you, "Solitude")

        driver.evokeActions(you, solitude) shouldBe emptyList()
        driver.submit(
            CastSpell(
                playerId = you,
                cardId = solitude,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.EVOKE,
                additionalCostPayment = AdditionalCostPayment(exiledCards = listOf(solitude))
            )
        ).error shouldNotBe null
    }
})
