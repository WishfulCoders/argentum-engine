package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Anchor to Reality (NEO #45, {2}{U}{U}, Sorcery).
 *
 *   As an additional cost to cast this spell, sacrifice an artifact or creature.
 *   Search your library for an Equipment or Vehicle card, put that card onto the battlefield, then
 *   shuffle. If it has mana value less than the sacrificed permanent's mana value, scry 2.
 *
 * Pins the cost-linked comparison: the found card's mana value against the sacrificed permanent's
 * last-known mana value, and no scry when the search finds nothing.
 */
class AnchorToRealityScenarioTest : ScenarioTestBase() {

    init {
        context("Anchor to Reality") {

            test("finding a cheaper Equipment than the sacrificed creature scries 2") {
                // Grizzly Bears (MV 2) sacrificed; Bonesplitter (MV 1) found → 1 < 2, scry 2.
                val game = board(sacrifice = "Grizzly Bears")
                cast(game, "Grizzly Bears")

                val search = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                withClue("only the Equipment is searchable, not the creature or lands") {
                    search.options.map { game.cardName(it) } shouldBe listOf("Bonesplitter")
                }
                game.selectCards(search.options)

                game.isOnBattlefield("Bonesplitter").shouldBeTrue()
                val scry = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                withClue("scry 2 looks at the top two cards") {
                    scry.options shouldHaveSize 2
                }
            }

            test("an Equipment that isn't cheaper than the sacrificed permanent doesn't scry") {
                // Memnite (MV 0) sacrificed; Bonesplitter (MV 1) found → 1 < 0 is false.
                val game = board(sacrifice = "Memnite")
                cast(game, "Memnite")

                val search = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(search.options)

                game.isOnBattlefield("Bonesplitter").shouldBeTrue()
                game.hasPendingDecision().shouldBeFalse()
                game.isInGraveyard(1, "Anchor to Reality").shouldBeTrue()
            }

            test("failing to find a card means there is nothing to compare, so no scry") {
                val game = board(sacrifice = "Grizzly Bears")
                cast(game, "Grizzly Bears")

                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(emptyList())

                game.isOnBattlefield("Bonesplitter").shouldBeFalse()
                game.hasPendingDecision().shouldBeFalse()
                game.isInGraveyard(1, "Anchor to Reality").shouldBeTrue()
            }
        }
    }

    private fun board(sacrifice: String): TestGame =
        scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Anchor to Reality")
            .withCardOnBattlefield(1, sacrifice)
            .withCardInLibrary(1, "Bonesplitter")
            .withCardInLibrary(1, "Grizzly Bears")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withLandsOnBattlefield(1, "Island", 4)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

    private fun cast(game: TestGame, sacrifice: String) {
        val victim = game.findPermanent(sacrifice)!!
        val spell = game.state.getHand(game.player1Id).first { game.cardName(it) == "Anchor to Reality" }
        val result = game.execute(
            CastSpell(
                game.player1Id,
                spell,
                emptyList(),
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(victim))
            )
        )
        withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
        game.isInGraveyard(1, sacrifice).shouldBeTrue()
        game.resolveStack()
    }

    private fun TestGame.cardName(id: EntityId): String? =
        state.getEntity(id)?.get<CardComponent>()?.name
}
