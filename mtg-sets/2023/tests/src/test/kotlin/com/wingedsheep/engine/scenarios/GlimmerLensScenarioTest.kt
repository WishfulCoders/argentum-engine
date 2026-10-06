package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.onc.cards.GlimmerLens
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Glimmer Lens (ONC #6) — {1}{W} Artifact — Equipment, For Mirrodin!, equip {1}{W}.
 *
 *   Whenever equipped creature and at least one other creature attack, draw a card.
 *
 * Pins the ATTACHED-bound battalion shape: the draw needs the equipped creature among two or more
 * attackers. Attacking alone, or attacking with others while the equipped creature stays home,
 * draws nothing.
 */
class GlimmerLensScenarioTest : ScenarioTestBase() {

    private val bearer = card("Test Lens Bearer") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Human Soldier"
        power = 2
        toughness = 2
    }

    private val friend = card("Test Lens Friend") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Human Soldier"
        power = 2
        toughness = 2
    }

    private val other = card("Test Lens Other") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Human Soldier"
        power = 2
        toughness = 2
    }

    private fun combatScenario(): TestGame = scenario()
        .withPlayers("Rebel", "Defender")
        .withCardOnBattlefield(1, "Test Lens Bearer")
        .withCardAttachedTo(1, "Glimmer Lens", "Test Lens Bearer")
        .withCardOnBattlefield(1, "Test Lens Friend")
        .withCardOnBattlefield(1, "Test Lens Other")
        .withCardInLibrary(1, "Test Lens Other")
        .withCardInLibrary(1, "Test Lens Other")
        .withCardInLibrary(2, "Test Lens Other")
        .withActivePlayer(1)
        .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        .build()

    init {
        cardRegistry.register(GlimmerLens)
        cardRegistry.register(bearer)
        cardRegistry.register(friend)
        cardRegistry.register(other)

        test("equipped creature attacking with another creature draws a card") {
            val game = combatScenario()
            val handBefore = game.handSize(1)
            game.declareAttackers(mapOf("Test Lens Bearer" to 2, "Test Lens Friend" to 2)).error shouldBe null
            game.resolveStack()
            withClue("one card for the battalion, however many others attack") {
                game.handSize(1) shouldBe handBefore + 1
            }
        }

        test("three attackers still draw exactly one card") {
            val game = combatScenario()
            val handBefore = game.handSize(1)
            game.declareAttackers(
                mapOf("Test Lens Bearer" to 2, "Test Lens Friend" to 2, "Test Lens Other" to 2)
            ).error shouldBe null
            game.resolveStack()
            game.handSize(1) shouldBe handBefore + 1
        }

        test("equipped creature attacking alone draws nothing") {
            val game = combatScenario()
            val handBefore = game.handSize(1)
            game.declareAttackers(mapOf("Test Lens Bearer" to 2)).error shouldBe null
            game.resolveStack()
            game.handSize(1) shouldBe handBefore
        }

        test("other creatures attacking without the equipped creature draw nothing") {
            val game = combatScenario()
            val handBefore = game.handSize(1)
            game.declareAttackers(mapOf("Test Lens Friend" to 2, "Test Lens Other" to 2)).error shouldBe null
            game.resolveStack()
            game.handSize(1) shouldBe handBefore
        }
    }
}
