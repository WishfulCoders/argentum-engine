package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Scenario test for `Effects.UntapEachTarget()` — the untap twin of `Effects.TapEachTarget()`
 * ("untap each of those target creatures"). Like its tap counterpart it composes
 * `ForEachTargetEffect` over `Effects.Untap`, so the count is owned entirely by the spell's
 * `TargetCreature` (here "up to three") and never duplicated on the effect.
 *
 * No printed card uses the facade yet (it backs the mtgish auto-generator's `UntapEachPermanent`
 * rendering), so this pins the wiring through an inline test sorcery.
 */
class UntapEachTargetScenarioTest : ScenarioTestBase() {

    private fun TestGame.handCardId(playerNumber: Int, name: String): EntityId {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getHand(playerId).first {
            state.getEntity(it)?.get<CardComponent>()?.name == name
        }
    }

    private fun TestGame.isTapped(id: EntityId): Boolean =
        state.getEntity(id)?.has<TappedComponent>() == true

    init {
        cardRegistry.register(
            card("Test Untap Surge") {
                manaCost = "{1}{U}"
                typeLine = "Sorcery"
                oracleText = "Untap up to three target creatures."
                spell {
                    targets(TargetFilter.Creature, count = 3, optional = true)
                    effect = Effects.UntapEachTarget()
                }
            }
        )

        context("Effects.UntapEachTarget — fixed count (up to three)") {
            test("untaps every targeted creature, leaves untargeted ones tapped") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Test Untap Surge")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findAllPermanents("Grizzly Bears")
                bears.size shouldBe 3
                withClue("all three bears start tapped") {
                    bears.all { game.isTapped(it) } shouldBe true
                }

                val targeted = bears.take(2)
                val untargeted = bears.drop(2)

                val cast = game.execute(
                    CastSpell(
                        game.player1Id,
                        game.handCardId(1, "Test Untap Surge"),
                        targeted.map { ChosenTarget.Permanent(it) }
                    )
                )
                withClue("Test Untap Surge should cast: ${cast.error}") { cast.error shouldBe null }

                game.resolveStack()

                withClue("both targeted creatures should be untapped") {
                    targeted.none { game.isTapped(it) } shouldBe true
                }
                withClue("the untargeted creature should remain tapped") {
                    untargeted.all { game.isTapped(it) } shouldBe true
                }
            }
        }
    }
}
