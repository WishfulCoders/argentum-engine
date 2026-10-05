package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.matchers.shouldBe

/**
 * Galvanic Blast (SOM #91) — {R} Instant.
 *
 *   Galvanic Blast deals 2 damage to any target.
 *   Metalcraft — Galvanic Blast deals 4 damage instead if you control three or more artifacts.
 */
class GalvanicBlastScenarioTest : ScenarioTestBase() {

    private val trinket = card("Test Blast Trinket") {
        manaCost = "{1}"
        typeLine = "Artifact"
    }

    private fun board(artifacts: Int): TestGame {
        var b = scenario()
            .withPlayers("Caster", "Opponent")
            .withCardInHand(1, "Galvanic Blast")
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(artifacts) { b = b.withCardOnBattlefield(1, "Test Blast Trinket") }
        return b.build()
    }

    init {
        cardRegistry.register(trinket)

        test("two artifacts: 2 damage") {
            val game = board(artifacts = 2)
            game.castSpellTargetingPlayer(1, "Galvanic Blast", 2).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 18
        }

        test("three artifacts (metalcraft): 4 damage") {
            val game = board(artifacts = 3)
            game.castSpellTargetingPlayer(1, "Galvanic Blast", 2).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 16
        }
    }
}
