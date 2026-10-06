package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A mid-resolution "choose a creature or player" (`selectTarget(Targets.CreatureOrPlayer,
 * nonTargeting = true)`) is a choice, not a target (CR 115.10 — only the word "target" makes a
 * target), so hexproof and shroud don't remove a creature from it (Comet, Stellar Pup: "a creature
 * or player", chosen on resolution per its ruling). The targeting form of the same requirement
 * still excludes them.
 */
class NonTargetingCreatureOrPlayerChoiceScenarioTest : ScenarioTestBase() {

    private fun zap(name: String, nonTargeting: Boolean) = card(name) {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Choose a creature or player. This deals 2 damage to it."
        spell {
            effect = Effects.Pipeline {
                val chosen = selectTarget(Targets.CreatureOrPlayer, nonTargeting = nonTargeting)
                run(Effects.DealDamage(2, chosen.asTarget))
            }
        }
    }

    private val hexBear = card("Test Hexproof Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywords(Keyword.HEXPROOF)
    }

    private val shroudBear = card("Test Shroud Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywords(Keyword.SHROUD)
    }

    private fun board(spell: String): TestGame = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, spell)
        .withCardOnBattlefield(2, "Test Hexproof Bear")
        .withCardOnBattlefield(2, "Test Shroud Bear")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        listOf(zap("Test Choice Zap", true), zap("Test Target Zap", false), hexBear, shroudBear)
            .forEach { cardRegistry.register(it) }

        test("a non-targeting choice offers hexproof and shroud creatures, and the damage lands") {
            val game = board("Test Choice Zap")
            game.castSpell(1, "Test Choice Zap").error shouldBe null
            game.resolveStack()
            val hex = game.findPermanent("Test Hexproof Bear")!!
            val shroud = game.findPermanent("Test Shroud Bear")!!
            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            val offered = decision.legalTargets[0]!!
            offered shouldContain hex
            offered shouldContain shroud
            offered shouldContain game.player1Id
            offered shouldContain game.player2Id
            game.selectTargets(listOf(hex)).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Test Hexproof Bear") shouldBe false
        }

        test("the targeting form still excludes them") {
            val game = board("Test Target Zap")
            game.castSpell(1, "Test Target Zap").error shouldBe null
            game.resolveStack()
            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            val offered = decision.legalTargets[0]!!
            offered shouldNotContain game.findPermanent("Test Hexproof Bear")!!
            offered shouldNotContain game.findPermanent("Test Shroud Bear")!!
            offered shouldContain game.player2Id
        }
    }
}
