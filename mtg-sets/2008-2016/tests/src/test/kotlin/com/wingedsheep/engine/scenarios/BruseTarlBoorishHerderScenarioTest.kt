package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Bruse Tarl, Boorish Herder (C16 #30).
 *
 * "{2}{R}{W} Legendary Creature — Human Ally, 3/3
 *  Whenever Bruse Tarl enters or attacks, target creature you control gains double strike and
 *  lifelink until end of turn.
 *  Partner"
 *
 * "Enters or attacks" is modelled as two separate triggers with the same effect (both fire — they
 * are not mutually exclusive). Partner is omitted (the engine plays two-commander-less games), so
 * it's not exercised here.
 */
class BruseTarlBoorishHerderScenarioTest : ScenarioTestBase() {

    init {
        test("entering the battlefield grants a target creature you control double strike and lifelink") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Bruse Tarl, Boorish Herder")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!

            val cast = game.castSpell(1, "Bruse Tarl, Boorish Herder")
            withClue("Casting Bruse Tarl should succeed: ${cast.error}") { cast.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            withClue("The ETB trigger should pause for a target creature you control") {
                game.hasPendingDecision() shouldBe true
            }
            game.selectTargets(listOf(bears))
            game.resolveStack()

            withClue("Grizzly Bears should have double strike and lifelink until end of turn") {
                game.state.projectedState.hasKeyword(bears, Keyword.DOUBLE_STRIKE) shouldBe true
                game.state.projectedState.hasKeyword(bears, Keyword.LIFELINK) shouldBe true
            }
        }

        test("attacking also grants double strike and lifelink to a targeted creature you control") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(1, "Bruse Tarl, Boorish Herder", summoningSickness = false)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            val attack = game.declareAttackers(mapOf("Bruse Tarl, Boorish Herder" to 2))
            withClue("Declaring Bruse Tarl as an attacker should succeed: ${attack.error}") {
                attack.error shouldBe null
            }

            withClue("The attack trigger should pause for a target creature you control") {
                game.hasPendingDecision() shouldBe true
            }
            game.selectTargets(listOf(bears))
            game.resolveStack()

            withClue("Grizzly Bears (a non-attacking creature) should have gained double strike and lifelink") {
                game.state.projectedState.hasKeyword(bears, Keyword.DOUBLE_STRIKE) shouldBe true
                game.state.projectedState.hasKeyword(bears, Keyword.LIFELINK) shouldBe true
            }
        }
    }
}
