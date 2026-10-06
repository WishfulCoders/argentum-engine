package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Time Spiral {4}{U}{U} — Sorcery.
 *
 * "Exile Time Spiral. Each player shuffles their hand and graveyard into their library, then draws
 *  seven cards. You untap up to six lands."
 *
 * Proves the three parts together: the wheel on both sides, Time Spiral ending in exile rather
 * than the graveyard or the library, and the caster untapping the six lands that paid for it.
 */
class TimeSpiralScenarioTest : ScenarioTestBase() {

    init {
        test("wheels both players, exiles itself, and untaps up to six lands") {
            val builder = scenario()
                .withPlayers()
                .withCardInHand(1, "Time Spiral")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Island", 7)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(10) { builder.withCardInLibrary(1, "Forest"); builder.withCardInLibrary(2, "Forest") }
            val game = builder.build()

            fun tapped(id: EntityId) = game.state.getEntity(id)?.has<TappedComponent>() == true

            game.castSpell(1, "Time Spiral").error shouldBe null
            val islands = game.findAllPermanents("Island")
            val paid = islands.filter { tapped(it) }
            paid.size shouldBe 6
            game.resolveStack()

            game.handSize(1) shouldBe 7
            game.handSize(2) shouldBe 7
            game.graveyardSize(1) shouldBe 0
            game.graveyardSize(2) shouldBe 0
            // 10 Forests + Bolt + Bears = 12 minus seven drawn; Time Spiral is not in the library.
            game.librarySize(1) shouldBe 5

            game.selectCards(paid).error shouldBe null
            withClue("all six lands that paid for it are untapped again") {
                islands.none { tapped(it) } shouldBe true
            }
            withClue("Time Spiral finished resolving and exiled itself") {
                game.isInExile(1, "Time Spiral") shouldBe true
                game.isInGraveyard(1, "Time Spiral") shouldBe false
            }
        }
    }
}
