package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Jodah, the Unifier (DMU #203) — {W}{U}{B}{R}{G} Legendary Creature — Human Wizard, 5/5.
 *
 * "Legendary creatures you control get +X/+X, where X is the number of legendary creatures you
 *  control.
 *  Whenever you cast a legendary spell from your hand, exile cards from the top of your library
 *  until you exile a legendary nonland card with lesser mana value. You may cast that card without
 *  paying its mana cost. Put the rest on the bottom of your library in a random order."
 *
 * The trigger is a hand-spelled "legendary cascade", so the walk is pinned card by card: it passes a
 * cheaper nonlegendary card, a legendary land, a legendary card of *greater* mana value and one of
 * *equal* mana value (lesser is strict) before
 * stopping on the cheaper legend. Everything else revealed goes to the bottom; a declined card stays
 * in exile (ruling).
 */
class JodahTheUnifierScenarioTest : ScenarioTestBase() {

    private val legendThree = card("Jodah Test Legend Three") {
        manaCost = "{2}{G}"
        typeLine = "Legendary Creature — Elf"
        power = 2
        toughness = 2
    }
    /** Same mana value as the triggering spell: "lesser" is strict, so the walk must pass it. */
    private val legendThreeB = card("Jodah Test Legend Three B") {
        manaCost = "{2}{G}"
        typeLine = "Legendary Creature — Elf"
        power = 2
        toughness = 2
    }
    private val legendTwo = card("Jodah Test Legend Two") {
        manaCost = "{1}{G}"
        typeLine = "Legendary Creature — Elf"
        power = 2
        toughness = 2
    }
    private val legendFour = card("Jodah Test Legend Four") {
        manaCost = "{3}{G}"
        typeLine = "Legendary Creature — Elf"
        power = 3
        toughness = 3
    }
    private val bear = card("Jodah Test Bear") {
        manaCost = "{G}"
        typeLine = "Creature — Bear"
        power = 1
        toughness = 1
    }
    private val legendaryLand = card("Jodah Test Legendary Land") {
        typeLine = "Legendary Land"
    }

    init {
        listOf(legendThree, legendThreeB, legendTwo, legendFour, bear, legendaryLand).forEach { cardRegistry.register(it) }

        fun board(castFromHand: String = "Jodah Test Legend Three") = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Jodah, the Unifier")
            .withCardInHand(1, castFromHand)
            .withLandsOnBattlefield(1, "Forest", 3)
            // Top → bottom: four cards the walk must pass, the hit, then two the walk never reaches.
            .withCardInLibrary(1, "Jodah Test Bear")
            .withCardInLibrary(1, "Jodah Test Legendary Land")
            .withCardInLibrary(1, "Jodah Test Legend Four")
            .withCardInLibrary(1, "Jodah Test Legend Three B")
            .withCardInLibrary(1, "Jodah Test Legend Two")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        context("the anthem") {

            test("legendary creatures you control get +X/+X for each legendary creature you control") {
                val game = board()
                val jodah = game.findPermanent("Jodah, the Unifier")!!
                withClue("alone, Jodah counts himself") {
                    game.state.projectedState.getPower(jodah) shouldBe 6
                    game.state.projectedState.getToughness(jodah) shouldBe 6
                }

                game.castSpell(1, "Jodah Test Legend Three").error shouldBe null
                game.resolveStack()
                if (game.hasPendingDecision()) game.answerYesNo(false)
                game.resolveStack()

                val legend = game.findPermanent("Jodah Test Legend Three")!!
                withClue("two legends: each gets +2/+2") {
                    game.state.projectedState.getPower(jodah) shouldBe 7
                    game.state.projectedState.getPower(legend) shouldBe 4
                }
            }
        }

        context("the cast trigger") {

            test("exiles to the first cheaper legendary nonland card, casts it free, and bottoms the rest") {
                val game = board()
                game.castSpell(1, "Jodah Test Legend Three").error shouldBe null
                game.resolveStack()

                withClue("the walk stopped on Legend Two and offers the free cast") {
                    game.isInExile(1, "Jodah Test Legend Two") shouldBe true
                }
                game.answerYesNo(true)
                game.resolveStack()

                withClue("both the free spell and the original resolved") {
                    game.isOnBattlefield("Jodah Test Legend Two") shouldBe true
                    game.isOnBattlefield("Jodah Test Legend Three") shouldBe true
                }
                val library = game.state.getLibrary(game.player1Id).map { game.state.getEntity(it)!!.get<CardComponent>()!!.name }
                withClue("the unrevealed cards stay on top; the passed-over four go to the bottom") {
                    library.take(2) shouldBe listOf("Island", "Island")
                    library.drop(2) shouldContainExactlyInAnyOrder listOf(
                        "Jodah Test Bear", "Jodah Test Legendary Land", "Jodah Test Legend Four",
                        "Jodah Test Legend Three B",
                    )
                }
                game.state.getExile(game.player1Id).size shouldBe 0
            }

            test("declining leaves the found card in exile") {
                val game = board()
                game.castSpell(1, "Jodah Test Legend Three").error shouldBe null
                game.resolveStack()
                game.answerYesNo(false)
                game.resolveStack()

                game.isInExile(1, "Jodah Test Legend Two") shouldBe true
                game.isOnBattlefield("Jodah Test Legend Two") shouldBe false
                game.librarySize(1) shouldBe 6
            }

            test("a nonlegendary spell from hand does not trigger") {
                val game = board(castFromHand = "Jodah Test Bear")
                game.castSpell(1, "Jodah Test Bear").error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.librarySize(1) shouldBe 7
                game.state.getExile(game.player1Id).size shouldBe 0
            }
        }
    }
}
