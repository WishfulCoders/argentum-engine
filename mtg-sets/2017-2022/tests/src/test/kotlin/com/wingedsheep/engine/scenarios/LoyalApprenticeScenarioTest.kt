package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Loyal Apprentice (C18 #23) — "Lieutenant — At the beginning of combat on your turn, if you
 * control your commander, create a 1/1 colorless Thopter artifact creature token with flying.
 * That token gains haste until end of turn."
 *
 * Pins the intervening-if: it is *your* commander that counts — not any commander — and the
 * granted haste on the freshly made Thopter.
 */
class LoyalApprenticeScenarioTest : ScenarioTestBase() {

    private fun game(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Loyal Apprentice")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Savannah Lions")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("controlling your commander makes a flying Thopter that has haste this turn") {
            val game = game()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.state = game.state.updateEntity(bears) { it.with(CommanderComponent(ownerId = game.player1Id)) }

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.resolveStack()

            val thopter = game.findAllPermanents("Thopter Token").single()
            val projected = game.state.projectedState
            projected.hasType(thopter, "ARTIFACT") shouldBe true
            projected.hasSubtype(thopter, "Thopter") shouldBe true
            projected.hasKeyword(thopter, Keyword.FLYING) shouldBe true
            projected.hasKeyword(thopter, Keyword.HASTE) shouldBe true
            projected.getPower(thopter) shouldBe 1
            projected.getToughness(thopter) shouldBe 1
        }

        test("without a commander on the battlefield nothing is created") {
            val game = game()

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.resolveStack()

            game.findAllPermanents("Thopter Token") shouldHaveSize 0
        }

        test("controlling an opponent's commander is not controlling your commander") {
            val game = game()
            val lions = game.findPermanent("Savannah Lions")!!
            game.state = game.state.updateEntity(lions) {
                it.with(CommanderComponent(ownerId = game.player2Id)).with(ControllerComponent(game.player1Id))
            }
            game.state.projectedState.getController(lions) shouldBe game.player1Id

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.resolveStack()

            game.findAllPermanents("Thopter Token") shouldHaveSize 0
        }
    }
}
