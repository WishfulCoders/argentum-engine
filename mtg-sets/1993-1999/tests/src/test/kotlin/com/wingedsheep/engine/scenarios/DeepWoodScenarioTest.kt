package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Deep Wood (POR #160) — "Cast this spell only during the declare attackers step and only if you've
 * been attacked this step. Prevent all damage that would be dealt to you this turn by attacking
 * creatures."
 *
 * The controller-only recipient shield narrowed to attacking sources
 * (`PreventDamage(alsoToYou = true, sources = Matching(Creature.attacking()))`): an unblocked
 * attacker's combat damage to the caster is prevented, while the same attacker's damage to a
 * blocking creature is not — only "you" are protected.
 */
class DeepWoodScenarioTest : ScenarioTestBase() {

    init {
        context("Deep Wood") {
            test("attacking creatures deal the caster no damage, but still damage blockers") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Deep Wood")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(1, "Savannah Lions", summoningSickness = false) // 2/1
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false) // 3/3
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false) // 2/2
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val before = game.getLifeTotal(1)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 1, "Grizzly Bears" to 1)).error shouldBe null
                game.passPriority()
                game.castSpell(1, "Deep Wood").error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Savannah Lions" to listOf("Hill Giant"))).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("the unblocked attacker's 2 damage to the caster is prevented") {
                    game.getLifeTotal(1) shouldBe before
                }
                withClue("only the caster is protected — the blocking Lions still dies to the Giant") {
                    game.isInGraveyard(1, "Savannah Lions") shouldBe true
                }
            }
        }
    }
}
