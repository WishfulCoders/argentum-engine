package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AbilityResolvedEvent
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.FinishResolvingAbilityContinuation
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SagaChapterResolvedEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * CR 608.2n: "As the final part of an ability's resolution, the ability is removed from the stack
 * and ceases to exist." A triggered or activated ability is therefore on the stack for its whole
 * resolution — including while it waits on a decision — and reports that it resolved exactly once,
 * at the end, whether or not the resolution paused.
 */
class AbilityResolutionLifecycleTest : FunSpec({

    // "When you cast this spell, discard a card, then draw a card." The discard asks which card,
    // so the trigger's resolution pauses.
    val looterSpell = card("Lifecycle Cast Looter") {
        manaCost = "{1}"
        typeLine = "Creature — Test"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.self.isCast()
            effect = Effects.Discard(1) then Effects.DrawCards(1)
        }
    }

    // "{0}: Discard a card, then draw a card." — the activated version of the same pause.
    val looterArtifact = card("Lifecycle Activated Looter") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Free
            effect = Effects.Discard(1) then Effects.DrawCards(1)
        }
    }

    // "{0}: You gain 1 life." — resolves without asking anything.
    val lifeArtifact = card("Lifecycle Life Artifact") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Free
            effect = Effects.GainLife(1)
        }
    }

    // A one-chapter Saga whose only (and so final) chapter pauses: "I — Discard a card, then draw a card."
    val pausingSaga = card("Lifecycle Pausing Saga") {
        manaCost = "{1}"
        typeLine = "Enchantment — Saga"
        oracleText = "I — Discard a card, then draw a card."
        sagaChapter(1) {
            effect = Effects.Discard(1) then Effects.DrawCards(1)
        }
    }

    // "Whenever the final chapter ability of a Saga you control resolves, you gain 3 life."
    val finalChapterWatcher = card("Lifecycle Final Chapter Watcher") {
        manaCost = "{1}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.you.sagaChapterResolves(true)
            effect = Effects.GainLife(3)
        }
    }

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(looterSpell, looterArtifact, lifeArtifact, pausingSaga, finalChapterWatcher))
        initMirrorMatch(Deck.of("Forest" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.stackAbility(predicate: (EntityId) -> Boolean): EntityId? =
        state.stack.firstOrNull(predicate)

    fun GameTestDriver.finishFrames() =
        state.continuationStack.filterIsInstance<FinishResolvingAbilityContinuation>()

    /** Cast the looter spell and pass, so its cast trigger begins resolving and asks for a discard. */
    fun GameTestDriver.pauseOnCastTrigger(): Pair<EntityId, EntityId> {
        val player = activePlayer!!
        putCardInHand(player, "Grizzly Bears")
        putCardInHand(player, "Grizzly Bears")
        val spell = putCardInHand(player, "Lifecycle Cast Looter")
        giveColorlessMana(player, 1)
        castSpell(player, spell).error shouldBe null
        val trigger = stackAbility { state.getEntity(it)?.has<TriggeredAbilityOnStackComponent>() == true }!!
        bothPass().outcome.shouldBeInstanceOf<Outcome.Paused>()
        return spell to trigger
    }

    test("a triggered ability stays on the stack while its resolution is paused, and leaves after the answer") {
        val d = setup()
        val player = d.activePlayer!!
        val (spell, trigger) = d.pauseOnCastTrigger()

        d.state.stack shouldBe listOf(spell, trigger)
        d.state.getEntity(trigger)?.has<TriggeredAbilityOnStackComponent>() shouldBe true
        d.finishFrames().single().abilityObject.entityId shouldBe trigger

        val discard = d.getHand(player).first { d.getCardName(it) == "Grizzly Bears" }
        d.submitCardSelection(player, listOf(discard)).outcome shouldBe Outcome.Done

        d.state.stack shouldBe listOf(spell)
        d.state.hasEntity(trigger) shouldBe false
        d.finishFrames().shouldBeEmpty()
        d.state.getZone(player, Zone.GRAVEYARD) shouldContain discard
    }

    test("an activated ability stays on the stack while its resolution is paused, and leaves after the answer") {
        val d = setup()
        val player = d.activePlayer!!
        val artifact = d.putPermanentOnBattlefield(player, "Lifecycle Activated Looter")
        d.putCardInHand(player, "Grizzly Bears")
        d.putCardInHand(player, "Grizzly Bears")

        d.submit(ActivateAbility(player, artifact, looterArtifact.script.activatedAbilities.single().id))
            .outcome shouldBe Outcome.Done
        val ability = d.stackAbility { d.state.getEntity(it)?.has<ActivatedAbilityOnStackComponent>() == true }!!
        d.bothPass().outcome.shouldBeInstanceOf<Outcome.Paused>()

        d.state.stack shouldBe listOf(ability)
        d.finishFrames().single().abilityObject.entityId shouldBe ability

        val discard = d.getHand(player).first { d.getCardName(it) == "Grizzly Bears" }
        d.submitCardSelection(player, listOf(discard)).outcome shouldBe Outcome.Done

        d.state.stack.shouldBeEmpty()
        d.state.hasEntity(ability) shouldBe false
        d.finishFrames().shouldBeEmpty()
    }

    test("a paused resolution reports AbilityResolvedEvent exactly once, after the effect's own events") {
        val d = setup()
        val player = d.activePlayer!!
        val before = d.events.size
        val (spell, _) = d.pauseOnCastTrigger()
        val discard = d.getHand(player).first { d.getCardName(it) == "Grizzly Bears" }
        d.submitCardSelection(player, listOf(discard)).outcome shouldBe Outcome.Done

        val events = d.events.drop(before)
        val resolved = events.withIndex().filter { (_, e) -> e is AbilityResolvedEvent && e.sourceId == spell }
        resolved.size shouldBe 1
        val draw = events.indexOfLast { it is ZoneChangeEvent && it.ownerId == player && it.fromZone == Zone.LIBRARY && it.toZone == Zone.HAND }
        val discarded = events.indexOfLast { it is ZoneChangeEvent && it.entityId == discard && it.toZone == Zone.GRAVEYARD }
        (discarded in 0 until resolved.single().index) shouldBe true
        (draw in 0 until resolved.single().index) shouldBe true
    }

    test("an unpaused resolution reports once, leaves the stack, and leaves no continuation behind") {
        val d = setup()
        val player = d.activePlayer!!
        val artifact = d.putPermanentOnBattlefield(player, "Lifecycle Life Artifact")
        val life = d.getLifeTotal(player)

        d.submit(ActivateAbility(player, artifact, lifeArtifact.script.activatedAbilities.single().id))
            .outcome shouldBe Outcome.Done
        val ability = d.state.stack.single()
        val before = d.events.size
        d.bothPass().outcome shouldBe Outcome.Done

        d.getLifeTotal(player) shouldBe life + 1
        d.state.stack.shouldBeEmpty()
        d.state.hasEntity(ability) shouldBe false
        d.state.continuationStack.shouldBeEmpty()
        d.events.drop(before).count { it is AbilityResolvedEvent && it.sourceId == artifact } shouldBe 1
    }

    test("a final chapter that paused still tells a final-chapter watcher it resolved") {
        val d = setup()
        val player = d.activePlayer!!
        d.putPermanentOnBattlefield(player, "Lifecycle Final Chapter Watcher")
        d.putCardInHand(player, "Grizzly Bears")
        d.putCardInHand(player, "Grizzly Bears")
        val saga = d.putCardInHand(player, "Lifecycle Pausing Saga")
        d.giveColorlessMana(player, 1)
        d.castSpell(player, saga).error shouldBe null
        d.bothPass() // the Saga resolves; its chapter I triggers
        val chapter = d.state.stack.single()
        val life = d.getLifeTotal(player)

        d.bothPass().outcome.shouldBeInstanceOf<Outcome.Paused>()
        d.state.stack shouldContain chapter
        // The chapter ability is still on the stack, so the Saga isn't sacrificed under it.
        d.state.getBattlefield() shouldContain saga

        val before = d.events.size
        val discard = d.getHand(player).first { d.getCardName(it) == "Grizzly Bears" }
        d.submitCardSelection(player, listOf(discard)).outcome shouldBe Outcome.Done
        d.events.drop(before).count { it is SagaChapterResolvedEvent && it.isFinalChapter } shouldBe 1
        d.state.stack shouldNotContain chapter

        var guard = 0
        while (guard++ < 10 && d.state.stack.isNotEmpty()) d.bothPass()
        d.getLifeTotal(player) shouldBe life + 3
    }
})
