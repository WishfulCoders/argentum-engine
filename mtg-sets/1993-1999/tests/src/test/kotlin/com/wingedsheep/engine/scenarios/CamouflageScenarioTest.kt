package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.mechanics.combat.RandomizedBlockerPiles
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull

class CamouflageScenarioTest : ScenarioTestBase() {
    init {
        fun board(active: Int = 1, step: Step = Step.DECLARE_ATTACKERS) = scenario().withPlayers()
            .withCardInHand(1, "Camouflage").withLandsOnBattlefield(1, "Forest", 1)
            .withCardOnBattlefield(1, "Grizzly Bears").withCardOnBattlefield(1, "Savannah Lions")
            .withCardOnBattlefield(2, "Wall of Wood")
            .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
            .withActivePlayer(active).withPriorityPlayer(1)
            .inPhase(if (step == Step.PRECOMBAT_MAIN) Phase.PRECOMBAT_MAIN else Phase.COMBAT, step).build()

        test("cast after attacking and resolve before choosing optional random piles") {
            val game = board()
            game.declareAttackers(mapOf("Grizzly Bears" to 2, "Savannah Lions" to 2)).error shouldBe null
            game.castSpell(1, "Camouflage").error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Camouflage") shouldBe true
            RandomizedBlockerPiles.isActive(game.state) shouldBe true
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.execute(DeclareBlockers(game.player2Id, emptyMap())).error shouldBe null
            val question = game.state.pendingDecision as SplitPilesDecision
            question.numberOfPiles shouldBe 2
            question.allowUnassigned shouldBe true
            val wall = game.findPermanent("Wall of Wood")!!
            game.execute(SubmitDecision(game.player2Id, PilesSplitResponse(question.id, listOf(listOf(wall), emptyList())))).error shouldBe null
            game.state.pendingDecision shouldBe null
            game.state.getEntity(wall)?.get<BlockingComponent>()?.blockedAttackerIds?.size shouldBe 1
        }

        test("main phase and opponent declare attackers step are illegal casting windows") {
            board(step = Step.PRECOMBAT_MAIN).castSpell(1, "Camouflage").error.shouldNotBeNull()
            val game = board(active = 2)
            game.declareAttackers(emptyMap()).error shouldBe null
            game.state = game.state.withPriority(game.player1Id)
            game.castSpell(1, "Camouflage").error.shouldNotBeNull()
        }

        test("all piles may be empty and ordinary assignments cannot bypass the policy") {
            val game = board()
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.castSpell(1, "Camouflage").error shouldBe null
            game.resolveStack()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            val wall = game.findPermanent("Wall of Wood")!!
            val bear = game.findPermanent("Grizzly Bears")!!
            game.execute(DeclareBlockers(game.player2Id, mapOf(wall to listOf(bear)))).error.shouldNotBeNull()
            game.execute(DeclareBlockers(game.player2Id, emptyMap())).error shouldBe null
            val question = game.state.pendingDecision as SplitPilesDecision
            game.execute(SubmitDecision(game.player2Id, PilesSplitResponse(question.id, listOf(emptyList())))).error shouldBe null
            game.state.getEntity(wall)?.get<BlockingComponent>() shouldBe null
        }
    }
}
