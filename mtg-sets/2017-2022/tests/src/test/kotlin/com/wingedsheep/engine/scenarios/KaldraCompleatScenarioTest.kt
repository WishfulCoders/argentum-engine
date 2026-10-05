package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Kaldra Compleat (MH2 #227).
 *
 *   Living weapon. Indestructible.
 *   Equipped creature gets +5/+5 and has first strike, trample, indestructible, haste, and
 *   "Whenever this creature deals combat damage to a creature, exile that creature."
 *   Equip {7}
 *
 * Pins the living-weapon Germ, the granted keywords (haste lets the Germ attack at once), and the
 * granted exile trigger firing off first-strike damage to a blocker that survives the damage.
 */
class KaldraCompleatScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        test("living weapon makes a 5/5 Germ with every granted keyword") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Kaldra Compleat")
                .withLandsOnBattlefield(1, "Plains", 7)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpell(1, "Kaldra Compleat")
            withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val germ = game.findPermanent("Phyrexian Germ Token")
            withClue("A Germ token should exist") { germ shouldNotBe null }
            val kaldra = game.findPermanent("Kaldra Compleat")!!
            withClue("Kaldra Compleat is attached to the Germ") {
                game.state.getEntity(kaldra)?.get<AttachedToComponent>()?.targetId shouldBe germ
            }
            val projected = stateProjector.project(game.state)
            withClue("The Germ is a 5/5 with first strike, trample, indestructible and haste") {
                projected.getPower(germ!!) shouldBe 5
                projected.getToughness(germ) shouldBe 5
                projected.hasKeyword(germ, Keyword.FIRST_STRIKE) shouldBe true
                projected.hasKeyword(germ, Keyword.TRAMPLE) shouldBe true
                projected.hasKeyword(germ, Keyword.INDESTRUCTIBLE) shouldBe true
                projected.hasKeyword(germ, Keyword.HASTE) shouldBe true
            }
            withClue("The Equipment itself is indestructible") {
                projected.hasKeyword(kaldra, Keyword.INDESTRUCTIBLE) shouldBe true
            }
        }

        test("combat damage to a blocking creature exiles it") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = true)
                .withCardAttachedTo(1, "Kaldra Compleat", "Grizzly Bears")
                .withCardOnBattlefield(2, "Colossus of Sardia") // 9/9: survives the 7 damage
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            withClue("Granted haste lets the freshly arrived creature attack") {
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            }
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Colossus of Sardia" to listOf("Grizzly Bears"))).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            withClue("The blocker was exiled by the granted trigger") {
                game.isInExile(2, "Colossus of Sardia") shouldBe true
            }
            withClue("The equipped creature survives (first strike, and indestructible)") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
            withClue("No trample damage went through: all 7 was assigned to the blocker") {
                game.getLifeTotal(2) shouldBe 20
            }
        }
    }
}
