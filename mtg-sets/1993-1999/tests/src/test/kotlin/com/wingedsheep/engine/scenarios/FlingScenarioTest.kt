package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Fling (STH #82).
 *
 * "{1}{R} Instant
 *  As an additional cost to cast this spell, sacrifice a creature.
 *  Fling deals damage equal to the sacrificed creature's power to any target."
 *
 * Pins two things: the sacrifice is a genuine additional cost (paid before the spell is even on
 * the stack) and the damage amount is the sacrificed creature's power as *last known on the
 * battlefield* — i.e. it must reflect a static pump the creature had at the moment it was
 * sacrificed, not its printed power, even though the creature is long gone by the time the
 * damage effect runs.
 */
class FlingScenarioTest : ScenarioTestBase() {

    init {
        test("sacrifices the named creature and deals damage equal to its power to a player") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Fling")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpellWithSacrifice(1, "Fling", "Grizzly Bears", targetPlayerNumber = 2)
            withClue("Casting Fling, sacrificing Grizzly Bears, should succeed: ${cast.error}") {
                cast.error shouldBe null
            }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            withClue("Grizzly Bears should have been sacrificed") {
                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }
            withClue("Opponent should take 2 damage — Grizzly Bears' power") {
                game.getLifeTotal(2) shouldBe 18
            }
            withClue("The caster's life total is untouched") {
                game.getLifeTotal(1) shouldBe 20
            }
        }

        test("damage uses the sacrificed creature's last-known (pumped) power, not its printed power") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Fling")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Unholy Strength", "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            val cardId = game.state.getHand(game.player1Id).find { id ->
                game.state.getEntity(id)?.get<CardComponent>()?.name == "Fling"
            } ?: error("Fling not found in hand")

            val cast = game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = cardId,
                    targets = listOf(ChosenTarget.Permanent(giant)),
                    additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears))
                )
            )
            withClue("Casting Fling at Hill Giant, sacrificing the enchanted Grizzly Bears, should succeed: ${cast.error}") {
                cast.error shouldBe null
            }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            withClue("Grizzly Bears (2/2 base + Unholy Strength's +2/+1 = 4 power) should deal 4 damage") {
                // Hill Giant is a 3/3; 4 damage is lethal.
                game.isOnBattlefield("Hill Giant") shouldBe false
            }
            withClue("The opponent takes no life loss — the damage went to their creature") {
                game.getLifeTotal(2) shouldBe 20
            }
        }
    }
}
