package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Pollywog Prodigy — {1}{U} Creature — Frog Wizard 1/3.
 *   Evolve
 *   Whenever an opponent casts a noncreature spell with mana value less than this creature's
 *   power, draw a card.
 *
 * Pins the Evolve keyword (CR 702.100a) — a creature with greater power *or* greater toughness
 * evolves it, one with neither doesn't — and the power-relative mana-value cap on the cast trigger.
 */
class PollywogProdigyScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Evolve") {
            test("each creature with greater power evolves it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Pollywog Prodigy")
                    .withCardsInHand(1, "Centaur Courser", 2) // 3/3
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val prodigy = game.findPermanent("Pollywog Prodigy")!!

                game.castSpell(1, "Centaur Courser").error shouldBe null
                game.resolveStack()
                withClue("3/3 beats the 1/3 on power — one +1/+1 counter") {
                    game.plusOneCounters(prodigy) shouldBe 1
                }

                // Now a 2/4: a 3/3 is greater on power, so it evolves again (to 3/5).
                game.castSpell(1, "Centaur Courser").error shouldBe null
                game.resolveStack()
                withClue("second 3/3 is still greater on power than the 2/4") {
                    game.plusOneCounters(prodigy) shouldBe 2
                }
            }

            test("greater toughness alone evolves it; a creature greater on neither stat does not") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Pollywog Prodigy")
                    .withCardInHand(1, "Wall of Shields") // 0/4: lesser power, greater toughness
                    .withCardInHand(1, "Pollywog Prodigy") // 1/3
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val first = game.findPermanent("Pollywog Prodigy")!!

                game.castSpell(1, "Wall of Shields").error shouldBe null
                game.resolveStack()
                withClue("toughness 4 beats toughness 3 — one +1/+1 counter") {
                    game.plusOneCounters(first) shouldBe 1
                }

                game.castSpell(1, "Pollywog Prodigy").error shouldBe null
                game.resolveStack()
                withClue("a 1/3 is greater on neither stat than the 2/4 — no second counter") {
                    game.plusOneCounters(first) shouldBe 1
                }
            }
        }

        context("opponent casts a cheap noncreature spell") {
            fun game(prodigyCounters: Int): TestGame {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Pollywog Prodigy")
                    .withCardInLibrary(1, "Island")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                if (prodigyCounters > 0) {
                    val prodigy = game.findPermanent("Pollywog Prodigy")!!
                    game.state = game.state.updateEntity(prodigy) {
                        it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to prodigyCounters)))
                    }
                }
                return game
            }

            test("mana value equal to its power does not draw") {
                val game = game(prodigyCounters = 0) // power 1, Bolt is MV 1
                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()
                withClue("MV 1 is not less than power 1") { game.handSize(1) shouldBe 0 }
            }

            test("mana value less than its power draws a card") {
                val game = game(prodigyCounters = 1) // power 2
                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()
                withClue("MV 1 is less than power 2") { game.handSize(1) shouldBe 1 }
            }
        }
    }
}
