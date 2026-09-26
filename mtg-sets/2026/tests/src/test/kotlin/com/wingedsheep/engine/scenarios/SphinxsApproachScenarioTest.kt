package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Sphinx's Approach {1}{U}{U} — Instant:
 * "Draw two cards. Then you may exile this spell and four cards named Sphinx's Approach from your
 *  graveyard. If you do, search your library for a Sphinx creature card, put it onto the
 *  battlefield, then shuffle."
 *
 * Pins: the full payoff (the resolving spell and four graveyard copies all exiled, a Sphinx
 * tutored onto the battlefield); declining (spell to the graveyard, no search); and fewer than
 * four other copies making the option unavailable (no prompt at all, nothing exiled).
 */
class SphinxsApproachScenarioTest : ScenarioTestBase() {

    private fun ScenarioBuilder.withApproachesInGraveyard(count: Int): ScenarioBuilder {
        repeat(count) { withCardInGraveyard(1, "Sphinx's Approach") }
        return this
    }

    private fun ScenarioBuilder.withLibrary(): ScenarioBuilder =
        withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Cerulean Sphinx")

    init {
        context("with four other copies in the graveyard") {
            test("exiling the spell and four copies tutors a Sphinx onto the battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sphinx's Approach")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withApproachesInGraveyard(4)
                    .withLibrary()
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                game.castSpell(1, "Sphinx's Approach").error shouldBe null
                game.resolveStack()

                withClue("Draws two before the optional exile") {
                    game.handSize(1) shouldBe handBefore - 1 + 2
                }
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true)

                // Exactly four copies: the graveyard pick is forced, so the next decision is the search.
                val search = game.getPendingDecision()
                search.shouldBeInstanceOf<SelectCardsDecision>()
                val sphinx = search.cardInfo!!.entries.single { it.value.name == "Cerulean Sphinx" }.key
                game.selectCards(listOf(sphinx))

                withClue("Cerulean Sphinx was put onto the battlefield") {
                    game.isOnBattlefield("Cerulean Sphinx") shouldBe true
                }
                withClue("The resolving spell and the four graveyard copies are all exiled") {
                    game.state.getExile(game.player1Id)
                        .count { game.state.getEntity(it)?.get<CardComponent>()?.name == "Sphinx's Approach" } shouldBe 5
                    game.isInGraveyard(1, "Sphinx's Approach") shouldBe false
                }
            }

            test("declining leaves every copy in the graveyard and searches nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sphinx's Approach")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withApproachesInGraveyard(4)
                    .withLibrary()
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Sphinx's Approach").error shouldBe null
                game.resolveStack()
                game.answerYesNo(false)

                withClue("No search happened") {
                    game.hasPendingDecision() shouldBe false
                    game.isOnBattlefield("Cerulean Sphinx") shouldBe false
                }
                withClue("The spell went to the graveyard as usual, joining the other four") {
                    game.graveyardSize(1) shouldBe 5
                    game.isInExile(1, "Sphinx's Approach") shouldBe false
                }
            }
        }

        context("with only three other copies in the graveyard") {
            test("the option isn't offered and nothing is exiled") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sphinx's Approach")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withApproachesInGraveyard(3)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLibrary()
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                game.castSpell(1, "Sphinx's Approach").error shouldBe null
                game.resolveStack()

                withClue("No yes/no — four copies can't be exiled, so the option isn't a legal choice") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("Still drew two") {
                    game.handSize(1) shouldBe handBefore - 1 + 2
                }
                withClue("Nothing exiled; the spell joins the graveyard") {
                    game.isInExile(1, "Sphinx's Approach") shouldBe false
                    game.graveyardSize(1) shouldBe 5
                    game.isOnBattlefield("Cerulean Sphinx") shouldBe false
                }
            }
        }
    }
}
