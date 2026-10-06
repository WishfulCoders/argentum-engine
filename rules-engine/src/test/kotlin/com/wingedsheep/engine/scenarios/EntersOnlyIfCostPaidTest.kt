package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EntersOnlyIfCostPaid
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * [EntersOnlyIfCostPaid] — "If this would enter, you may [cost] instead. If you do, put it onto the
 * battlefield. If you don't, put it into its owner's graveyard." (Mox Diamond).
 *
 * - CR 614.1a / 614.12: a self-replacement on the permanent's own entry, from any entry path —
 *   a resolving spell, an effect moving one card (MoveToZone), an effect moving a collection
 *   (MoveCollection).
 * - CR 614.12a: the payment is chosen and made before it enters.
 * - CR 614.6: an unpaid entry never happens — the card goes to its owner's graveyard and no
 *   zone change to the battlefield is ever emitted (2008-05-01 Mox Diamond ruling).
 * - CR 118.3: a cost the player can't pay takes the "if you don't" branch without a prompt.
 * - The discard is a real discard (a [CardsDiscardedEvent]), restricted to land cards.
 */
class EntersOnlyIfCostPaidTest : FunSpec({

    val mox = card("Test Entry Mox") {
        manaCost = "{0}"
        typeLine = "Artifact"
        replacementEffect(EntersOnlyIfCostPaid(Costs.pay.Discard(GameObjectFilter.Land)))
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddManaOfChoice()
            manaAbility = true
        }
    }
    val show = card("Test Show Artifacts") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Pipeline {
                val artifacts = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Artifact))
                move(artifacts, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
            }
        }
    }
    val recall = card("Test Artifact Recall") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            val artifact = target(TargetFilter.ArtifactInYourGraveyard)
            effect = Effects.PutOntoBattlefieldFromGraveyard(artifact)
        }
    }

    val watcher = card("Test Artifact Watcher") {
        manaCost = "{0}"
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Artifact).enters()
            effect = Effects.GainLife(1)
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(mox, show, recall, watcher))
        initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.enteredBattlefield(id: EntityId): Boolean =
        events.any { it is ZoneChangeEvent && it.entityId == id && it.toZone == Zone.BATTLEFIELD }

    fun GameTestDriver.castAndResolve(player: EntityId, cardId: EntityId) {
        castSpell(player, cardId).error shouldBe null
        bothPass()
    }

    test("cast: discarding a land card puts it onto the battlefield, and the discard is a real discard") {
        val d = driver()
        val you = d.activePlayer!!
        val land = d.putCardInHand(you, "Forest")
        val moxCard = d.putCardInHand(you, "Test Entry Mox")
        d.castAndResolve(you, moxCard)

        val ask = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("asked before it enters (CR 614.12a)") { d.enteredBattlefield(moxCard) shouldBe false }
        d.submitCardSelection(you, listOf(land)).error shouldBe null

        d.findPermanent(you, "Test Entry Mox") shouldBe moxCard
        d.getGraveyard(you) shouldContain land
        d.events.filterIsInstance<CardsDiscardedEvent>().flatMap { it.cardIds } shouldContain land
        ask.playerId shouldBe you
        d.state.stack.size shouldBe 0
    }

    test("cast: only land cards may be discarded") {
        val d = driver()
        val you = d.activePlayer!!
        val forest = d.putCardInHand(you, "Forest")
        val island = d.putCardInHand(you, "Island")
        d.putCardInHand(you, "Lightning Bolt")
        val moxCard = d.putCardInHand(you, "Test Entry Mox")
        d.castAndResolve(you, moxCard)

        val ask = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        ask.options shouldContainExactlyInAnyOrder listOf(forest, island)
    }

    test("cast: declining puts it into its owner's graveyard — it never enters") {
        val d = driver()
        val you = d.activePlayer!!
        val land = d.putCardInHand(you, "Forest")
        val moxCard = d.putCardInHand(you, "Test Entry Mox")
        d.castAndResolve(you, moxCard)
        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()

        d.submitCardSelection(you, emptyList()).error shouldBe null

        d.findPermanent(you, "Test Entry Mox").shouldBeNull()
        d.getGraveyard(you) shouldContain moxCard
        d.getHand(you) shouldContain land
        withClue("CR 614.6 — the replaced entry never happens: no zone change to the battlefield") {
            d.enteredBattlefield(moxCard) shouldBe false
        }
        d.state.stack.size shouldBe 0
        d.pendingDecision.shouldBeNull()
    }

    test("cast: with no land card in hand the cost is unpayable (CR 118.3) — graveyard, no prompt") {
        val d = driver()
        val you = d.activePlayer!!
        d.putCardInHand(you, "Lightning Bolt")
        val moxCard = d.putCardInHand(you, "Test Entry Mox")
        d.castAndResolve(you, moxCard)

        d.pendingDecision.shouldBeNull()
        d.getGraveyard(you) shouldContain moxCard
        d.enteredBattlefield(moxCard) shouldBe false
    }

    test("effect moving a collection (put artifacts from hand onto the battlefield): pay or graveyard") {
        val d = driver()
        val you = d.activePlayer!!
        val land = d.putCardInHand(you, "Forest")
        val moxCard = d.putCardInHand(you, "Test Entry Mox")
        d.castAndResolve(you, d.putCardInHand(you, "Test Show Artifacts"))

        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(you, listOf(land)).error shouldBe null
        d.findPermanent(you, "Test Entry Mox") shouldBe moxCard
        d.getGraveyard(you) shouldContain land

        val d2 = driver()
        val p2 = d2.activePlayer!!
        val land2 = d2.putCardInHand(p2, "Forest")
        val mox2 = d2.putCardInHand(p2, "Test Entry Mox")
        d2.castAndResolve(p2, d2.putCardInHand(p2, "Test Show Artifacts"))
        d2.submitCardSelection(p2, emptyList()).error shouldBe null
        d2.findPermanent(p2, "Test Entry Mox").shouldBeNull()
        d2.getGraveyard(p2) shouldContain mox2
        d2.getHand(p2) shouldContain land2
        d2.enteredBattlefield(mox2) shouldBe false
    }

    test("effect moving one card (return target artifact card from your graveyard): pay or stay put") {
        val d = driver()
        val you = d.activePlayer!!
        val land = d.putCardInHand(you, "Forest")
        val moxCard = d.putCardInGraveyard(you, "Test Entry Mox")
        val recallCard = d.putCardInHand(you, "Test Artifact Recall")
        d.castSpellWithTargets(you, recallCard, listOf(ChosenTarget.Card(moxCard, you, Zone.GRAVEYARD))).error shouldBe null
        d.bothPass()

        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(you, emptyList()).error shouldBe null
        withClue("unpaid: put into its owner's graveyard — where it already is") {
            d.findPermanent(you, "Test Entry Mox").shouldBeNull()
            d.getGraveyard(you) shouldContain moxCard
            d.enteredBattlefield(moxCard) shouldBe false
            d.getHand(you) shouldContain land
        }

        val d2 = driver()
        val p2 = d2.activePlayer!!
        val land2 = d2.putCardInHand(p2, "Forest")
        val mox2 = d2.putCardInGraveyard(p2, "Test Entry Mox")
        val recall2 = d2.putCardInHand(p2, "Test Artifact Recall")
        d2.castSpellWithTargets(p2, recall2, listOf(ChosenTarget.Card(mox2, p2, Zone.GRAVEYARD))).error shouldBe null
        d2.bothPass()
        d2.submitCardSelection(p2, listOf(land2)).error shouldBe null
        d2.findPermanent(p2, "Test Entry Mox") shouldBe mox2
        d2.getGraveyard(p2) shouldContain land2
        d2.getGraveyard(p2) shouldNotContain mox2
    }

    test("a paid entry is a normal entry: the permanent can be tapped for mana") {
        val d = driver()
        val you = d.activePlayer!!
        val land = d.putCardInHand(you, "Forest")
        val moxCard = d.putCardInHand(you, "Test Entry Mox")
        d.castAndResolve(you, moxCard)
        d.submitCardSelection(you, listOf(land)).error shouldBe null

        d.findPermanent(you, "Test Entry Mox").shouldNotBeNull()
        d.services.manaSolver.getAvailableManaCount(d.state, you) shouldBe 1
    }

    test("enter triggers see a paid entry but never an unpaid one (2008-05-01 ruling)") {
        val d = driver()
        val you = d.activePlayer!!
        d.putPermanentOnBattlefield(you, "Test Artifact Watcher")
        val land = d.putCardInHand(you, "Forest")
        val declined = d.putCardInHand(you, "Test Entry Mox")
        d.castAndResolve(you, declined)
        d.submitCardSelection(you, emptyList()).error shouldBe null
        withClue("no entry, so \"whenever an artifact enters\" never triggers") {
            d.state.stack.size shouldBe 0
            d.getLifeTotal(you) shouldBe 20
        }

        val paid = d.putCardInHand(you, "Test Entry Mox")
        d.castAndResolve(you, paid)
        d.submitCardSelection(you, listOf(land)).error shouldBe null
        d.findPermanent(you, "Test Entry Mox") shouldBe paid
        d.state.stack.size shouldBe 1
        d.bothPass()
        d.getLifeTotal(you) shouldBe 21
    }
})
