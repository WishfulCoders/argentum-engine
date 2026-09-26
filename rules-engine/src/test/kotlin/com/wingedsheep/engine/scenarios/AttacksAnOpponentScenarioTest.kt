package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import com.wingedsheep.sdk.scripting.events.AttackPredicate

/**
 * `AttackPredicate.DefenderIsPlayer` / `Triggers.self.attacks(setOf(AttackPredicate.DefenderIsPlayer))` — "Whenever this creature
 * attacks a player (an opponent), …" (engine gap #1258, discovered adding Kaalia of the Vast).
 *
 * A creature is declared as attacking a player, a planeswalker, or a battle (CR 508.1). The
 * unfiltered `Triggers.self.attacks()` fires regardless of that choice; this predicate gates the trigger
 * to the *player* case only. Kaalia's 2024 ruling makes this concrete: her ability "doesn't
 * trigger if it attacks a planeswalker or battle."
 *
 * The defender kind is fixed at declaration and the `AttackersDeclaredEvent` carries no
 * per-attacker defender identity, so the player-vs-permanent fact is stamped on the event at
 * declaration (`attackersAgainstPlayer`) rather than re-derived downstream — mirroring the
 * `firstTimeAttackers` treatment for `AttackPredicate.FirstTimeEachTurn`.
 *
 * Battles: the engine does not yet model battles (no `CardType.BATTLE`) and attack validation only
 * accepts an opponent player or an opponent's planeswalker as a defender
 * (`AttackPhaseManager.validateAttacker` defender check). Because the predicate is defined
 * positively — "the defender is a player" (`defenderId in turnOrder`) — a battle defender would
 * fall out of `attackersAgainstPlayer` exactly like a planeswalker does, so the planeswalker case
 * below is the representative "non-player defender" test until battles are wired in.
 */
class AttacksAnOpponentScenarioTest : FunSpec({

    // Proof creature: a 2/2 that draws a card when it attacks a player (an opponent).
    val opponentStriker = card("Opponent Striker") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Scout"
        power = 2
        toughness = 2
        oracleText = "Whenever Opponent Striker attacks a player, draw a card."
        triggeredAbility {
            trigger = Triggers.self.attacks(setOf(AttackPredicate.DefenderIsPlayer))
            effect = Effects.DrawCards(1)
        }
    }

    // A minimal opponent planeswalker the striker can attack instead of the player.
    val testWalker = card("Test Walker") {
        manaCost = "{2}"
        typeLine = "Legendary Planeswalker — Tester"
        startingLoyalty = 3
        loyaltyAbility(1) {
            effect = Effects.GainLife(1)
        }
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(
            TestCards.all +
                com.wingedsheep.mtg.sets.tokens.PredefinedTokens.allTokens +
                listOf(opponentStriker, testWalker)
        )
        return d
    }

    test("fires when the creature attacks a player") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val striker = d.putCreatureOnBattlefield(active, "Opponent Striker")
        d.removeSummoningSickness(striker)

        val handBefore = d.getHandSize(active)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(striker), opp).error shouldBe null
        repeat(6) { if (d.pendingDecision != null) d.autoResolveDecision() else d.bothPass() }

        // Attacked a player -> the trigger fired and drew a card.
        d.getHandSize(active) shouldBe handBefore + 1
    }

    test("does NOT fire when the creature attacks a planeswalker") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent's planeswalker, seeded with loyalty so an SBA doesn't destroy it before combat.
        val walker = d.putPermanentOnBattlefield(opp, "Test Walker")
        d.replaceState(
            d.state.updateEntity(walker) { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.LOYALTY, 3))
            }
        )

        val striker = d.putCreatureOnBattlefield(active, "Opponent Striker")
        d.removeSummoningSickness(striker)

        val handBefore = d.getHandSize(active)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        // Attack the planeswalker, not the player.
        d.declareAttackers(active, mapOf(striker to walker)).error shouldBe null
        repeat(6) { if (d.pendingDecision != null) d.autoResolveDecision() else d.bothPass() }

        // Attacked a planeswalker -> the "attacks a player" trigger must NOT fire.
        d.getHandSize(active) shouldBe handBefore
    }
})
