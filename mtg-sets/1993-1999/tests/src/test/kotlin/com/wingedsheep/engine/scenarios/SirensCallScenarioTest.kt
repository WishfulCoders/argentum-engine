package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ControlHistory
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class SirensCallScenarioTest : ScenarioTestBase() {
    init {
        fun board() = scenario().withPlayers()
            .withCardInHand(1, "Siren's Call")
            .withLandsOnBattlefield(1, "Island", 1)
            .withCardOnBattlefield(1, "Craw Wurm")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant", tapped = true)
            .withCardOnBattlefield(2, "Wall of Wood")
            .withCardOnBattlefield(1, "Gray Ogre", summoningSickness = true)
            .withCardInLibrary(1, "Island").withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Forest").withCardInLibrary(2, "Forest")
            .withActivePlayer(2).withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

        test("forces attacks and destroys the active player's non-Wall stay-at-homes it controlled all turn") {
            val game = board()
            // Player 2 takes Gray Ogre this turn (so it's summoning sick and can't attack): it's
            // exempt from the end-step sweep because it wasn't controlled since the turn began.
            val ogre = game.findPermanent("Gray Ogre")!!
            game.state = ControlHistory.beginTurn(game.state)
            game.state = ControlHistory.record(
                game.state.updateEntity(ogre) { it.with(ControllerComponent(game.player2Id)) }, emptyList()
            )
            game.state.controlAtTurnStart!!.containsKey(ogre) shouldBe false

            game.castSpell(1, "Siren's Call").error shouldBe null
            game.resolveStack()
            game.state.delayedTriggers.size shouldBe 1
            game.state.getEntity(game.findPermanent("Grizzly Bears")!!)!!
                .has<com.wingedsheep.engine.state.components.combat.MustAttackThisTurnComponent>() shouldBe true

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.execute(DeclareAttackers(game.player2Id, emptyMap())).error shouldNotBe null
            game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.findPermanent("Grizzly Bears") shouldNotBe null
            game.findPermanent("Wall of Wood") shouldNotBe null
            game.findPermanent("Gray Ogre") shouldNotBe null
            game.findPermanent("Craw Wurm") shouldNotBe null
        }

        test("cannot be cast on your own turn or after attackers are declared") {
            val own = board()
            own.state = own.state.copy(activePlayerId = own.player1Id)
            own.castSpell(1, "Siren's Call").error shouldNotBe null
            val late = board()
            late.state = late.state.copy(phase = Phase.POSTCOMBAT_MAIN, step = Step.POSTCOMBAT_MAIN)
            late.castSpell(1, "Siren's Call").error shouldNotBe null
        }
    }
}
