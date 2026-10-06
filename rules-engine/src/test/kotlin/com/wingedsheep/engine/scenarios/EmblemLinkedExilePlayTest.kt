package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.EmblemLinkedSourceComponent
import com.wingedsheep.engine.state.components.identity.EmblemSourceComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantMayCastFromLinkedExile
import com.wingedsheep.sdk.scripting.OnEnterRun
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * An emblem whose own text is a [GrantMayCastFromLinkedExile] — "You may play cards exiled with
 * [the planeswalker that made it]" (Tibalt, Cosmic Impostor).
 *
 * The rules this pins:
 *  - CR 114.4 — an emblem's abilities function from the command zone, for the player who got it.
 *  - The pile is the creating permanent's *object* (CR 400.7): the emblem keeps letting its owner
 *    play the cards after that permanent has left the battlefield (2021-02-05 Tibalt ruling), and a
 *    new visit of the same card is a new object whose exiles the old emblem does not cover.
 *  - "Play" covers land cards (CR 305.1) as well as spells, with normal timing; "spend mana as though
 *    it were mana of any color" relaxes the colored requirement of those casts only.
 *  - Only the emblem's owner gets the permission, and only for cards in that pile.
 */
class EmblemLinkedExilePlayTest : FunSpec({

    val walker = card("Pile Emblem Walker") {
        manaCost = "{2}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 5
        oracleText = "As this planeswalker enters, you get an emblem with \"You may play cards exiled with " +
            "Pile Emblem Walker, and you may spend mana as though it were mana of any color to cast those spells.\"\n" +
            "+1: Exile the top card of each player's library.\n" +
            "−1: Exile target creature."
        replacementEffect(
            OnEnterRun(
                Effects.CreatePermanentEmblem(
                    ownedStaticAbilities = listOf(
                        GrantMayCastFromLinkedExile(filter = GameObjectFilter.Any, withAnyManaType = true)
                    ),
                    emblemDescription = "You may play cards exiled with Pile Emblem Walker, and you may " +
                        "spend mana as though it were mana of any color to cast those spells.",
                )
            )
        )
        loyaltyAbility(1) {
            effect = Effects.Pipeline {
                val top = gather(CardSource.TopOfLibrary(count = 1, player = Player.Each))
                exile(top, linkToSource = true)
            }
            description = "Exile the top card of each player's library."
        }
        loyaltyAbility(-1) {
            val creature = target(TargetFilter.Creature)
            effect = Effects.ExileLinkedToSource(creature)
            description = "Exile target creature."
        }
    }

    // A non-emblem linked exiler: its pile must not be playable through the walker's emblem.
    val otherExiler = card("Other Pile Exiler") {
        manaCost = "{0}"
        typeLine = "Artifact"
        activatedAbility {
            cost = com.wingedsheep.sdk.dsl.Costs.Free
            val creature = target(TargetFilter.Creature)
            effect = Effects.ExileLinkedToSource(creature)
            description = "Exile target creature."
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(walker, otherExiler))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.castWalker(): EntityId {
        val id = putCardInHand(player1, "Pile Emblem Walker")
        giveColorlessMana(player1, 2)
        castSpell(player1, id).error shouldBe null
        bothPass()
        return id
    }

    fun GameTestDriver.activate(source: EntityId, description: String, targets: List<EntityId> = emptyList()) {
        val info = legalActions(player1).first {
            val a = it.action
            a is ActivateAbility && a.sourceId == source && it.description.contains(description)
        }
        val action = info.action as ActivateAbility
        val withTargets = if (targets.isEmpty()) action else action.copy(
            targets = targets.map { com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(it) }
        )
        submit(withTargets).error shouldBe null
        bothPass()
    }

    fun GameTestDriver.exiled(player: EntityId, name: String): EntityId =
        state.getZone(ZoneKey(player, Zone.EXILE)).single { getCardName(it) == name }

    fun GameTestDriver.canCast(player: EntityId, cardId: EntityId): Boolean =
        legalActions(player).any { (it.action as? CastSpell)?.cardId == cardId }

    fun GameTestDriver.canPlayLand(player: EntityId, cardId: EntityId): Boolean =
        legalActions(player).any { (it.action as? PlayLand)?.cardId == cardId }

    test("as it enters its controller gets the emblem, bound to this object, with nothing on the stack") {
        val d = driver()
        val pw = d.castWalker()
        d.state.stack.size shouldBe 0
        val emblem = d.state.entities.values.single { it.has<EmblemSourceComponent>() }
        val link = emblem.get<EmblemLinkedSourceComponent>().shouldNotBeNull()
        link.sourceId shouldBe pw
    }

    test("the owner may cast an opponent's exiled card, spending mana as though it were any color") {
        val d = driver()
        val pw = d.castWalker()
        d.putCardOnTopOfLibrary(d.player2, "Grizzly Bears")
        d.putCardOnTopOfLibrary(d.player1, "Forest")
        d.activate(pw, "Exile the top card")

        val bears = d.exiled(d.player2, "Grizzly Bears")
        d.canCast(d.player1, bears) shouldBe true
        withClue("the permission is the emblem owner's, not the card owner's") {
            d.canCast(d.player2, bears) shouldBe false
        }
        // {1}{G} paid entirely with blue mana.
        d.giveMana(d.player1, Color.BLUE, 2)
        d.castSpell(d.player1, bears).error shouldBe null
        d.bothPass()
        (bears in d.state.getBattlefield()) shouldBe true
        d.getController(bears) shouldBe d.player1
    }

    test("land cards exiled with it can be played, following the land-play rules") {
        val d = driver()
        val pw = d.castWalker()
        d.putCardOnTopOfLibrary(d.player1, "Forest")
        d.activate(pw, "Exile the top card")
        val forest = d.exiled(d.player1, "Forest")
        d.canPlayLand(d.player1, forest) shouldBe true
        d.playLand(d.player1, forest).error shouldBe null
        (forest in d.state.getBattlefield()) shouldBe true
    }

    test("the cards stay playable after the planeswalker has left the battlefield") {
        val d = driver()
        val pw = d.castWalker()
        val giant = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        d.activate(pw, "Exile target creature", listOf(giant))
        d.replaceState(d.zones.moveToZone(d.state, pw, Zone.GRAVEYARD).state)
        (pw in d.state.getBattlefield()) shouldBe false

        val exiledGiant = d.exiled(d.player2, "Hill Giant")
        d.canCast(d.player1, exiledGiant) shouldBe true
    }

    test("cards exiled with a different permanent are not covered") {
        val d = driver()
        d.castWalker()
        val exiler = d.putPermanentOnBattlefield(d.player1, "Other Pile Exiler")
        val giant = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        d.activate(exiler, "Exile target creature", listOf(giant))
        d.canCast(d.player1, d.exiled(d.player2, "Hill Giant")) shouldBe false
    }

    test("an old emblem does not cover what a new visit of the same card exiles") {
        val d = driver()
        val first = d.castWalker()
        // The first visit leaves; the card is cast again as a new object with its own emblem.
        d.replaceState(d.zones.moveToZone(d.state, first, Zone.HAND).state)
        d.giveColorlessMana(d.player1, 2)
        d.castSpell(d.player1, first).error shouldBe null
        d.bothPass()
        val second = first
        (second in d.state.getBattlefield()) shouldBe true
        val emblems = d.state.entities.filterValues { it.has<EmblemLinkedSourceComponent>() }
        emblems.size shouldBe 2
        val stamps = emblems.values.map { it.get<EmblemLinkedSourceComponent>()!!.battlefieldTimestamp }.toSet()
        stamps.size shouldBe 2

        val giant = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        d.activate(second, "Exile target creature", listOf(giant))
        val exiledGiant = d.exiled(d.player2, "Hill Giant")
        // Drop the second visit's emblem: only the first visit's emblem is left, and it must not see
        // the second visit's pile.
        val secondEmblem = emblems.entries.single {
            it.value.get<EmblemLinkedSourceComponent>()!!.battlefieldTimestamp == stamps.max()
        }.key
        d.replaceState(d.state.updateEntity(secondEmblem) { it.without<EmblemLinkedSourceComponent>() })
        d.canCast(d.player1, exiledGiant) shouldBe false
    }
})
