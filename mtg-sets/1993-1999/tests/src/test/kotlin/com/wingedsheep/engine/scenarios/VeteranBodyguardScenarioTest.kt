package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Veteran Bodyguard (LEA #41).
 *
 * "As long as this creature is untapped, all damage that would be dealt to you by unblocked
 *  creatures is dealt to this creature instead."
 *
 * First static redirect keyed on the *unblocked* combat status of the damage source: proves the
 * untapped gate, that trample damage from a blocked attacker is not redirected (2004-10-04
 * ruling), and that the Bodyguard still soaks unblocked damage while it is itself blocking.
 */
class VeteranBodyguardScenarioTest : ScenarioTestBase() {

    init {
        context("Veteran Bodyguard") {

            fun damageOn(game: TestGame, name: String): Int =
                game.state.getEntity(game.findPermanent(name)!!)?.get<DamageComponent>()?.amount ?: 0

            test("untapped: an unblocked attacker's combat damage is dealt to the Bodyguard instead") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Veteran Bodyguard")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(emptyMap()).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("the defending player took no damage") { game.getLifeTotal(2) shouldBe 20 }
                withClue("the Bodyguard took the 2 damage") { damageOn(game, "Veteran Bodyguard") shouldBe 2 }
            }

            test("tapped: the redirect is off") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Veteran Bodyguard", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(emptyMap()).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                game.getLifeTotal(2) shouldBe 18
                damageOn(game, "Veteran Bodyguard") shouldBe 0
            }

            test("trample damage from a blocked attacker is not redirected") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Colossal Dreadmaw")
                    .withCardOnBattlefield(2, "Veteran Bodyguard")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Colossal Dreadmaw" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Colossal Dreadmaw"))).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("4 trample damage reached the player (6 power, 2 lethal to the blocker)") {
                    game.getLifeTotal(2) shouldBe 16
                }
                withClue("the Bodyguard took nothing from the blocked trampler") {
                    damageOn(game, "Veteran Bodyguard") shouldBe 0
                }
            }

            test("works while the Bodyguard is blocking another attacker") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(2, "Veteran Bodyguard")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2, "Craw Wurm" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Veteran Bodyguard" to listOf("Grizzly Bears"))).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("the unblocked Craw Wurm's 6 damage was redirected and killed the Bodyguard (2 + 6 >= 5)") {
                    game.getLifeTotal(2) shouldBe 20
                    game.isInGraveyard(2, "Veteran Bodyguard") shouldBe true
                }
            }
        }
    }
}
