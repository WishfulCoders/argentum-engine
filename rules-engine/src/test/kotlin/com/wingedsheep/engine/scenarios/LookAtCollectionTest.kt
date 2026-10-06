package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.HandLookedAtEvent
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientEvent
import com.wingedsheep.engine.view.ClientEventTransformer
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.LookAtCollectionEffect
import com.wingedsheep.sdk.scripting.effects.LookAudience
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * [LookAtCollectionEffect] — the pipeline's private "look at [those cards]" step (CR 701.20e:
 * looking follows the rules for revealing, except the card is shown only to the specified player).
 *
 * - "Look at a card at random in target player's hand" (Urza's Bauble): exactly one card of that
 *   hand becomes known to the looker, and to no one else; the hand doesn't change.
 * - The event carries only the card looked at and is withheld from every other player — the hand's
 *   owner included (CR 402.3).
 * - An empty collection looks at nothing; looking at your own hand shows you nothing new.
 * - The audience is a parameter: an opponent can be the one who looks.
 */
class LookAtCollectionTest : FunSpec({

    val peek = card("Test Peek At Random") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            val player = target(Targets.Player)
            effect = Effects.Pipeline {
                val hand = gather(CardSource.FromZone(Zone.HAND, player.asPlayer))
                look(chooseRandom(1, from = hand))
            }
        }
    }
    val showTop = card("Test Show Opponent Top") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Pipeline {
                val top = gather(CardSource.TopOfLibrary(1, Player.You), lookAudience = LookAudience.None)
                look(top, audience = LookAudience.Opponent)
            }
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(peek, showTop))
        initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.revealedTo(cardId: EntityId): Set<EntityId> =
        state.getEntity(cardId)?.get<RevealedToComponent>()?.playerIds.orEmpty()

    fun GameTestDriver.peekAt(caster: EntityId, targetPlayer: EntityId) {
        val spell = putCardInHand(caster, "Test Peek At Random")
        castSpellWithTargets(caster, spell, listOf(ChosenTarget.Player(targetPlayer))).error shouldBe null
        bothPass()
    }

    test("a card at random in target player's hand: exactly one card, shown to the looker only") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        val handBefore = d.getHand(opponent)
        handBefore.size shouldBe 7

        d.peekAt(you, opponent)

        withClue("looking moves nothing") { d.getHand(opponent) shouldContainExactly handBefore }
        val known = handBefore.filter { you in d.revealedTo(it) }
        known shouldHaveSize 1
        handBefore.filter { d.revealedTo(it).isNotEmpty() } shouldContainExactly known

        val event = d.events.filterIsInstance<HandLookedAtEvent>().single()
        event.viewingPlayerId shouldBe you
        event.targetPlayerId shouldBe opponent
        event.cardIds shouldContainExactly known

        val forYou = ClientEventTransformer.transform(listOf(event), you, d.state).single()
        forYou.shouldBeInstanceOf<ClientEvent.HandLookedAt>().cardIds shouldContainExactly known
        withClue("CR 402.3 / 701.20e — no one else learns which card was looked at") {
            ClientEventTransformer.transform(listOf(event), opponent, d.state).shouldBeEmpty()
        }
    }

    test("an empty hand looks at nothing") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.replaceState(d.state.copy(zones = d.state.zones + (ZoneKey(opponent, Zone.HAND) to emptyList())))

        d.peekAt(you, opponent)

        d.state.stack.size shouldBe 0
        d.events.filterIsInstance<HandLookedAtEvent>().shouldBeEmpty()
    }

    test("looking at a card in your own hand shows you nothing new") {
        val d = driver()
        val you = d.activePlayer!!
        val ownHand = d.getHand(you)

        d.peekAt(you, you)

        d.events.filterIsInstance<HandLookedAtEvent>().shouldBeEmpty()
        ownHand.filter { d.revealedTo(it).isNotEmpty() }.shouldBeEmpty()
    }

    test("the audience is a parameter: an opponent looks, the controller doesn't") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        val top = d.putCardOnTopOfLibrary(you, "Forest")
        d.castSpell(you, d.putCardInHand(you, "Test Show Opponent Top")).error shouldBe null
        d.bothPass()

        d.revealedTo(top) shouldBe setOf(opponent)
        withClue("a library card is not in a hand, so no hand-look event") {
            d.events.filterIsInstance<HandLookedAtEvent>().shouldBeEmpty()
        }
    }
})
