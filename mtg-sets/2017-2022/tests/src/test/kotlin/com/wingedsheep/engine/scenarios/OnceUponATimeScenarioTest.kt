package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Once Upon a Time (ELD #169): free if it's the first spell you've cast this game; dig five for a
 * creature or land, the rest to the bottom in a random order.
 */
class OnceUponATimeScenarioTest : ScenarioTestBase() {

    init {
        context("Once Upon a Time") {
            test("as the first spell of the game it is free, and digs a creature or land out of the top five") {
                val builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Once Upon a Time")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                listOf("Lightning Bolt", "Grizzly Bears", "Counterspell", "Forest", "Divination", "Hill Giant")
                    .forEach { builder.withCardInLibrary(1, it) }
                builder.withCardInLibrary(2, "Island")
                val game = builder.build()

                val ouat = game.findCardsInHand(1, "Once Upon a Time").single()
                withClue("the free cast is offered") {
                    game.getLegalActions(1).any {
                        (it.action as? CastSpell)?.let { a -> a.cardId == ouat && a.useAlternativeCost } == true
                    } shouldBe true
                }
                game.execute(
                    CastSpell(game.player1Id, ouat, useAlternativeCost = true, alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE)
                ).error shouldBe null
                withClue("no land was tapped") {
                    game.findPermanents("Forest").none { game.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe true
                }
                game.resolveStack()

                val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
                val forest = game.findCardsInLibrary(1, "Forest").single()
                withClue("only the creature and the land among the top five are eligible") {
                    decision.options shouldContainExactlyInAnyOrder listOf(bears, forest)
                }
                game.selectCards(listOf(bears))
                game.resolveStack()
                game.isInHand(1, "Grizzly Bears") shouldBe true
                withClue("the other four went to the bottom, under Hill Giant") {
                    game.state.getLibrary(game.player1Id).first() shouldBe game.findCardsInLibrary(1, "Hill Giant").single()
                    game.librarySize(1) shouldBe 5
                }
            }

            test("after another spell this game, it costs its mana cost") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Once Upon a Time")
                    .withCardInHand(1, "Giant Growth")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.castSpell(1, "Giant Growth", game.findPermanent("Grizzly Bears")).error shouldBe null
                game.resolveStack()

                val ouat = game.findCardsInHand(1, "Once Upon a Time").single()
                withClue("no free cast is offered any more") {
                    game.getLegalActions(1).none {
                        (it.action as? CastSpell)?.let { a -> a.cardId == ouat && a.useAlternativeCost } == true
                    } shouldBe true
                }
                game.execute(
                    CastSpell(game.player1Id, ouat, useAlternativeCost = true, alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE)
                ).error shouldNotBe null
                game.castSpell(1, "Once Upon a Time").error shouldBe null
            }
        }
    }
}
