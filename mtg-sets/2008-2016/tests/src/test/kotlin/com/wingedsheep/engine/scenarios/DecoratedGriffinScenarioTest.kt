package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Decorated Griffin (THS #7) — {4}{W} Creature — Griffin 2/3.
 *
 *   Flying
 *   {1}{W}: Prevent the next 1 combat damage that would be dealt to you this turn.
 *
 * The shield is combat-only: noncombat damage (a Shock) must pass through it untouched and leave it
 * up for the combat damage it was bought for.
 */
class DecoratedGriffinScenarioTest : ScenarioTestBase() {

    private val shieldAbilityId =
        cardRegistry.getCard("Decorated Griffin")!!.script.activatedAbilities.single().id

    init {
        test("a noncombat Shock neither is prevented by nor uses up the combat-only shield") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Decorated Griffin")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(2, "Shock")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // P2 Shocks P1; P1 responds by raising the shield, which resolves first.
            game.castSpellTargetingPlayer(2, "Shock", 1).error shouldBe null
            game.passPriority().error shouldBe null
            val griffin = game.findPermanent("Decorated Griffin")!!
            game.execute(ActivateAbility(game.player1Id, griffin, shieldAbilityId)).error shouldBe null
            game.resolveStack()

            withClue("Shock's 2 noncombat damage is dealt in full") {
                game.getLifeTotal(1) shouldBe 18
            }

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("the untouched shield prevents 1 of the Bears' 2 combat damage") {
                game.getLifeTotal(1) shouldBe 17
            }
        }
    }
}
