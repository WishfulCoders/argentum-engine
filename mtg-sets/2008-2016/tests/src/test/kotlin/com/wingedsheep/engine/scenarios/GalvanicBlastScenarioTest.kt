package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Galvanic Blast (SOM).
 *
 * "{R} Instant
 *  Galvanic Blast deals 2 damage to any target.
 *  Metalcraft — Galvanic Blast deals 4 damage instead if you control three or more artifacts."
 *
 * Pins the ordinary 2-damage case, the upgraded 4-damage metalcraft case (three artifacts), and
 * that two artifacts (one short of metalcraft) still only deals 2.
 */
class GalvanicBlastScenarioTest : ScenarioTestBase() {

    init {
        context("Galvanic Blast") {

            test("deals 2 damage to any target without metalcraft") {
                val game = scenario()
                    .withPlayers("Caster", "Victim")
                    .withCardInHand(1, "Galvanic Blast")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Galvanic Blast", targetId = giant).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val damage = game.state.getEntity(giant)?.get<DamageComponent>()?.amount ?: 0
                withClue("A 3/3 Hill Giant should survive 2 damage, marked as damage taken") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                    damage shouldBe 2
                }
            }

            test("deals 4 damage instead with metalcraft (three or more artifacts)") {
                val game = scenario()
                    .withPlayers("Caster", "Victim")
                    .withCardInHand(1, "Galvanic Blast")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(1, "Voltaic Key")
                    .withCardOnBattlefield(1, "Voltaic Key")
                    .withCardOnBattlefield(1, "Voltaic Key")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Galvanic Blast", targetId = giant).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Metalcraft's 4 damage should kill a 3/3 Hill Giant") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }
            }

            test("two artifacts is not metalcraft — still only 2 damage") {
                val game = scenario()
                    .withPlayers("Caster", "Victim")
                    .withCardInHand(1, "Galvanic Blast")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(1, "Voltaic Key")
                    .withCardOnBattlefield(1, "Voltaic Key")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Galvanic Blast", targetId = giant).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Only 2 artifacts — metalcraft is not active, a 3/3 Hill Giant survives") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
            }
        }
    }
}
