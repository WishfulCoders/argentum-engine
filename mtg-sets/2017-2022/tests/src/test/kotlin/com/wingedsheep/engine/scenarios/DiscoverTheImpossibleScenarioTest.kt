package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Discover the Impossible (NEO #50) — {2}{U} Instant.
 *
 *   Look at the top five cards of your library. Exile one of them face down and put the rest on
 *   the bottom of your library in a random order. You may cast the exiled card without paying its
 *   mana cost if it's an instant spell with mana value 2 or less. If you don't, put that card into
 *   your hand.
 *
 * Covers the free cast of a qualifying instant out of face-down exile, a cheap *sorcery* not being
 * castable (it goes to hand instead), and declining the cast.
 */
class DiscoverTheImpossibleScenarioTest : ScenarioTestBase() {

    /** Casts Discover the Impossible with [exiled] on top and four Islands under it. */
    private fun castDiscover(exiled: String): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Discover the Impossible")
            .withLandsOnBattlefield(1, "Island", 3)
            .withCardInLibrary(1, exiled)
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castSpell(1, "Discover the Impossible").error shouldBe null
        game.resolveStack()

        val pick = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
        val card = game.findCardsInLibrary(1, exiled).single()
        pick.options.size shouldBe 5
        game.selectCards(listOf(card)).error shouldBe null
        return game
    }

    init {
        context("Discover the Impossible") {

            test("an instant with mana value 2 or less is cast from exile without paying its mana cost") {
                val game = castDiscover("Think Twice")
                val thinkTwice = game.state.getExile(game.player1Id).single()
                val choice = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                choice.options shouldBe listOf(thinkTwice)
                game.selectCards(listOf(thinkTwice)).error shouldBe null
                game.resolveStack()

                withClue("Think Twice resolved (drawing a card) instead of going to hand") {
                    game.isInGraveyard(1, "Think Twice") shouldBe true
                    game.isInExile(1, "Think Twice") shouldBe false
                    game.isInHand(1, "Think Twice") shouldBe false
                    game.handSize(1) shouldBe 1
                }
                withClue("the other four went to the bottom; Think Twice then drew one") {
                    game.librarySize(1) shouldBe 4
                }
            }

            test("a sorcery with mana value 2 or less can't be cast and goes to hand") {
                val game = castDiscover("Ponder")
                if (game.getPendingDecision() is SelectCardsDecision) {
                    val ponder = game.state.getExile(game.player1Id).single()
                    withClue("Ponder is a sorcery, so it isn't selectable") {
                        game.selectCards(listOf(ponder)).error shouldNotBe null
                    }
                    game.selectCards(emptyList()).error shouldBe null
                }
                game.resolveStack()

                game.isInHand(1, "Ponder") shouldBe true
                game.isInExile(1, "Ponder") shouldBe false
                game.librarySize(1) shouldBe 5
            }

            test("declining the free cast puts the exiled card into hand") {
                val game = castDiscover("Think Twice")
                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Think Twice") shouldBe true
                game.isInExile(1, "Think Twice") shouldBe false
                game.isInGraveyard(1, "Think Twice") shouldBe false
                game.librarySize(1) shouldBe 5
            }
        }
    }
}
