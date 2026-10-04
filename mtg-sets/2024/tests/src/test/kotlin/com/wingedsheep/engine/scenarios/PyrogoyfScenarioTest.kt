package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m3c.cards.Pyrogoyf
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Pyrogoyf (M3C #59) — {3}{R} Creature — Lhurgoyf, star / 1+star.
 *
 *   Pyrogoyf's power is equal to the number of card types among cards in all graveyards and its
 *   toughness is equal to that number plus 1.
 *   Whenever this creature or another Lhurgoyf creature you control enters, that creature deals
 *   damage equal to its power to any target.
 *
 * Pins the two halves of the enter trigger: Pyrogoyf itself (its power read from the graveyards),
 * and another Lhurgoyf, which is the source of its own damage at its own power. A non-Lhurgoyf
 * entering does nothing.
 */
class PyrogoyfScenarioTest : ScenarioTestBase() {

    private val instant = card("Test Goyf Instant") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell { effect = Effects.GainLife(1) }
    }

    private val sorcery = card("Test Goyf Sorcery") {
        manaCost = "{R}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(1) }
    }

    private val otherGoyf = card("Test Other Lhurgoyf") {
        manaCost = "{1}"
        typeLine = "Creature — Lhurgoyf"
        power = 3
        toughness = 3
    }

    private val bear = card("Test Plain Bear") {
        manaCost = "{1}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    init {
        cardRegistry.register(Pyrogoyf)
        cardRegistry.register(instant)
        cardRegistry.register(sorcery)
        cardRegistry.register(otherGoyf)
        cardRegistry.register(bear)

        test("Pyrogoyf entering deals damage equal to its graveyard-defined power") {
            val game = scenario()
                .withPlayers("Goyf", "Defender")
                .withCardInHand(1, "Pyrogoyf")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardInGraveyard(1, "Test Goyf Instant")
                .withCardInGraveyard(2, "Test Goyf Sorcery")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Pyrogoyf").error shouldBe null
            game.resolveStack()

            val goyf = game.findPermanent("Pyrogoyf")!!
            withClue("instant + sorcery across both graveyards: 2/3") {
                game.state.projectedState.getPower(goyf) shouldBe 2
                game.state.projectedState.getToughness(goyf) shouldBe 3
            }

            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 18
        }

        test("another Lhurgoyf entering deals damage equal to its own power; a non-Lhurgoyf does not trigger") {
            val game = scenario()
                .withPlayers("Goyf", "Defender")
                .withCardOnBattlefield(1, "Pyrogoyf")
                .withCardInHand(1, "Test Other Lhurgoyf")
                .withCardInHand(1, "Test Plain Bear")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Test Plain Bear").error shouldBe null
            game.resolveStack()
            withClue("a Bear is not a Lhurgoyf, so nothing triggers") {
                game.hasPendingDecision() shouldBe false
                game.getLifeTotal(2) shouldBe 20
            }

            game.castSpell(1, "Test Other Lhurgoyf").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            withClue("the 3/3 Lhurgoyf deals 3, not Pyrogoyf's 0") {
                game.getLifeTotal(2) shouldBe 17
            }
        }
    }
}
