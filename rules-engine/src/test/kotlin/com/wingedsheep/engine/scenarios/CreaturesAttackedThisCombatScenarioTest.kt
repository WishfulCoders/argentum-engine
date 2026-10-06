package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.combat.PlayerAttackersThisCombatComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Feature test for `Conditions.CreaturesAttackedThisCombat` /
 * [com.wingedsheep.sdk.scripting.conditions.PlayerAttackedWithCreaturesThisCombat], driven through an
 * inline creature with Kytheon, Hero of Akros's trigger shape: "At end of combat, if this creature and
 * at least two other creatures attacked this combat, you gain 5 life."
 *
 * Pins: the count is of creatures *declared* as attackers this combat (CR 508.1), so one that died in
 * combat still counts (Kytheon ruling 2015-06-22); fewer than two others, or the source not attacking,
 * fails the intervening-if (CR 603.4); and the record is gone once the combat phase ends (CR 511.3).
 */
class CreaturesAttackedThisCombatScenarioTest : ScenarioTestBase() {

    private val captain = card("Combat Captain") {
        manaCost = "{W}"
        typeLine = "Creature — Human Soldier"
        power = 2
        toughness = 2
        oracleText = "At end of combat, if this creature and at least two other creatures attacked this combat, you gain 5 life."
        triggeredAbility {
            trigger = Triggers.anyPlayer.beginningOf(Step.END_COMBAT)
            interveningIf = Conditions.All(
                Conditions.SourceAttackedThisCombat,
                Conditions.CreaturesAttackedThisCombat(2, GameObjectFilter.Any.notSourceItself())
            )
            effect = Effects.GainLife(5)
        }
    }

    private fun board() = scenario()
        .withPlayers("Attacker", "Defender")
        .withCardOnBattlefield(1, "Combat Captain")
        .withCardOnBattlefield(1, "Savannah Lions")
        .withCardOnBattlefield(1, "Goblin Guide")
        .withCardOnBattlefield(1, "Centaur Courser")
        .withCardOnBattlefield(2, "Force of Nature")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    /** Advance from declare attackers through end of combat, resolving the end-of-combat trigger. */
    private fun TestGame.finishCombat() {
        passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
        resolveStack()
    }

    init {
        cardRegistry.register(captain)

        test("the source and two other attackers satisfy it") {
            val game = board()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(
                mapOf("Combat Captain" to 2, "Savannah Lions" to 2, "Goblin Guide" to 2)
            ).error shouldBe null
            withClue("the per-combat record holds the declared attackers") {
                game.state.getEntity(game.player1Id)!!.get<PlayerAttackersThisCombatComponent>()!!
                    .attackerIds.size shouldBe 3
            }
            game.finishCombat()
            game.getLifeTotal(1) shouldBe 25
        }

        test("an attacker that died in combat still counts as having attacked this combat") {
            val game = board()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(
                mapOf("Combat Captain" to 2, "Savannah Lions" to 2, "Goblin Guide" to 2)
            ).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Force of Nature" to listOf("Savannah Lions"))).error shouldBe null
            game.finishCombat()
            withClue("Savannah Lions died to the blocker") {
                game.isInGraveyard(1, "Savannah Lions") shouldBe true
            }
            game.getLifeTotal(1) shouldBe 25
        }

        test("only one other attacker: no trigger") {
            val game = board()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Combat Captain" to 2, "Savannah Lions" to 2)).error shouldBe null
            game.finishCombat()
            game.getLifeTotal(1) shouldBe 20
        }

        test("three other attackers but not the source: no trigger") {
            val game = board()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(
                mapOf("Savannah Lions" to 2, "Goblin Guide" to 2, "Centaur Courser" to 2)
            ).error shouldBe null
            game.finishCombat()
            game.getLifeTotal(1) shouldBe 20
        }

        test("the record is cleared when the combat phase ends") {
            val game = board()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(
                mapOf("Combat Captain" to 2, "Savannah Lions" to 2, "Goblin Guide" to 2)
            ).error shouldBe null
            game.finishCombat()
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.state.getEntity(game.player1Id)!!.has<PlayerAttackersThisCombatComponent>() shouldBe false
        }
    }
}
