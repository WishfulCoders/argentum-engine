package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.EmblemLinkedSourceComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khm.cards.ValkiGodOfLies
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Valki, God of Lies // Tibalt, Cosmic Impostor (KHM #114).
 *
 * Valki: the reveal-and-exile "until Valki leaves the battlefield" (CR 610.3 — the card returns to
 * the hand it came from), the leaves-before-resolution ruling, and the {X} copy of a linked card
 * with mana value X (chosen on resolution; nothing happens on a miss).
 *
 * Tibalt: cast as the modal back face (CR 712.11b) it enters with its own loyalty, the as-enters
 * emblem (no trigger), and the cards its loyalty abilities exile stay playable — spells with mana
 * spent as though any color, lands as land plays — by the emblem's owner after Tibalt is gone.
 */
class ValkiGodOfLiesScenarioTest : FunSpec({
    val xAbility = ValkiGodOfLies.activatedAbilities.single().id

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(ValkiGodOfLies))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.castValki(): EntityId {
        val id = putCardInHand(player1, "Valki, God of Lies")
        giveMana(player1, Color.BLACK, 2)
        castSpell(player1, id).error shouldBe null
        bothPass() // Valki resolves; its enters trigger goes on the stack
        return id
    }

    fun GameTestDriver.exileOf(player: EntityId): List<String?> =
        state.getZone(ZoneKey(player, Zone.EXILE)).map { getCardName(it) }

    test("each opponent reveals; Valki's controller exiles a creature card until Valki leaves") {
        val d = driver()
        val giant = d.putCardInHand(d.player2, "Hill Giant")
        val bears = d.putCardInHand(d.player2, "Grizzly Bears")
        val island = d.putCardInHand(d.player2, "Island")
        val valki = d.castValki()
        d.bothPass() // resolve the trigger

        val decision = d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("Valki's controller chooses, from the opponent's creature cards only") {
            decision.playerId shouldBe d.player1
            decision.options shouldContainExactlyInAnyOrder listOf(giant, bears)
            decision.options shouldNotContain island
        }
        d.submitCardSelection(d.player1, listOf(giant)).error shouldBe null
        d.exileOf(d.player2) shouldBe listOf("Hill Giant")

        d.replaceState(d.zones.moveToZone(d.state, valki, Zone.GRAVEYARD).state)
        withClue("CR 610.3: the card returns to the zone it came from — its owner's hand") {
            d.getHand(d.player2) shouldContain giant
            d.exileOf(d.player2) shouldBe emptyList()
        }
    }

    test("if Valki leaves before its trigger resolves, nothing is exiled") {
        val d = driver()
        d.putCardInHand(d.player2, "Hill Giant")
        val valki = d.castValki()
        d.replaceState(d.zones.moveToZone(d.state, valki, Zone.GRAVEYARD).state)
        d.bothPass()
        if (d.state.pendingDecision is SelectCardsDecision) {
            d.submitCardSelection(d.player1, (d.state.pendingDecision as SelectCardsDecision).options.take(1))
        }
        d.exileOf(d.player2) shouldBe emptyList()
        d.getHand(d.player2).map { d.getCardName(it) } shouldContain "Hill Giant"
    }

    test("{X}: Valki becomes a copy of an exiled creature card with mana value X") {
        val d = driver()
        val giant = d.putCardInHand(d.player2, "Hill Giant")
        val valki = d.castValki()
        d.bothPass()
        // A single creature card is the only choice; the engine may take it without asking.
        if (d.state.pendingDecision is SelectCardsDecision) {
            d.submitCardSelection(d.player1, listOf(giant)).error shouldBe null
        }
        d.exileOf(d.player2) shouldBe listOf("Hill Giant")

        // X = 2 misses the mana-value-4 Hill Giant: nothing happens.
        d.giveColorlessMana(d.player1, 2)
        d.submit(ActivateAbility(d.player1, valki, xAbility, xValue = 2)).error shouldBe null
        d.bothPass()
        d.state.getEntity(valki)!!.get<CardComponent>()!!.name shouldBe "Valki, God of Lies"

        d.giveColorlessMana(d.player1, 4)
        d.submit(ActivateAbility(d.player1, valki, xAbility, xValue = 4)).error shouldBe null
        d.bothPass()
        if (d.state.pendingDecision is SelectCardsDecision) d.submitCardSelection(d.player1, listOf(giant))
        val copied = d.state.getEntity(valki)!!.get<CardComponent>()!!
        copied.name shouldBe "Hill Giant"
        d.state.projectedState.getPower(valki) shouldBe 3
        d.state.projectedState.getToughness(valki) shouldBe 3
        withClue("the copy has no printed Valki ability any more") {
            d.legalActions(d.player1).any { (it.action as? ActivateAbility)?.sourceId == valki } shouldBe false
        }

        // The card is still exiled with this object, so it comes back when the copy leaves.
        d.replaceState(d.zones.moveToZone(d.state, valki, Zone.GRAVEYARD).state)
        d.getHand(d.player2) shouldContain giant
    }

    test("Tibalt: cast as the back face, enters with 5 loyalty and its controller gets the emblem") {
        val d = driver()
        val card = d.putCardInHand(d.player1, "Valki, God of Lies")
        d.giveMana(d.player1, Color.BLACK, 6)
        d.giveMana(d.player1, Color.RED, 1)
        d.submit(
            CastSpell(
                d.player1, card,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.MODAL_BACK_FACE,
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).error shouldBe null
        d.bothPass()

        d.getCardName(card) shouldBe "Tibalt, Cosmic Impostor"
        d.state.getEntity(card)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY) shouldBe 5
        withClue("the emblem is an as-enters replacement: nothing used the stack") { d.state.stack.size shouldBe 0 }
        val emblem = d.state.entities.values.single { it.has<EmblemLinkedSourceComponent>() }
        emblem.get<EmblemLinkedSourceComponent>()!!.sourceId shouldBe card

        // +2: exile the top card of each library; the opponent's creature card is castable with
        // mana of the wrong color, and the land is a land play.
        d.putCardOnTopOfLibrary(d.player2, "Grizzly Bears")
        d.putCardOnTopOfLibrary(d.player1, "Forest")
        val plusTwo = ValkiGodOfLies.backFace!!.activatedAbilities.first().id
        d.submit(ActivateAbility(d.player1, card, plusTwo)).error shouldBe null
        d.bothPass()
        d.state.getEntity(card)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY) shouldBe 7
        val bears = d.state.getZone(ZoneKey(d.player2, Zone.EXILE)).single { d.getCardName(it) == "Grizzly Bears" }
        val forest = d.state.getZone(ZoneKey(d.player1, Zone.EXILE)).single { d.getCardName(it) == "Forest" }

        // Tibalt leaves: the emblem keeps working (ruling).
        d.replaceState(d.zones.moveToZone(d.state, card, Zone.GRAVEYARD).state)
        d.playLand(d.player1, forest).error shouldBe null
        d.giveMana(d.player1, Color.BLACK, 2)
        d.castSpell(d.player1, bears).error shouldBe null
        d.bothPass()
        d.getController(bears) shouldBe d.player1
    }

    test("Tibalt −3 exiles target artifact or creature; −8 exiles all graveyards and adds {R}{R}{R}") {
        val d = driver()
        val card = d.putCardInHand(d.player1, "Valki, God of Lies")
        d.giveMana(d.player1, Color.BLACK, 6)
        d.giveMana(d.player1, Color.RED, 1)
        d.submit(
            CastSpell(
                d.player1, card,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.MODAL_BACK_FACE,
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).error shouldBe null
        d.bothPass()

        val giant = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        val minusThree = ValkiGodOfLies.backFace!!.activatedAbilities[1].id
        d.submit(ActivateAbility(d.player1, card, minusThree, targets = listOf(ChosenTarget.Permanent(giant)))).error shouldBe null
        d.bothPass()
        d.exileOf(d.player2) shouldContain "Hill Giant"

        // Next turn: set loyalty to 8 and use the ultimate.
        d.replaceState(d.state.updateEntity(card) {
            it.with(CountersComponent().withAdded(CounterType.LOYALTY, 8))
        })
        d.putCardInGraveyard(d.player2, "Grizzly Bears")
        d.putCardInGraveyard(d.player1, "Lightning Bolt")
        d.passPriorityUntil(Step.END)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.END)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.state.activePlayerId shouldBe d.player1
        val minusEight = ValkiGodOfLies.backFace!!.activatedAbilities[2].id
        d.submit(ActivateAbility(d.player1, card, minusEight)).error shouldBe null
        d.bothPass()
        d.getGraveyard(d.player1).filter { d.getCardName(it) != "Tibalt, Cosmic Impostor" } shouldBe emptyList()
        d.getGraveyard(d.player2) shouldBe emptyList()
        d.exileOf(d.player2) shouldContain "Grizzly Bears"
        d.exileOf(d.player1) shouldContain "Lightning Bolt"
        val pool = d.state.getEntity(d.player1)!!
            .get<com.wingedsheep.engine.state.components.player.ManaPoolComponent>()!!
        pool.red shouldBe 3
    }
})
