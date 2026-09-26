package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for the Tarkir: Dragonstorm "…storm" enchantment cycle: Breaching, Roiling,
 * Corroding, Encroaching, and Teeming Dragonstorm. Every card shares the cycle's second ability —
 * "When a Dragon you control enters, return this enchantment to its owner's hand" — exercised here
 * with Breaching as the representative, plus each card's distinct enters-the-battlefield ability.
 *
 * Breaching also exercises the `currentlyIn(zone)` filter (`StatePredicate.InZone`): after the optional
 * free-cast of the exiled nonland, only the copy *still in exile* (declined / mana value > 8) is
 * put into hand — the one just cast has moved to the stack and must not be bounced.
 */
class DragonstormCycleScenarioTest : ScenarioTestBase() {

    init {
        // A cheap nonland the impulse may free-cast (mana value 2 ≤ 8).
        cardRegistry.register(
            CardDefinition.creature(
                name = "Cheap Ogre",
                manaCost = ManaCost.parse("{1}{R}"),
                subtypes = setOf(Subtype("Ogre")),
                power = 2,
                toughness = 2
            )
        )
        // A nonland too expensive to free-cast (mana value 9 > 8).
        cardRegistry.register(
            CardDefinition.creature(
                name = "Colossal Wurm",
                manaCost = ManaCost.parse("{7}{G}{G}"),
                subtypes = setOf(Subtype("Wurm")),
                power = 9,
                toughness = 9
            )
        )
        // A Dragon to trigger the cycle's "return to hand" bounce.
        cardRegistry.register(
            CardDefinition.creature(
                name = "Bounce Dragon",
                manaCost = ManaCost.parse("{4}{R}"),
                subtypes = setOf(Subtype.DRAGON),
                power = 4,
                toughness = 4
            )
        )

        context("Breaching Dragonstorm — impulse to free-cast or hand") {

            test("mana value ≤ 8: declining the free-cast puts the nonland into hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Breaching Dragonstorm")
                    .withCardInLibrary(1, "Cheap Ogre")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Breaching Dragonstorm").error shouldBe null
                game.resolveStack()
                // ETB exiles until the nonland (Cheap Ogre), then asks "you may cast it".
                game.answerYesNo(false)

                withClue("Declined cast → Cheap Ogre is put into hand") {
                    namesInHand(game, 1).contains("Cheap Ogre") shouldBe true
                }
                withClue("It is no longer in exile") {
                    namesInExile(game, 1).contains("Cheap Ogre") shouldBe false
                }
            }

            test("mana value ≤ 8: accepting the free-cast casts it (and it does NOT go to hand)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Breaching Dragonstorm")
                    .withCardInLibrary(1, "Cheap Ogre")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Breaching Dragonstorm").error shouldBe null
                game.resolveStack()
                game.answerYesNo(true)
                // Cheap Ogre is a creature spell with no targets — resolve it.
                game.resolveStack()

                withClue("Accepted free-cast → Cheap Ogre is on the battlefield") {
                    game.isOnBattlefield("Cheap Ogre") shouldBe true
                }
                withClue("The cast card is NOT also put into hand (InZone(EXILE) filtered it out)") {
                    namesInHand(game, 1).contains("Cheap Ogre") shouldBe false
                }
            }

            test("mana value > 8: no free-cast option; the nonland goes straight to hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Breaching Dragonstorm")
                    .withCardInLibrary(1, "Colossal Wurm")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Breaching Dragonstorm").error shouldBe null
                game.resolveStack()

                withClue("MV 9 > 8 is never castable, so Colossal Wurm is put into hand") {
                    namesInHand(game, 1).contains("Colossal Wurm") shouldBe true
                }
                withClue("It is not on the battlefield (never cast)") {
                    game.isOnBattlefield("Colossal Wurm") shouldBe false
                }
            }
        }

        context("Dragonstorm cycle — Dragon-enters bounce (Breaching)") {

            test("a Dragon you control entering returns the enchantment to hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Breaching Dragonstorm")
                    .withCardInHand(1, "Bounce Dragon")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Bounce Dragon").error shouldBe null
                game.resolveStack()

                withClue("Bounce Dragon resolved onto the battlefield") {
                    game.isOnBattlefield("Bounce Dragon") shouldBe true
                }
                withClue("Its entry returned Breaching Dragonstorm to its owner's hand") {
                    namesInHand(game, 1).contains("Breaching Dragonstorm") shouldBe true
                }
                withClue("Breaching Dragonstorm is no longer on the battlefield") {
                    game.isOnBattlefield("Breaching Dragonstorm") shouldBe false
                }
            }
        }

        context("Roiling Dragonstorm — draw two, discard one") {

            test("draws two then discards one on enter (net +1 card)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Roiling Dragonstorm")
                    .withCardInLibrary(1, "Cheap Ogre")
                    .withCardInLibrary(1, "Colossal Wurm")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = namesInHand(game, 1).size // = 1 (Roiling itself)
                game.castSpell(1, "Roiling Dragonstorm").error shouldBe null
                game.resolveStack()
                // Discard prompt: discard one of the two drawn cards.
                game.selectCards(listOf(game.state.getHand(game.player1Id).first()))

                withClue("Cast Roiling (-1 hand) then drew 2 and discarded 1 → net +0 vs start") {
                    namesInHand(game, 1).size shouldBe handBefore + 0
                }
                withClue("Exactly one card was discarded to the graveyard") {
                    game.state.getGraveyard(game.player1Id).size shouldBe 1
                }
            }
        }

        context("Corroding Dragonstorm — drain 2 and surveil 2") {

            test("each opponent loses 2 and you gain 2 on enter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Corroding Dragonstorm")
                    .withCardInLibrary(1, "Cheap Ogre")
                    .withCardInLibrary(1, "Colossal Wurm")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Corroding Dragonstorm").error shouldBe null
                game.resolveStack()
                // Surveil 2 may prompt to choose cards to keep/bin; the base auto-resolver handles it.
                game.resolveStack()

                withClue("Opponent loses 2 life") {
                    game.getLifeTotal(2) shouldBe 18
                }
                withClue("You gain 2 life") {
                    game.getLifeTotal(1) shouldBe 22
                }
            }
        }

        context("Encroaching Dragonstorm — ramp two basics tapped") {

            test("searches up to two basic lands onto the battlefield tapped") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Encroaching Dragonstorm")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val forestsBefore = game.findPermanents("Forest").size
                game.castSpell(1, "Encroaching Dragonstorm").error shouldBe null
                game.resolveStack()
                // Search prompt: pick both Forests from the library.
                val libForests = game.state.getLibrary(game.player1Id)
                    .filter { game.state.getEntity(it)?.get<CardComponent>()?.name == "Forest" }
                game.selectCards(libForests)
                game.resolveStack()

                withClue("Two additional Forests entered the battlefield") {
                    game.findPermanents("Forest").size shouldBe forestsBefore + 2
                }
            }
        }

        context("Teeming Dragonstorm — make two 2/2 Soldiers") {

            test("creates two 2/2 white Soldier tokens on enter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Teeming Dragonstorm")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Teeming Dragonstorm").error shouldBe null
                game.resolveStack()

                withClue("Two Soldier tokens were created") {
                    game.findPermanents("Soldier Token").size + game.findPermanents("Soldier").size shouldBe 2
                }
            }
        }
    }

    private fun namesInHand(game: TestGame, playerNumber: Int): List<String> {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getHand(playerId).mapNotNull {
            game.state.getEntity(it)?.get<CardComponent>()?.name
        }
    }

    private fun namesInExile(game: TestGame, playerNumber: Int): List<String> {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getExile(playerId).mapNotNull {
            game.state.getEntity(it)?.get<CardComponent>()?.name
        }
    }
}
