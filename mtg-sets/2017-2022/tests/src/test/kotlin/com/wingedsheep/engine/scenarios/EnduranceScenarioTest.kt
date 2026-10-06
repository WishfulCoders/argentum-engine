package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.mtg.sets.definitions.mh2.cards.Endurance
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Endurance {1}{G}{G} — Flash, reach; enters: up to one target player puts all the cards from
 * their graveyard on the bottom of their library in a random order.
 * Evoke—Exile a green card from your hand.
 *
 * Pitching a green card evokes it for no mana; the target's graveyard goes to the bottom of their
 * library (as the bottom three cards, in some order), and Endurance is sacrificed to its evoke
 * trigger — landing in *your* graveyard, which the already-resolved trigger no longer touches.
 */
class EnduranceScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(Endurance))
        initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
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

    test("evoked by pitching a green card, it puts the target player's graveyard on the bottom of their library") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val buried = listOf(
            driver.putCardInGraveyard(opponent, "Grizzly Bears"),
            driver.putCardInGraveyard(opponent, "Lightning Bolt"),
            driver.putCardInGraveyard(opponent, "Counterspell"),
        )
        val libraryBefore = driver.state.getZone(ZoneKey(opponent, Zone.LIBRARY)).size
        val pitched = driver.putCardInHand(you, "Llanowar Elves")
        val endurance = driver.putCardInHand(you, "Endurance")

        driver.evoke(you, endurance, pitched).error shouldBe null
        driver.getExileCardNames(you) shouldBe listOf("Llanowar Elves")

        driver.settle { decision ->
            decision as ChooseTargetsDecision
            driver.submitMultiTargetSelection(you, mapOf(0 to listOf(opponent))).error shouldBe null
        }

        driver.getGraveyard(opponent) shouldBe emptyList()
        val library = driver.state.getZone(ZoneKey(opponent, Zone.LIBRARY))
        library.size shouldBe libraryBefore + 3
        library.takeLast(3).map { driver.getCardName(it) } shouldContainExactlyInAnyOrder
            buried.map { driver.getCardName(it) }
        driver.findPermanent(you, "Endurance") shouldBe null
        driver.getGraveyardCardNames(you) shouldContain "Endurance"
    }
})
