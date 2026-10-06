package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Frantic Search {2}{U} — Instant.
 *
 * "Draw two cards, then discard two cards. Untap up to three lands."
 *
 * The untap is untargeted and may pick any lands, including an opponent's (2022-12-08 ruling), so
 * the test pays with three Islands, then untaps two of them and an opponent's tapped Mountain —
 * the "free spell" line plus the any-controller reading.
 */
class FranticSearchScenarioTest : ScenarioTestBase() {

    init {
        test("draws two, discards two, then untaps up to three lands of any controller") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Frantic Search")
                .withCardInHand(1, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardOnBattlefield(2, "Mountain", tapped = true)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun tapped(id: EntityId) = game.state.getEntity(id)?.has<TappedComponent>() == true

            game.castSpell(1, "Frantic Search").error shouldBe null
            val islands = game.findAllPermanents("Island")
            val mountain = game.findPermanent("Mountain")!!
            islands.all { tapped(it) } shouldBe true
            game.resolveStack()

            // Discard two: the two original non-Search cards.
            val discards = game.findCardsInHand(1, "Grizzly Bears") + game.findCardsInHand(1, "Lightning Bolt")
            game.selectCards(discards).error shouldBe null

            // Untap up to three lands: two Islands and the opponent's Mountain.
            game.selectCards(listOf(islands[0], islands[1], mountain)).error shouldBe null

            withClue("drew two Forests and discarded the chosen two cards") {
                game.findCardsInHand(1, "Forest").size shouldBe 2
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Lightning Bolt") shouldBe true
                game.isInGraveyard(1, "Frantic Search") shouldBe true
            }
            withClue("the chosen lands untapped, including the opponent's; the third Island stays tapped") {
                tapped(islands[0]) shouldBe false
                tapped(islands[1]) shouldBe false
                tapped(mountain) shouldBe false
                tapped(islands[2]) shouldBe true
            }
        }
    }
}
