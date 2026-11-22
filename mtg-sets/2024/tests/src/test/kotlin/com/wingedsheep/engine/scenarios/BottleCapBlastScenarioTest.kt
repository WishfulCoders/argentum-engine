package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Bottle-Cap Blast (Fallout #55) — {4}{R} Instant, Improvise:
 *   Bottle-Cap Blast deals 5 damage to any target. If excess damage was dealt to a permanent this
 *   way, create that many tapped Treasure tokens.
 *
 * Pins the excess (CR 120.4a) feeding a tapped-Treasure count, and that a player target — which
 * has no excess — yields no Treasure.
 */
class BottleCapBlastScenarioTest : ScenarioTestBase() {

    private fun game(vararg opponentPermanents: String): ScenarioTestBase.TestGame {
        var builder = scenario().withPlayers()
            .withCardInHand(1, "Bottle-Cap Blast")
            .withLandsOnBattlefield(1, "Mountain", 5)
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        for (p in opponentPermanents) builder = builder.withCardOnBattlefield(2, p)
        return builder.build()
    }

    init {
        test("5 damage to a 2/2 is 3 excess: three tapped Treasures") {
            val game = game("Grizzly Bears")
            game.castSpell(1, "Bottle-Cap Blast", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe false
            val treasures = game.findPermanents("Treasure")
            treasures shouldHaveSize 3
            treasures.all { game.state.getEntity(it)?.get<TappedComponent>() != null } shouldBe true
        }

        test("5 damage to a 6/4 is 1 excess: one Treasure") {
            val game = game("Craw Wurm") // 6/4
            val wurm = game.findPermanent("Craw Wurm")!!
            game.castSpell(1, "Bottle-Cap Blast", wurm).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Craw Wurm") shouldBe false
            game.findPermanents("Treasure") shouldHaveSize 1
        }

        test("targeting a player: damage, but no excess and no Treasure") {
            val game = game()
            game.castSpellTargetingPlayer(1, "Bottle-Cap Blast", 2).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 15
            game.findPermanents("Treasure") shouldHaveSize 0
        }
    }
}
