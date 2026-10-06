package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Personal Incarnation (LEA) — both abilities name the creature's *owner*, so the interesting
 * cases split ownership from control (a simulated Control Magic): only the owner may activate the
 * redirect, the redirected damage goes to the owner, and the dies trigger costs the owner — not
 * the thief who controlled it — half their life, rounded up.
 */
class PersonalIncarnationScenarioTest : ScenarioTestBase() {

    private fun TestGame.activateIncarnation(playerNumber: Int) = execute(
        ActivateAbility(
            playerId = if (playerNumber == 1) player1Id else player2Id,
            sourceId = findPermanent("Personal Incarnation") ?: error("Personal Incarnation not on battlefield"),
            abilityId = cardRegistry.getCard("Personal Incarnation")!!.activatedAbilities[0].id,
        )
    )

    private fun TestGame.stealIncarnation() {
        val incarnation = findPermanent("Personal Incarnation") ?: error("Personal Incarnation not on battlefield")
        state = state.updateEntity(incarnation) { it.with(ControllerComponent(player2Id)) }
    }

    init {
        test("owner redirects 1 of a Lightning Bolt's 3 damage to themselves") {
            val game = scenario()
                .withPlayers("Owner", "Opponent")
                .withCardOnBattlefield(1, "Personal Incarnation")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.activateIncarnation(1).error.shouldBeNull()
            game.resolveStack()

            val incarnation = game.findPermanent("Personal Incarnation")!!
            game.passPriority() // owner passes on an empty stack; the opponent gets priority
            game.castSpell(2, "Lightning Bolt", incarnation).error.shouldBeNull()
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 19
            game.state.getEntity(incarnation)?.get<DamageComponent>()?.amount shouldBe 2
        }

        test("once stolen, the thief cannot activate it but the owner can, and the owner takes the damage") {
            val game = scenario()
                .withPlayers("Owner", "Thief")
                .withCardOnBattlefield(1, "Personal Incarnation")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2)
                .withPriorityPlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.stealIncarnation()

            // The thief controls it, but only its owner may activate the ability.
            game.activateIncarnation(2).error.shouldNotBeNull()

            game.passPriority() // thief passes on an empty stack; the owner gets priority
            game.activateIncarnation(1).error.shouldBeNull()
            game.resolveStack()

            val incarnation = game.findPermanent("Personal Incarnation")!!
            // Hand priority to the thief for the Bolt, whichever player the engine left it with.
            if (game.state.priorityPlayerId != game.player2Id) game.passPriority()
            game.castSpell(2, "Lightning Bolt", incarnation).error.shouldBeNull()
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 19
            game.getLifeTotal(2) shouldBe 20
            game.state.getEntity(incarnation)?.get<DamageComponent>()?.amount shouldBe 2
        }

        test("when it dies under a thief's control, its owner loses half their life, rounded up") {
            val game = scenario()
                .withPlayers("Owner", "Thief")
                .withCardOnBattlefield(1, "Personal Incarnation")
                .withCardInHand(1, "Terror")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withLifeTotal(1, 15)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.stealIncarnation()

            val incarnation = game.findPermanent("Personal Incarnation")!!
            game.castSpell(1, "Terror", incarnation).error.shouldBeNull()
            game.resolveStack()

            game.isInGraveyard(1, "Personal Incarnation") shouldBe true
            game.getLifeTotal(1) shouldBe 7
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
