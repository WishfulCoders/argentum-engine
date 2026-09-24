package com.wingedsheep.ai.engine.hidden

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.model.GameRng
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * [OpponentModel.DecklistMixture]: the opponent's hidden cards come from the prior, never from their
 * real deck (mtg-draft-ai `docs/51_ptcg_style_selfplay` §9).
 */
class DecklistMixtureTest : ScenarioTestBase() {

    init {
        test("hidden cards are drawn from one list of the mixture, and seen cards stay what they are") {
            val game = scenario().withPlayers()
                .withCardInHand(2, "Mountain").withCardInHand(2, "Hill Giant")
                .withCardInLibrary(2, "Grizzly Bears").withCardInLibrary(2, "Craw Wurm")
                .withCardOnBattlefield(2, "Forest")
                .build()
            val hidden = game.state.getHand(game.player2Id) + game.state.getLibrary(game.player2Id)
            val model = OpponentModel.DecklistMixture(
                listOf(mapOf("Forest" to 1, "Llanowar Elves" to 4), mapOf("Forest" to 1, "Giant Growth" to 4)),
            )
            val sampler = Determinizer(cardRegistry)
            val seenLists = mutableSetOf<String>()
            for (seed in 1L..32L) {
                val world = sampler.sample(game.state, game.player1Id, model, GameRng.seeded(seed))
                val names = hidden.map { world.getEntity(it)!!.get<CardComponent>()!!.name }
                names.distinct().size shouldBe 1
                seenLists += names.first()
                world.getBattlefield().map { world.getEntity(it)!!.get<CardComponent>()!!.name } shouldBe listOf("Forest")
                world.zones.forEach { (key, ids) -> ids.shouldContainExactlyInAnyOrder(game.state.zones.getValue(key)) }
            }
            seenLists shouldBe setOf("Llanowar Elves", "Giant Growth")
        }

        test("a list shorter than the hidden cards is topped up from itself, never left at the truth") {
            val game = scenario().withPlayers()
                .withCardInHand(2, "Mountain").withCardInHand(2, "Hill Giant")
                .withCardInLibrary(2, "Grizzly Bears").withCardInLibrary(2, "Craw Wurm")
                .build()
            val hidden = game.state.getHand(game.player2Id) + game.state.getLibrary(game.player2Id)
            val model = OpponentModel.DecklistMixture(listOf(mapOf("Llanowar Elves" to 1)))
            for (seed in 1L..8L) {
                val world = Determinizer(cardRegistry).sample(game.state, game.player1Id, model, GameRng.seeded(seed))
                hidden.map { world.getEntity(it)!!.get<CardComponent>()!!.name } shouldBe List(4) { "Llanowar Elves" }
            }
        }

        test("the viewer's own model is untouched by the opponent's mixture") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Forest").withCardInLibrary(1, "Hill Giant")
                .withCardInHand(2, "Mountain").withCardInLibrary(2, "Craw Wurm")
                .build()
            val models = mapOf(
                game.player1Id to OpponentModel.KnownDecklist(mapOf("Forest" to 1, "Hill Giant" to 1)),
                game.player2Id to OpponentModel.DecklistMixture(listOf(mapOf("Llanowar Elves" to 2))),
            )
            val world = Determinizer(cardRegistry).sampleForSearch(game.state, game.player1Id, models)
            world.getLibrary(game.player1Id).map { world.getEntity(it)!!.get<CardComponent>()!!.name } shouldBe
                listOf("Hill Giant")
            world.getHand(game.player2Id).map { world.getEntity(it)!!.get<CardComponent>()!!.name } shouldBe
                listOf("Llanowar Elves")
        }
    }
}
