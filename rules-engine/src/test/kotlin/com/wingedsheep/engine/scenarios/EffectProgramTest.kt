package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CursorStep
import com.wingedsheep.engine.core.EffectProgram
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.ProgramCursor
import com.wingedsheep.engine.core.ProgramOrigin
import com.wingedsheep.engine.core.Suspension
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
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SecretChoice
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The effect interpreter: a resolving effect tree runs as one [EffectProgram], whose cursor names
 * the node in progress. Nested composites are positions in that one program, not frames of their
 * own; a leaf that hasn't moved to the interpreter yet (a gate) starts a program of its own over
 * the subtree it runs, labelled with the step that built it.
 */
class EffectProgramTest : FunSpec({

    fun discardThenDraw(): Effect = CompositeEffect(listOf(Effects.Discard(1), Effects.DrawCards(1)))

    // "When you cast this spell, discard a card, then draw a card."
    val castLooter = card("Program Cast Looter") {
        manaCost = "{1}"
        typeLine = "Creature — Test"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.self.isCast()
            effect = discardThenDraw()
        }
    }

    fun activated(name: String, effect: Effect): CardDefinition = card(name) {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Free
            this.effect = effect
        }
    }

    // Three nested levels with an instruction after each; the discard pauses at the innermost.
    val nestedEffect = CompositeEffect(listOf(
        CompositeEffect(listOf(
            CompositeEffect(listOf(Effects.Discard(1), Effects.GainLife(1))),
            Effects.GainLife(2),
        )),
        Effects.GainLife(3),
    ))
    val nested = activated("Program Nested Artifact", nestedEffect)

    // A gain with no player to gain it fails: the ability has no targets to read.
    val failing = Effects.GainLife(10, EffectTarget.ContextTarget(0))
    val costThenPayoff = activated("Program Cost Artifact", CompositeEffect(listOf(
        CompositeEffect(listOf(failing, Effects.GainLife(1)), stopOnError = true),
        Effects.GainLife(2),
    )))
    val skipFailure = activated("Program Skip Artifact", CompositeEffect(listOf(failing, Effects.GainLife(2))))

    // A number stored before the discard's pause, read after it.
    val storedNumber = activated("Program Stored Number Artifact", CompositeEffect(listOf(
        Effects.StoreNumber("stored", DynamicAmount.Fixed(3)),
        Effects.Discard(1),
        Effects.GainLife(DynamicAmount.VariableReference("stored")),
    )))

    val gatedThen = CompositeEffect(listOf(Effects.Discard(1), Effects.GainLife(1)))
    val gated = activated("Program Gated Artifact", CompositeEffect(listOf(Effects.May(gatedThen), Effects.GainLife(2))))

    val secret = activated("Program Secret Artifact", Effects.SecretChoice(
        SecretChoice.Number(minValue = 0, maxValue = DynamicAmount.Fixed(3)),
        participants = listOf(Player.You),
        then = Effects.Discard(1),
    ))

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(castLooter, nested, costThenPayoff, skipFailure, storedNumber, gated, secret))
        initMirrorMatch(Deck.of("Forest" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.programs() = state.continuationStack.filterIsInstance<EffectProgram>()

    fun GameTestDriver.bears(player: EntityId) = getHand(player).filter { getCardName(it) == "Grizzly Bears" }

    /** Activate [artifact]'s free ability and let it resolve; returns the ability's stack entity. */
    fun GameTestDriver.activateAndResolve(definition: CardDefinition): EntityId {
        val player = activePlayer!!
        val artifact = putPermanentOnBattlefield(player, definition.name)
        submit(ActivateAbility(player, artifact, definition.script.activatedAbilities.single().id)).error shouldBe null
        val ability = state.stack.single { state.getEntity(it)?.has<ActivatedAbilityOnStackComponent>() == true }
        bothPass()
        return ability
    }

    /** The child indices along a cursor (each step also carries its composite's entry context). */
    fun ProgramCursor.indices(): List<Int> = steps.map { (it as CursorStep.Child).index }

    /** Cast the looter and pass, so its cast trigger begins resolving and asks for a discard. */
    fun GameTestDriver.pauseOnCastTrigger(): EntityId {
        val player = activePlayer!!
        putCardInHand(player, "Grizzly Bears")
        putCardInHand(player, "Grizzly Bears")
        val spell = putCardInHand(player, "Program Cast Looter")
        giveColorlessMana(player, 1)
        castSpell(player, spell).error shouldBe null
        val trigger = state.stack.single { state.getEntity(it)?.has<TriggeredAbilityOnStackComponent>() == true }
        bothPass().outcome.shouldBeInstanceOf<Outcome.Paused>()
        return trigger
    }

    test("one program waits beneath the paused discard, its cursor at the Select in progress") {
        val d = setup()
        val player = d.activePlayer!!
        val trigger = d.pauseOnCastTrigger()
        val triggerObject = d.state.objectRef(trigger)!!

        // Composite[Composite[Gather, Select, Move], Draw] — paused on the Select.
        val program = d.programs().single()
        val stack = d.state.continuationStack
        stack.last().shouldBeInstanceOf<Suspension>()
        stack[stack.lastIndex - 1] shouldBe program
        program.origin shouldBe ProgramOrigin.Resolving(triggerObject)
        program.root shouldBe d.state.getEntity(trigger)!!.get<TriggeredAbilityOnStackComponent>()!!.effect
        program.root shouldBe discardThenDraw()
        val discard = (program.root as CompositeEffect).effects[0] as CompositeEffect
        discard.effects[0].shouldBeInstanceOf<GatherCardsEffect>()
        discard.effects[1].shouldBeInstanceOf<SelectFromCollectionEffect>()
        discard.effects[2].shouldBeInstanceOf<MoveCollectionEffect>()
        program.cursor.indices() shouldBe listOf(0, 1)

        val handBefore = d.getHand(player).size
        val chosen = d.bears(player).first()
        val before = d.events.size
        d.submitCardSelection(player, listOf(chosen)).outcome shouldBe Outcome.Done

        d.state.getZone(player, Zone.GRAVEYARD) shouldContain chosen
        d.getHand(player).size shouldBe handBefore
        d.programs().shouldBeEmpty()
        val events = d.events.drop(before)
        val discarded = events.indexOfFirst { it is ZoneChangeEvent && it.entityId == chosen && it.toZone == Zone.GRAVEYARD }
        val drawn = events.indexOfFirst { it is ZoneChangeEvent && it.fromZone == Zone.LIBRARY && it.toZone == Zone.HAND }
        (discarded in 0 until drawn) shouldBe true
    }

    test("a pause three composites deep resumes every level's remaining instructions in order") {
        val d = setup()
        val player = d.activePlayer!!
        d.putCardInHand(player, "Grizzly Bears")
        d.putCardInHand(player, "Grizzly Bears")
        val lifeBefore = d.getLifeTotal(player)
        val ability = d.activateAndResolve(nested)

        val program = d.programs().single()
        program.root shouldBe nestedEffect
        program.origin shouldBe ProgramOrigin.Resolving(d.state.objectRef(ability)!!)
        program.cursor.indices() shouldBe listOf(0, 0, 0, 1)

        val before = d.events.size
        d.submitCardSelection(player, listOf(d.bears(player).first())).outcome shouldBe Outcome.Done
        d.getLifeTotal(player) shouldBe lifeBefore + 6
        d.events.drop(before).filterIsInstance<LifeChangedEvent>()
            .map { it.newLife - it.oldLife } shouldBe listOf(1, 2, 3)
        d.state.continuationStack.shouldBeEmpty()
    }

    test("a failed cost abandons only its stopOnError composite; a plain composite skips a failure") {
        val d = setup()
        val player = d.activePlayer!!
        val lifeBefore = d.getLifeTotal(player)
        d.activateAndResolve(costThenPayoff)
        // The payoff (1) is abandoned with its cost; the instruction after the composite (2) runs.
        d.getLifeTotal(player) shouldBe lifeBefore + 2
        d.state.continuationStack.shouldBeEmpty()

        d.activateAndResolve(skipFailure)
        d.getLifeTotal(player) shouldBe lifeBefore + 4
        d.state.continuationStack.shouldBeEmpty()
    }

    test("a number stored before a pause is read by a later instruction after it") {
        val d = setup()
        val player = d.activePlayer!!
        d.putCardInHand(player, "Grizzly Bears")
        d.putCardInHand(player, "Grizzly Bears")
        val lifeBefore = d.getLifeTotal(player)
        d.activateAndResolve(storedNumber)

        val program = d.programs().single()
        program.cursor.indices() shouldBe listOf(1, 1)
        program.effectContext.pipeline.storedNumbers["stored"] shouldBe 3

        d.submitCardSelection(player, listOf(d.bears(player).first())).outcome shouldBe Outcome.Done
        d.getLifeTotal(player) shouldBe lifeBefore + 3
    }

    test("one paused state resumes independently with different answers") {
        val d = setup()
        val player = d.activePlayer!!
        d.pauseOnCastTrigger()
        val paused = d.state
        val (first, second) = d.bears(player)

        d.submitCardSelection(player, listOf(first)).outcome shouldBe Outcome.Done
        val afterFirst = d.state
        d.replaceState(paused)
        d.submitCardSelection(player, listOf(second)).outcome shouldBe Outcome.Done
        val afterSecond = d.state

        afterFirst.getZone(player, Zone.GRAVEYARD) shouldContain first
        afterFirst.getZone(player, Zone.GRAVEYARD) shouldNotContain second
        afterFirst.getZone(player, Zone.HAND) shouldContain second
        afterSecond.getZone(player, Zone.GRAVEYARD) shouldContain second
        afterSecond.getZone(player, Zone.GRAVEYARD) shouldNotContain first
        afterSecond.getZone(player, Zone.HAND) shouldContain first
        afterFirst.getZone(player, Zone.HAND).size shouldBe afterSecond.getZone(player, Zone.HAND).size
        // The paused state itself is untouched by either resume.
        paused.continuationStack.filterIsInstance<EffectProgram>().single().cursor.indices() shouldBe listOf(0, 1)
    }

    test("a gate's branch runs as a program of its own, labelled with the gate, its cursor local") {
        val d = setup()
        val player = d.activePlayer!!
        d.putCardInHand(player, "Grizzly Bears")
        d.putCardInHand(player, "Grizzly Bears")
        val lifeBefore = d.getLifeTotal(player)
        val ability = d.activateAndResolve(gated)
        val resolving = ProgramOrigin.Resolving(d.state.objectRef(ability)!!)

        // Paused on the "may": the outer program sits at the gate.
        d.programs().single().cursor.indices() shouldBe listOf(0)
        d.submitYesNo(player, true).outcome.shouldBeInstanceOf<Outcome.Paused>()

        val (outer, branch) = d.programs()
        outer.origin shouldBe resolving
        outer.cursor.indices() shouldBe listOf(0)
        branch.origin shouldBe ProgramOrigin.BuiltBy("Gated", resolving)
        branch.root shouldBe gatedThen
        branch.cursor.indices() shouldBe listOf(0, 1)

        d.submitCardSelection(player, listOf(d.bears(player).first())).outcome shouldBe Outcome.Done
        d.getLifeTotal(player) shouldBe lifeBefore + 3
        d.state.continuationStack.shouldBeEmpty()
    }

    test("effects an executor queues itself are labelled with that executor's step") {
        val d = setup()
        val player = d.activePlayer!!
        d.putCardInHand(player, "Grizzly Bears")
        d.putCardInHand(player, "Grizzly Bears")
        val ability = d.activateAndResolve(secret)
        val resolving = ProgramOrigin.Resolving(d.state.objectRef(ability)!!)

        val question = d.state.pendingDecision!!
        d.submitDecision(player, NumberChosenResponse(question.id, 2)).outcome.shouldBeInstanceOf<Outcome.Paused>()

        // The "then" branch was queued by SecretChoice and has paused in its discard.
        val branch = d.programs().single()
        branch.origin shouldBe ProgramOrigin.BuiltBy("SecretChoice", resolving)
        branch.root shouldBe CompositeEffect(listOf(Effects.Discard(1)))
        branch.cursor.indices() shouldBe listOf(0, 1)

        d.submitCardSelection(player, listOf(d.bears(player).first())).outcome shouldBe Outcome.Done
        d.state.getZone(player, Zone.GRAVEYARD).size shouldBe 1
        d.state.continuationStack.shouldBeEmpty()
    }
})
