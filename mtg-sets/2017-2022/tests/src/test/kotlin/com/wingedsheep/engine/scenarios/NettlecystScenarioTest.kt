package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Nettlecyst (MH2 #231).
 *
 *   Living weapon. Equipped creature gets +1/+1 for each artifact and/or enchantment you control.
 *   Equip {2}
 *
 * Pins that the bonus counts Nettlecyst itself (so the Germ survives as a 1/1 on its own), counts
 * artifacts and enchantments you control, and ignores an opponent's.
 */
class NettlecystScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        test("the Germ counts Nettlecyst itself plus your other artifacts and enchantments") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Nettlecyst")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardOnBattlefield(1, "Ornithopter")      // artifact
                .withCardOnBattlefield(1, "Glorious Anthem")  // enchantment
                .withCardOnBattlefield(2, "Ornithopter")      // opponent's — doesn't count
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpell(1, "Nettlecyst")
            withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val germ = game.findPermanent("Phyrexian Germ Token")
            withClue("The Germ survives") { germ shouldNotBe null }
            val projected = stateProjector.project(game.state)
            withClue("+3/+3 (Nettlecyst, Ornithopter, Anthem) and +1/+1 from Glorious Anthem") {
                projected.getPower(germ!!) shouldBe 4
                projected.getToughness(germ) shouldBe 4
            }
        }

        test("alone, Nettlecyst makes a 1/1 Germ") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Nettlecyst")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Nettlecyst").error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val germ = game.findPermanent("Phyrexian Germ Token")!!
            val projected = stateProjector.project(game.state)
            projected.getPower(germ) shouldBe 1
            projected.getToughness(germ) shouldBe 1
        }
    }
}
