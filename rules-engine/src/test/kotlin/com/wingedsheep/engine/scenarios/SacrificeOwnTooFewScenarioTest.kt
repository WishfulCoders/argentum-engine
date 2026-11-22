package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "Sacrifice N" (`Effects.SacrificeOwn` with count > 1) when the player controls fewer than N
 * matching permanents: they sacrifice as many as they can (CR 609.3 — an effect that attempts
 * something impossible does only as much as possible; Barter in Blood's ruling, "If a player
 * controls only one creature, that creature is sacrificed"). Before #2706 the executor sacrificed
 * nothing in that case.
 *
 * Inline card, no set dependency.
 */
class SacrificeOwnTooFewScenarioTest : ScenarioTestBase() {

    private val sacrificeTwo = card("Sacrifice Two Creatures") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.SacrificeOwn(GameObjectFilter.Creature, count = 2)
        }
    }

    private fun gameWithBears(bears: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .apply { repeat(bears) { withCardOnBattlefield(1, "Grizzly Bears") } }
        .withCardInHand(1, "Sacrifice Two Creatures")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        cardRegistry.register(sacrificeTwo)

        context("sacrifice two creatures") {

            test("with only one creature, that creature is sacrificed") {
                val game = gameWithBears(1)

                game.castSpell(1, "Sacrifice Two Creatures")
                game.resolveStack()

                withClue("no choice to make — the lone creature goes") {
                    game.hasPendingDecision() shouldBe false
                }
                game.findPermanents("Grizzly Bears").size shouldBe 0
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }

            test("with no creatures, nothing happens") {
                val game = gameWithBears(0)

                game.castSpell(1, "Sacrifice Two Creatures")
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
            }

            test("with more than two creatures, the player chooses exactly two") {
                val game = gameWithBears(3)

                game.castSpell(1, "Sacrifice Two Creatures")
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                decision.minSelections shouldBe 2
                decision.maxSelections shouldBe 2

                game.selectCards(game.findPermanents("Grizzly Bears").take(2))
                game.findPermanents("Grizzly Bears").size shouldBe 1
            }
        }
    }
}
