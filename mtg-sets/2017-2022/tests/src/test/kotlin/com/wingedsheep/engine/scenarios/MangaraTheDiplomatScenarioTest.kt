package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Mangara, the Diplomat (M21 #27) — {3}{W} Legendary Creature — Human Cleric, 2/4.
 *
 * "Lifelink
 *  Whenever an opponent attacks with creatures, if two or more of those creatures are attacking
 *  you and/or planeswalkers you control, draw a card.
 *  Whenever an opponent casts their second spell each turn, draw a card."
 *
 * Two independent, non-life-loss triggers (Tomik, Wielder of Law's attack shape and Howling Moon's
 * second-spell shape, both trimmed to a single draw). NOTE: Lifelink itself is a standard keyword
 * exercised widely elsewhere in this corpus and isn't re-tested here; these tests focus on the two
 * novel triggers, including the intervening "if" re-check the attack trigger shares with Tomik.
 */
class MangaraTheDiplomatScenarioTest : ScenarioTestBase() {

    private val freeCantrip = card("Test Free Cantrip (Mangara)") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { effect = Effects.GainLife(1) } // a free, harmless spell to cast repeatedly
    }

    init {
        cardRegistry.register(freeCantrip)

        context("Mangara, the Diplomat") {

            test("two attackers at you draws a card") {
                val game = scenario()
                    .withPlayers("Mangara", "Attacker")
                    .withCardOnBattlefield(1, "Mangara, the Diplomat", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Centaur Courser", summoningSickness = false)
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val handBefore = game.handSize(1)

                game.declareAttackers(mapOf("Grizzly Bears" to 1, "Centaur Courser" to 1))
                    .error shouldBe null
                game.resolveStack()

                withClue("two of the opponent's attackers targeting you draws a card") {
                    game.handSize(1) shouldBe handBefore + 1
                }
            }

            test("a single attacker does not trigger the draw") {
                val game = scenario()
                    .withPlayers("Mangara", "Attacker")
                    .withCardOnBattlefield(1, "Mangara, the Diplomat", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val handBefore = game.handSize(1)

                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.resolveStack()

                withClue("one attacker is not 'two or more of those creatures'") {
                    game.handSize(1) shouldBe handBefore
                }
            }

            test("an opponent's second spell each turn draws a card, but your own second spell does not") {
                val game = scenario()
                    .withPlayers("Mangara", "Opponent")
                    .withCardOnBattlefield(1, "Mangara, the Diplomat", summoningSickness = false)
                    .withCardsInHand(2, "Test Free Cantrip (Mangara)", 2)
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)

                // Opponent's first spell this turn: no trigger.
                game.castSpell(2, "Test Free Cantrip (Mangara)").error shouldBe null
                game.resolveStack()
                withClue("no draw off the opponent's first spell") {
                    game.handSize(1) shouldBe handBefore
                }

                // Opponent's second spell this turn: triggers the draw.
                game.castSpell(2, "Test Free Cantrip (Mangara)").error shouldBe null
                game.resolveStack()
                withClue("the opponent's second spell of the turn draws you a card") {
                    game.handSize(1) shouldBe handBefore + 1
                }
            }
        }
    }
}
