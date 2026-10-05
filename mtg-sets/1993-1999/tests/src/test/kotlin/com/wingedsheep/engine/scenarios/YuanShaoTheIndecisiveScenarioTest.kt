package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class YuanShaoTheIndecisiveScenarioTest : ScenarioTestBase() {
    init {
        val yuan = "Yuan Shao, the Indecisive"

        for (attacker in listOf("Grizzly Bears", yuan)) {
            test("$attacker permits one blocker but rejects two") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, yuan, summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Wu Light Cavalry")
                    .withCardOnBattlefield(2, "Shu Elite Companions")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf(attacker to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf(
                    "Wu Light Cavalry" to listOf(attacker),
                    "Shu Elite Companions" to listOf(attacker)
                )).error shouldNotBe null
                game.declareBlockers(mapOf("Wu Light Cavalry" to listOf(attacker))).error shouldBe null
            }
        }

        test("horsemanship still excludes ordinary blockers") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, yuan, summoningSickness = false)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf(yuan to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf(yuan))).error shouldNotBe null
        }

        test("opposing creatures are not restricted") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(2, yuan)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(2, "Wu Light Cavalry")
                .withCardOnBattlefield(2, "Shu Elite Companions")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf(
                "Wu Light Cavalry" to listOf("Grizzly Bears"),
                "Shu Elite Companions" to listOf("Grizzly Bears")
            )).error shouldBe null
        }

        test("the restriction ends when Yuan Shao leaves the battlefield") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, yuan)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(2, "Wu Light Cavalry")
                .withCardOnBattlefield(2, "Shu Elite Companions")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.hasKeyword(bears, AbilityFlag.CANT_BE_BLOCKED_BY_MORE_THAN_ONE) shouldBe true
            game.castSpell(1, "Lightning Bolt", game.findPermanent(yuan)!!).error shouldBe null
            game.resolveStack()
            game.findPermanent(yuan) shouldBe null
            game.state.projectedState.hasKeyword(bears, AbilityFlag.CANT_BE_BLOCKED_BY_MORE_THAN_ONE) shouldBe false

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf(
                "Wu Light Cavalry" to listOf("Grizzly Bears"),
                "Shu Elite Companions" to listOf("Grizzly Bears")
            )).error shouldBe null
        }
    }
}
