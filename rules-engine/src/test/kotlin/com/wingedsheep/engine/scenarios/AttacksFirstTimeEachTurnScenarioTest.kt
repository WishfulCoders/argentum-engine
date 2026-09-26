package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.combat.PlayerAttackersThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `AttackPredicate.FirstTimeEachTurn` — "Whenever this creature attacks for the first time each
 * turn, …" (engine gap #136a, the trigger half of Fear of Missing Out).
 *
 * The flag gates a SELF attack trigger on the per-turn attacker set: it fires the first time the
 * source is declared as an attacker in a turn, does NOT fire if it attacks again later that same
 * turn (an extra combat phase — the *other* half of Fear of Missing Out), and fires again on a
 * later turn because the window resets each turn. The "first time" fact is captured on the
 * `AttackersDeclaredEvent` at declaration, so the second-combat case can't be papered over by
 * post-declaration state.
 */
class AttacksFirstTimeEachTurnScenarioTest : FunSpec({

    // Proof creature: a 2/2 that draws a card the first time it attacks each turn.
    val firstStriker = CardDefinition.creature(
        name = "First-Strike Scout",
        manaCost = ManaCost.parse("{1}{R}"),
        subtypes = setOf(Subtype("Scout")),
        power = 2,
        toughness = 2,
        oracleText = "Whenever this creature attacks for the first time each turn, draw a card.",
        script = CardScript.creature(
            TriggeredAbility.create(
                id = AbilityId("AttacksFirstTimeEachTurnScenarioTest_1"),
                trigger = EventPattern.AttackEvent(
                    requires = setOf(AttackPredicate.FirstTimeEachTurn)
                ),
                binding = TriggerBinding.SELF,
                effect = DrawCardsEffect(1)
            )
        )
    )

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(
            TestCards.all +
                com.wingedsheep.mtg.sets.tokens.PredefinedTokens.allTokens +
                listOf(firstStriker)
        )
        return d
    }

    test("fires on the first attack each turn, and again the next turn") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val scout = d.putCreatureOnBattlefield(active, "First-Strike Scout")
        d.removeSummoningSickness(scout)

        // Turn 1: first attack of the turn -> draw.
        val handTurn1 = d.getHandSize(active)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(scout), opp)
        repeat(6) { if (d.pendingDecision != null) d.autoResolveDecision() else d.bothPass() }
        d.getHandSize(active) shouldBe handTurn1 + 1

        // Advance whole turns until it is the active player's turn again (their next turn).
        do {
            d.passPriorityUntil(Step.END)            // current turn's end step
            d.passPriorityUntil(Step.PRECOMBAT_MAIN) // next turn's precombat main
        } while (d.activePlayer != active)

        // Turn 3 (active again): the window has reset -> draw again.
        val handTurn3 = d.getHandSize(active)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(scout), opp)
        repeat(6) { if (d.pendingDecision != null) d.autoResolveDecision() else d.bothPass() }
        d.getHandSize(active) shouldBe handTurn3 + 1
    }

    test("does NOT fire on a second attack the same turn (extra combat)") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val scout = d.putCreatureOnBattlefield(active, "First-Strike Scout")
        d.removeSummoningSickness(scout)

        // Simulate that the Scout already attacked earlier this turn (the state an extra combat
        // phase produces): seed the per-turn attacker set with it before declaring this combat.
        d.replaceState(
            d.state.updateEntity(active) { container ->
                val prev = container.get<PlayerAttackersThisTurnComponent>()?.attackerIds ?: emptySet()
                container.with(PlayerAttackersThisTurnComponent(prev + scout))
            }
        )

        val handBefore = d.getHandSize(active)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(scout), opp)
        repeat(6) { if (d.pendingDecision != null) d.autoResolveDecision() else d.bothPass() }

        // Not the first attack this turn -> no draw.
        d.getHandSize(active) shouldBe handBefore
    }
})
