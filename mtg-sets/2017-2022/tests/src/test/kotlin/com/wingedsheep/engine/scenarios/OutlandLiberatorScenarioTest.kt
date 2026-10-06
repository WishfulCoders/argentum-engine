package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.DayNight
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Outland Liberator // Frenzied Trapbreaker (MID #190).
 *
 * The night face's "whenever this creature attacks, destroy target artifact or enchantment
 * defending player controls" reads the defending player off the attacker's own combat at target
 * selection, so the attacker's controller's own artifacts are never offered.
 */
class OutlandLiberatorScenarioTest : ScenarioTestBase() {

    init {
        test("cast at night it is Frenzied Trapbreaker, whose attack trigger destroys the defender's artifact") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Outland Liberator")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardOnBattlefield(1, "Sol Ring")
                .withCardOnBattlefield(2, "Ornithopter")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.state = game.state.copy(dayNight = DayNight.NIGHT)

            game.castSpell(1, "Outland Liberator").error shouldBe null
            game.resolveStack()
            val trapbreaker = game.findPermanent("Frenzied Trapbreaker")
            withClue("a daybound permanent enters night-face up at night") { trapbreaker shouldNotBe null }
            game.state = game.state.updateEntity(trapbreaker!!) {
                it.without<com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent>()
            }

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Frenzied Trapbreaker" to 2)).error shouldBe null

            val ornithopter = game.findPermanent("Ornithopter")!!
            val solRing = game.findPermanent("Sol Ring")!!
            val decision = game.getPendingDecision()
            withClue("the attack trigger asks for a target") { (decision is ChooseTargetsDecision) shouldBe true }
            val legal = (decision as ChooseTargetsDecision).legalTargets.values.flatten()
            withClue("only the defending player's artifacts and enchantments are legal") {
                legal.contains(ornithopter) shouldBe true
                legal.contains(solRing) shouldBe false
            }
            game.selectTargets(listOf(ornithopter)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Ornithopter") shouldBe true
            game.findPermanent("Sol Ring") shouldNotBe null
        }
    }
}
