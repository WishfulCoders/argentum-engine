package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Griffin Aerie (M21 #22) — {1}{W} Enchantment.
 *
 * "At the beginning of your end step, if you gained 3 or more life this turn, create a 2/2 white
 *  Griffin creature token with flying."
 *
 * An intervening "if" (Starlit Soothsayer's shape, per the 2020-06-23 rulings): no trigger at all
 * without the 3 life, and the life gained counts for the whole turn even before the Aerie was on
 * the battlefield. Mirrors Resplendent Angel's own test of the same pattern; the created-token
 * lookup uses a generic "new creature token" diff rather than a name, since the token's exact card
 * name is an implementation detail of `Effects.CreateToken`.
 */
class GriffinAerieScenarioTest : ScenarioTestBase() {

    // Minimal sorceries that gain the caster life, to drive the "gained N life this turn" tracker.
    private val gainThreeLife = card("Test Gain Three (Griffin)") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(3) }
    }
    private val gainTwoLife = card("Test Gain Two (Griffin)") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(2) }
    }

    init {
        cardRegistry.register(gainThreeLife)
        cardRegistry.register(gainTwoLife)

        context("Griffin Aerie") {

            test("gaining 3 or more life this turn creates a 2/2 flying Griffin at the end step") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Griffin Aerie")
                    .withCardInHand(1, "Test Gain Three (Griffin)")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tokensBefore = creatureTokens(game.state, game.player1Id)

                game.castSpell(1, "Test Gain Three (Griffin)").error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                val newTokens = creatureTokens(game.state, game.player1Id) - tokensBefore
                withClue("gaining 3 life creates exactly one Griffin token at the end step") {
                    newTokens.size shouldBe 1
                }
                val griffin = newTokens.first()
                withClue("the token is a 2/2 white flying Griffin") {
                    game.state.projectedState.getPower(griffin) shouldBe 2
                    game.state.projectedState.getToughness(griffin) shouldBe 2
                    game.state.projectedState.hasKeyword(griffin, Keyword.FLYING) shouldBe true
                    game.state.getEntity(griffin)?.get<CardComponent>()?.typeLine?.subtypes?.map { it.value } shouldBe
                        listOf("Griffin")
                }
            }

            test("gaining only 2 life this turn does not create a token (intervening-if fails)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Griffin Aerie")
                    .withCardInHand(1, "Test Gain Two (Griffin)")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tokensBefore = creatureTokens(game.state, game.player1Id)

                game.castSpell(1, "Test Gain Two (Griffin)").error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                val newTokens = creatureTokens(game.state, game.player1Id) - tokensBefore
                withClue("gaining only 2 life should not create a Griffin token") {
                    newTokens.size shouldBe 0
                }
            }
        }
    }
}

private fun creatureTokens(state: GameState, player: EntityId): Set<EntityId> =
    state.getBattlefield().filter {
        val e = state.getEntity(it) ?: return@filter false
        e.has<TokenComponent>() &&
            e.get<ControllerComponent>()?.playerId == player &&
            e.get<CardComponent>()?.typeLine?.isCreature == true
    }.toSet()
