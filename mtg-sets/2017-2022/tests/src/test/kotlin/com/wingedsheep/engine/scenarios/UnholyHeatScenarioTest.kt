package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mh2.cards.UnholyHeat
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Unholy Heat (MH2 #145) — {R} Instant.
 *
 *   Unholy Heat deals 2 damage to target creature or planeswalker.
 *   Delirium — Unholy Heat deals 6 damage instead if there are four or more card types among cards
 *   in your graveyard.
 */
class UnholyHeatScenarioTest : ScenarioTestBase() {

    private val bigBeast = card("Test Heat Beast") {
        manaCost = "{5}{G}"
        typeLine = "Creature — Beast"
        power = 5
        toughness = 5
    }

    private val artifact = card("Test Heat Relic") {
        manaCost = "{1}"
        typeLine = "Artifact"
    }

    private val sorcery = card("Test Heat Sorcery") {
        manaCost = "{R}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(1) }
    }

    private fun board(delirium: Boolean): TestGame {
        var b = scenario()
            .withPlayers("Caster", "Opponent")
            .withCardInHand(1, "Unholy Heat")
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withCardOnBattlefield(2, "Test Heat Beast")
            .withCardInGraveyard(1, "Mountain")
            .withCardInGraveyard(1, "Test Heat Relic")
            .withCardInGraveyard(1, "Test Heat Sorcery")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (delirium) b = b.withCardInGraveyard(1, "Grizzly Bears")
        return b.build()
    }

    init {
        cardRegistry.register(UnholyHeat)
        cardRegistry.register(bigBeast)
        cardRegistry.register(artifact)
        cardRegistry.register(sorcery)

        test("three card types: 2 damage, the 5/5 survives") {
            val game = board(delirium = false)
            val beast = game.findPermanent("Test Heat Beast")!!
            game.castSpell(1, "Unholy Heat", beast).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Test Heat Beast") shouldBe true
        }

        test("four card types (delirium): 6 damage kills the 5/5") {
            val game = board(delirium = true)
            val beast = game.findPermanent("Test Heat Beast")!!
            game.castSpell(1, "Unholy Heat", beast).error shouldBe null
            game.resolveStack()
            withClue("land, artifact, sorcery, creature in the graveyard") {
                game.isInGraveyard(2, "Test Heat Beast") shouldBe true
            }
        }
    }
}
