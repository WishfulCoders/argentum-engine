package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Yotian Frontliner (BRO #42) — {1} Artifact Creature — Soldier, 1/1.
 *
 * "Whenever this creature attacks, another target creature you control gets +1/+1 until end of turn.
 *  Unearth {W}"
 *
 * Covers the attack trigger and the unearth keyword (CR 702.84a): return with haste, exile at the
 * beginning of the next end step, and exile instead of leaving the battlefield any other way.
 */
class YotianFrontlinerScenarioTest : ScenarioTestBase() {

    private val unearthAbilityId =
        cardRegistry.getCard("Yotian Frontliner")!!.activatedAbilities.first().id

    private fun unearthGame() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInGraveyard(1, "Yotian Frontliner")
        .withLandsOnBattlefield(1, "Plains", 1)
        .withCardInHand(1, "Unsummon")
        .withLandsOnBattlefield(1, "Island", 1)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.unearth() {
        val frontliner = findCardsInGraveyard(1, "Yotian Frontliner").first()
        val result = execute(
            ActivateAbility(playerId = player1Id, sourceId = frontliner, abilityId = unearthAbilityId)
        )
        withClue("Activating unearth should succeed: ${result.error}") { result.error shouldBe null }
        resolveStack()
    }

    init {
        context("Yotian Frontliner") {

            test("attacking gives another target creature you control +1/+1 until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Yotian Frontliner", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Yotian Frontliner" to 2)).error shouldBe null
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears is 3/3 after the trigger") {
                    game.state.projectedState.getPower(bears) shouldBe 3
                    game.state.projectedState.getToughness(bears) shouldBe 3
                }
            }

            test("unearth returns it with haste, and it is exiled at the beginning of the next end step") {
                val game = unearthGame()
                game.unearth()

                val frontliner = game.findPermanent("Yotian Frontliner")
                withClue("Yotian Frontliner is back on the battlefield") { (frontliner != null) shouldBe true }
                withClue("It gained haste") {
                    game.state.projectedState.hasKeyword(frontliner!!, Keyword.HASTE) shouldBe true
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Exiled at the beginning of the end step") {
                    game.isOnBattlefield("Yotian Frontliner") shouldBe false
                    game.isInExile(1, "Yotian Frontliner") shouldBe true
                }
            }

            test("if the unearthed creature would leave the battlefield, it is exiled instead") {
                val game = unearthGame()
                game.unearth()

                val frontliner = game.findPermanent("Yotian Frontliner")!!
                game.castSpell(1, "Unsummon", frontliner).error shouldBe null
                game.resolveStack()

                withClue("Bounced to hand is replaced by exile") {
                    game.isInHand(1, "Yotian Frontliner") shouldBe false
                    game.isInExile(1, "Yotian Frontliner") shouldBe true
                }
            }

            test("unearth can't be activated outside sorcery timing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Yotian Frontliner")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val frontliner = game.findCardsInGraveyard(1, "Yotian Frontliner").first()
                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = frontliner, abilityId = unearthAbilityId)
                )
                withClue("Unearth on the opponent's turn is rejected") { (result.error != null) shouldBe true }
                game.findCardsInGraveyard(1, "Yotian Frontliner").size shouldBe 1
            }
        }
    }
}
