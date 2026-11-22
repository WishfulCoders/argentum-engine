package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CardRevealedForMiracleEvent
import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.DecisionSubmittedEvent
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.pendingQuestionIsPrivate
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientEvent
import com.wingedsheep.engine.view.ClientEventTransformer
import com.wingedsheep.mtg.sets.definitions.avr.cards.EntreatTheAngels
import com.wingedsheep.mtg.sets.definitions.avr.cards.TemporalMastery
import com.wingedsheep.mtg.sets.definitions.avr.cards.ThunderousWrath
import com.wingedsheep.mtg.sets.definitions.sos.cards.LoreholdTheHistorian
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Miracle (CR 702.94a): "You may reveal this card from your hand as you draw it if it's the first
 * card you've drawn this turn. When you reveal this card this way, you may cast it by paying [cost]
 * rather than its mana cost."
 *
 * The mechanic test: the private reveal question as the card is drawn, the trigger on the stack,
 * the cast while it resolves (no timing restrictions, CR 608.2g), and no miracle cast at any other
 * time. Uses Temporal Mastery (sorcery, miracle {1}{U}), Thunderous Wrath (instant, miracle {R}),
 * and Lorehold, the Historian's granted miracle {2}.
 */
class MiracleTest : FunSpec({

    val drawOne = card("Miracle Draw Test") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Draw a card."
        spell { effect = Effects.DrawCards(1) }
    }
    val drawOneInstant = card("Miracle Draw Instant Test") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Draw a card."
        spell { effect = Effects.DrawCards(1) }
    }
    val drawTwo = card("Miracle Draw Two Test") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Draw two cards."
        spell { effect = Effects.DrawCards(2) }
    }

    fun newGame(land: String): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(TemporalMastery, EntreatTheAngels, ThunderousWrath, LoreholdTheHistorian, drawOne, drawOneInstant, drawTwo)
        )
        driver.initMirrorMatch(deck = Deck.of(land to 40), skipMulligans = true)
        return driver
    }

    fun GameTestDriver.inHand(player: EntityId, card: EntityId) = card in state.getZone(ZoneKey(player, Zone.HAND))

    fun GameTestDriver.isMiracleQuestion() =
        (pendingDecision as? YesNoDecision)?.prompt?.contains("miracle") == true

    /** Pass priority until the miracle reveal question is up, declining any other "you may" on the way. */
    fun GameTestDriver.passUntilMiracleQuestion() {
        repeat(60) {
            if (isMiracleQuestion()) return
            when (val other = pendingDecision) {
                null -> passPriority(state.priorityPlayerId ?: error("No one has priority at ${state.step}"))
                is YesNoDecision -> submitYesNo(other.playerId, false)
                else -> error("Unexpected decision: ${other.prompt}")
            }
        }
        error("The miracle question never came")
    }

    /** What [viewer]'s client is shown for [events]. */
    fun GameTestDriver.clientView(events: List<GameEvent>, viewer: EntityId): List<ClientEvent> =
        ClientEventTransformer.transform(events, viewer, state)

    fun GameTestDriver.miracleCastsOffered(player: EntityId) = legalActions(player).filter {
        (it.action as? CastSpell)?.alternativeCostType == AlternativeCostType.MIRACLE
    }

    fun GameTestDriver.submitMiracleCast(player: EntityId, card: EntityId, targets: List<ChosenTarget> = emptyList()) =
        submit(
            CastSpell(
                player, card, targets,
                useAlternativeCost = true, alternativeCostType = AlternativeCostType.MIRACLE
            )
        )

    fun GameTestDriver.life(player: EntityId) = state.getEntity(player)?.get<LifeTotalComponent>()?.life

    test("drawing a miracle card in the draw step asks its owner privately, and only them") {
        val driver = newGame("Island")
        val a = driver.activePlayer!!
        val b = driver.getOpponent(a)
        val mastery = driver.putCardOnTopOfLibrary(b, "Temporal Mastery")

        driver.passUntilMiracleQuestion()

        driver.state.step shouldBe Step.DRAW
        driver.pendingDecision!!.playerId shouldBe b
        driver.state.pendingQuestionIsPrivate shouldBe true
        driver.inHand(b, mastery) shouldBe true
        // Nothing about the card has reached the table yet.
        driver.events.filterIsInstance<CardsRevealedEvent>().shouldBeEmpty()
    }

    test("declining reveals nothing, logs nothing, and puts nothing on the stack") {
        val driver = newGame("Island")
        val a = driver.activePlayer!!
        val b = driver.getOpponent(a)
        val mastery = driver.putCardOnTopOfLibrary(b, "Temporal Mastery")
        driver.putLandOnBattlefield(b, "Island")
        driver.putLandOnBattlefield(b, "Island")
        driver.passUntilMiracleQuestion()

        val result = driver.submitYesNo(b, false)

        result.outcome shouldBe Outcome.Done
        result.events.filterIsInstance<CardsRevealedEvent>().shouldBeEmpty()
        result.events.filterIsInstance<CardRevealedForMiracleEvent>().shouldBeEmpty()
        result.events.filterIsInstance<DecisionSubmittedEvent>().single().description shouldBe null
        driver.clientView(result.events, a).filterIsInstance<ClientEvent.DecisionMade>().shouldBeEmpty()
        driver.state.stack.shouldBeEmpty()
        driver.state.priorityPlayerId shouldBe b

        // The miracle cost is gone for the rest of the turn, main phase included.
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.miracleCastsOffered(b).shouldBeEmpty()
        driver.submitMiracleCast(b, mastery).error shouldNotBe null
        driver.inHand(b, mastery) shouldBe true
    }

    test("revealing shows the card, triggers, and the sorcery is cast in the draw step as the trigger resolves") {
        val driver = newGame("Island")
        val a = driver.activePlayer!!
        val b = driver.getOpponent(a)
        val mastery = driver.putCardOnTopOfLibrary(b, "Temporal Mastery")
        driver.putLandOnBattlefield(b, "Island")
        driver.putLandOnBattlefield(b, "Island")
        driver.passUntilMiracleQuestion()

        val reveal = driver.submitYesNo(b, true)

        reveal.events.filterIsInstance<CardsRevealedEvent>().single().cardIds shouldBe listOf(mastery)
        driver.clientView(reveal.events, a).filterIsInstance<ClientEvent.CardsRevealed>()
            .single().cardIds shouldBe listOf(mastery)
        // The trigger uses the stack: it is there, and the owner holds priority over it.
        driver.state.step shouldBe Step.DRAW
        driver.state.stack.size shouldBe 1
        driver.state.stack shouldNotContain mastery
        driver.state.priorityPlayerId shouldBe b

        driver.bothPass()
        val castOffer = driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        castOffer.playerId shouldBe b
        driver.submitYesNo(b, true).outcome shouldBe Outcome.Done

        // Cast for {1}{U} off two Islands, at sorcery speed's worst moment: B's draw step.
        driver.state.step shouldBe Step.DRAW
        driver.inHand(b, mastery) shouldBe false
        driver.state.stack shouldContain mastery
        driver.getUntappedLands(b).shouldBeEmpty()
    }

    test("a response to the trigger resolves first; a card no longer in hand isn't offered") {
        val driver = newGame("Mountain")
        val a = driver.activePlayer!!
        val b = driver.getOpponent(a)
        // Lorehold grants miracle {2} to instants and sorceries in B's hand; Shock costs {R}.
        driver.putPermanentOnBattlefield(b, "Lorehold, the Historian")
        val shock = driver.putCardOnTopOfLibrary(b, "Shock")
        driver.putLandOnBattlefield(b, "Mountain")
        driver.passUntilMiracleQuestion()
        driver.submitYesNo(b, true)
        driver.state.stack.size shouldBe 1

        // In response, B casts the revealed Shock for its normal cost.
        driver.castSpellWithTargets(b, shock, listOf(ChosenTarget.Player(a))).outcome shouldBe Outcome.Done
        driver.state.stack.size shouldBe 2
        driver.bothPass() // Shock resolves first
        driver.life(a) shouldBe 18
        driver.bothPass() // the trigger resolves with its card gone: nothing to ask

        driver.pendingDecision shouldBe null
        driver.state.stack.shouldBeEmpty()
    }

    test("an instant with miracle is castable only while the trigger resolves, not later that turn") {
        val driver = newGame("Mountain")
        val a = driver.activePlayer!!
        val b = driver.getOpponent(a)
        val wrath = driver.putCardOnTopOfLibrary(b, "Thunderous Wrath")
        driver.putLandOnBattlefield(b, "Mountain")
        driver.passUntilMiracleQuestion()
        driver.submitYesNo(b, true)
        driver.bothPass()
        driver.submitYesNo(b, false) // decline the cast

        driver.state.stack.shouldBeEmpty()
        driver.inHand(b, wrath) shouldBe true
        // Draw step, then the main phase: no miracle cast is offered, and a submitted one fails.
        driver.miracleCastsOffered(b).shouldBeEmpty()
        driver.submitMiracleCast(b, wrath, listOf(ChosenTarget.Player(a))).error shouldNotBe null
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.miracleCastsOffered(b).shouldBeEmpty()
        driver.submitMiracleCast(b, wrath, listOf(ChosenTarget.Player(a))).error shouldNotBe null
    }

    test("the cast is offered when only the miracle cost is affordable, and asks for targets") {
        val driver = newGame("Mountain")
        val a = driver.activePlayer!!
        val b = driver.getOpponent(a)
        val wrath = driver.putCardOnTopOfLibrary(b, "Thunderous Wrath")
        // One Mountain: {R} miracle, never {4}{R}{R}.
        driver.putLandOnBattlefield(b, "Mountain")
        driver.passUntilMiracleQuestion()
        driver.submitYesNo(b, true)
        driver.bothPass()
        driver.submitYesNo(b, true)
        driver.submitTargetSelection(b, listOf(a)).outcome shouldBe Outcome.Done

        driver.state.stack shouldContain wrath
        driver.getUntappedLands(b).shouldBeEmpty()
        driver.bothPass()
        driver.life(a) shouldBe 15
    }

    test("a first draw on an opponent's turn is asked too, and the sorcery is cast on that turn") {
        val driver = newGame("Island")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val a = driver.activePlayer!!
        val b = driver.getOpponent(a)
        val mastery = driver.putCardOnTopOfLibrary(b, "Temporal Mastery")
        driver.putLandOnBattlefield(b, "Island")
        driver.putLandOnBattlefield(b, "Island")
        val instant = driver.putCardInHand(b, "Miracle Draw Instant Test")

        // A passes with an empty stack; B gets priority in A's main phase and draws.
        driver.passPriority(a)
        driver.castSpell(b, instant).outcome shouldBe Outcome.Done
        driver.passUntilMiracleQuestion()
        driver.pendingDecision!!.playerId shouldBe b
        driver.submitYesNo(b, true)
        driver.bothPass()
        driver.submitYesNo(b, true).outcome shouldBe Outcome.Done

        driver.state.activePlayerId shouldBe a
        driver.state.stack shouldContain mastery
    }

    test("a draw-two pauses after the first card, before the second is drawn") {
        val driver = newGame("Island")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val a = driver.activePlayer!! // skipped the turn-1 draw, so nothing drawn yet this turn
        val second = driver.putCardOnTopOfLibrary(a, "Island")
        val mastery = driver.putCardOnTopOfLibrary(a, "Temporal Mastery")
        val drawSpell = driver.putCardInHand(a, "Miracle Draw Two Test")

        driver.castSpell(a, drawSpell).outcome shouldBe Outcome.Done
        driver.passUntilMiracleQuestion()

        driver.inHand(a, mastery) shouldBe true
        driver.inHand(a, second) shouldBe false
        driver.submitYesNo(a, false)
        driver.inHand(a, second) shouldBe true
    }

    test("only the first card drawn in a turn is asked about") {
        val driver = newGame("Island")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val a = driver.activePlayer!!
        driver.putCardOnTopOfLibrary(a, "Temporal Mastery")
        driver.putCardOnTopOfLibrary(a, "Island") // drawn first
        val drawSpell = driver.putCardInHand(a, "Miracle Draw Two Test")

        driver.castSpell(a, drawSpell).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.pendingDecision shouldBe null
        driver.events.filterIsInstance<CardsRevealedEvent>().shouldBeEmpty()
    }

    test("granted miracle: Lorehold's {2} is cast as the trigger resolves") {
        val driver = newGame("Mountain")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val a = driver.activePlayer!!
        val b = driver.getOpponent(a)
        driver.putPermanentOnBattlefield(a, "Lorehold, the Historian")
        val shock = driver.putCardOnTopOfLibrary(a, "Shock")
        driver.putLandOnBattlefield(a, "Mountain")
        driver.putLandOnBattlefield(a, "Mountain")
        val drawSpell = driver.putCardInHand(a, "Miracle Draw Test")

        driver.castSpell(a, drawSpell).outcome shouldBe Outcome.Done
        driver.passUntilMiracleQuestion()
        driver.submitYesNo(a, true)
        driver.bothPass() // the miracle trigger resolves
        driver.submitYesNo(a, true)
        driver.submitTargetSelection(a, listOf(b)).outcome shouldBe Outcome.Done
        driver.getUntappedLands(a).shouldBeEmpty() // {2}, not Shock's {R}
        driver.bothPass()
        driver.life(b) shouldBe 18
    }

    test("granted miracle doesn't reach a creature card, so drawing one asks nothing") {
        val driver = newGame("Mountain")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val a = driver.activePlayer!!
        driver.putPermanentOnBattlefield(a, "Lorehold, the Historian")
        val bears = driver.putCardOnTopOfLibrary(a, "Grizzly Bears")
        val drawSpell = driver.putCardInHand(a, "Miracle Draw Test")

        driver.castSpell(a, drawSpell).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.inHand(a, bears) shouldBe true
        driver.pendingDecision shouldBe null
    }

    test("an {X} miracle cost lets the caster choose X as the trigger resolves") {
        val driver = newGame("Plains")
        val a = driver.activePlayer!!
        val b = driver.getOpponent(a)
        driver.putCardOnTopOfLibrary(b, "Entreat the Angels")
        repeat(4) { driver.putLandOnBattlefield(b, "Plains") }
        driver.passUntilMiracleQuestion()
        driver.submitYesNo(b, true)
        driver.bothPass()
        driver.submitYesNo(b, true)

        // {X}{W}{W} off four Plains: X is at most 2.
        val chooseX = driver.pendingDecision.shouldBeInstanceOf<com.wingedsheep.engine.core.ChooseNumberDecision>()
        chooseX.maxValue shouldBe 2
        driver.submitDecision(b, com.wingedsheep.engine.core.NumberChosenResponse(chooseX.id, 2))
        driver.bothPass()
        driver.getCreatures(b).size shouldBe 2
    }

    test("Two-Headed Giant: a teammate's miracle question doesn't swallow the active player's draw") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TemporalMastery))
        val players = driver.initMultiplayer(
            List(4) { Deck.of("Island" to 40) },
            format = com.wingedsheep.sdk.core.Format.TwoHeadedGiant(),
            teams = listOf(listOf(0, 1), listOf(2, 3)),
        )
        // Team two's first draw step: each of them draws a Temporal Mastery.
        driver.putCardOnTopOfLibrary(players[2], "Temporal Mastery")
        driver.putCardOnTopOfLibrary(players[3], "Temporal Mastery")

        driver.passUntilMiracleQuestion()
        val active = driver.state.activePlayerId!!
        val first = driver.pendingDecision!!.playerId
        (first in players.drop(2)) shouldBe true
        first shouldNotBe active // the teammate draws first (CR 805.4b)
        driver.submitYesNo(first, false)

        // The active player's draw still happens, and asks in turn.
        driver.isMiracleQuestion() shouldBe true
        driver.pendingDecision!!.playerId shouldBe active
        driver.submitYesNo(active, false)
        driver.state.step shouldBe Step.DRAW
        driver.state.priorityPlayerId shouldBe active
    }
})
