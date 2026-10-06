package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LastKnownCopiableComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Two pieces of vocabulary, pinned through the Fractured Identity shape — "Exile target nonland
 * permanent. Each player other than its controller creates a token that's a copy of it.":
 *
 *  - [Player.EachOtherThan] — every player except the one a single-player reference names, in APNAP
 *    order (CR 101.4); with [Player.ControllerOf] that reference reads the exiled permanent's
 *    last-known controller (CR 608.2h).
 *  - A token copy of a **permanent target that has left the battlefield** copies it as it last
 *    existed there (CR 608.2h, CR 707.2): what it was copying, its transformed face. A card targeted
 *    in its current zone is copied as it is there (CR 400.7), and the last-known values do not
 *    outlive the card's next zone change.
 */
class TokenCopyOfDepartedPermanentTest : FunSpec({

    val fracture = card("Test Fracture") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            val permanent = target(TargetFilter.NonlandPermanent)
            effect = Effects.Exile(permanent) then
                Effects.ForEachPlayer(
                    Player.EachOtherThan(Player.ControllerOf("target")),
                    Effects.CreateTokenCopyOfTarget(permanent),
                )
        }
    }

    // A 1/1 Clone, so a token copy of the *card* survives to be inspected.
    val shifter = card("Test Shifter") {
        manaCost = "{0}"
        typeLine = "Creature — Shapeshifter"
        power = 1
        toughness = 1
        replacementEffect(EntersAsCopy(optional = true))
    }

    // "Create a token that's a copy of target creature card in a graveyard."
    val graveCopy = card("Test Grave Copy") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            val creature = target(TargetFilter.CreatureInGraveyard)
            effect = Effects.CreateTokenCopyOfTarget(creature)
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(fracture, shifter, graveCopy))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.tokensOf(player: EntityId): List<EntityId> =
        getPermanents(player).filter { state.getEntity(it)?.has<TokenComponent>() == true }

    fun GameTestDriver.fracture(target: EntityId) {
        val spell = putCardInHand(player1, "Test Fracture")
        castSpellWithTargets(player1, spell, listOf(ChosenTarget.Permanent(target))).error shouldBe null
        bothPass()
    }

    fun GameTestDriver.shifterCopying(owner: EntityId, copied: EntityId): EntityId {
        val id = putCardInHand(owner, "Test Shifter")
        if (owner != player1) {
            // Cast on the opponent's behalf: hand them priority with an empty stack.
            passPriority(player1).error shouldBe null
        }
        submit(CastSpell(owner, id, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        bothPass()
        state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        submitCardSelection(owner, listOf(copied)).error shouldBe null
        return id
    }

    test("exiling an opponent's permanent: every player other than its controller — you — gets a copy") {
        val d = driver()
        val giant = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        d.fracture(giant)
        d.state.getZone(com.wingedsheep.engine.state.ZoneKey(d.player2, Zone.EXILE)).contains(giant) shouldBe true
        d.tokensOf(d.player1).map { d.getCardName(it) } shouldBe listOf("Hill Giant")
        d.tokensOf(d.player2) shouldBe emptyList()
    }

    test("exiling your own permanent: the opponent gets the copy and you do not") {
        val d = driver()
        val giant = d.putCreatureOnBattlefield(d.player1, "Hill Giant")
        d.fracture(giant)
        d.tokensOf(d.player1) shouldBe emptyList()
        val tokens = d.tokensOf(d.player2)
        tokens.map { d.getCardName(it) } shouldBe listOf("Hill Giant")
        d.getController(tokens.single()) shouldBe d.player2
    }

    test("a permanent that was copying something is copied as what it copied (last-known values)") {
        val d = driver()
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val shifted = d.shifterCopying(d.player1, bears)
        d.state.getEntity(shifted)!!.get<CardComponent>()!!.name shouldBe "Grizzly Bears"

        d.fracture(shifted)
        withClue("the card in exile is its printed self again (CR 400.7)") {
            d.state.getEntity(shifted)!!.get<CardComponent>()!!.name shouldBe "Test Shifter"
        }
        withClue("but the token copies the permanent as it last existed: a Grizzly Bears") {
            d.tokensOf(d.player2).map { d.getCardName(it) } shouldBe listOf("Grizzly Bears")
        }
    }

    test("a transformed double-faced permanent is copied with its back face up") {
        val d = driver()
        val dfc = d.putCreatureOnBattlefield(d.player2, "Test DFC Front")
        val transform = d.putCardInHand(d.player1, "Transform Target Creature")
        d.giveMana(d.player1, Color.BLUE, 2)
        d.castSpellWithTargets(d.player1, transform, listOf(ChosenTarget.Permanent(dfc))).error shouldBe null
        d.bothPass()
        d.getCardName(dfc) shouldBe "Test DFC Back"

        d.fracture(dfc)
        d.getCardName(dfc) shouldBe "Test DFC Front"
        val token = d.tokensOf(d.player1).single()
        d.getCardName(token) shouldBe "Test DFC Back"
        d.state.projectedState.getPower(token) shouldBe 4
    }

    test("a card targeted in the graveyard is copied as the card it is there, not its last permanent") {
        val d = driver()
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val shifted = d.shifterCopying(d.player1, bears)
        d.replaceState(d.zones.moveToZone(d.state, shifted, Zone.GRAVEYARD).state)
        withClue("the departure left last-known copiable values behind") {
            (d.state.getEntity(shifted)!!.get<LastKnownCopiableComponent>() != null) shouldBe true
        }
        val spell = d.putCardInHand(d.player1, "Test Grave Copy")
        d.castSpellWithTargets(
            d.player1, spell, listOf(ChosenTarget.Card(shifted, d.player1, Zone.GRAVEYARD))
        ).error shouldBe null
        d.bothPass()
        if (d.state.pendingDecision is SelectCardsDecision) d.submitCardSelection(d.player1, emptyList())
        d.tokensOf(d.player1).map { d.getCardName(it) } shouldBe listOf("Test Shifter")
    }

    test("the last-known copiable values do not survive the card's next zone change") {
        val d = driver()
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val shifted = d.shifterCopying(d.player1, bears)
        d.replaceState(d.zones.moveToZone(d.state, shifted, Zone.GRAVEYARD).state)
        d.replaceState(d.zones.moveToZone(d.state, shifted, Zone.HAND).state)
        (d.state.getEntity(shifted)!!.get<LastKnownCopiableComponent>() == null) shouldBe true
    }

    test("a plain card's departure leaves nothing behind") {
        val d = driver()
        val giant = d.putCreatureOnBattlefield(d.player1, "Hill Giant")
        d.replaceState(d.zones.moveToZone(d.state, giant, Zone.GRAVEYARD).state)
        (d.state.getEntity(giant)!!.get<LastKnownCopiableComponent>() == null) shouldBe true
    }
})
