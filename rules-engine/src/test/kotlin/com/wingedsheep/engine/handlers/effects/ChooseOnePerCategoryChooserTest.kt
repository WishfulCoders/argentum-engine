package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * `ChooseOnePerCategoryEffect.chooser` — one player makes every per-category pick over the whole
 * pool, whoever controls each member ("choose a land of each basic land type, then destroy those
 * lands", Sundering Titan). Pinned against the 2020-08-07 Sundering Titan rulings:
 *
 * - the controller chooses, from among **all** players' lands;
 * - "if one of the basic land types isn't present, it isn't chosen";
 * - "if the only land of a certain type is one you control, you must choose it" (a lone candidate
 *   is forced, no prompt);
 * - "if a land has more than one basic land type, it can be chosen more than once";
 * - the chosen lands are destroyed together, after every choice is made.
 */
class ChooseOnePerCategoryChooserTest : FunSpec({

    val sunder = card("Test Sunder") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Pipeline {
                val lands = gather(CardSource.BattlefieldMatching(GameObjectFilter.Land))
                destroy(
                    chooseOnePerCategory(lands, Filters.BasicLandTypes, chooser = Chooser.Controller, purpose = "destroy")
                )
            }
        }
    }
    val tundra = card("Test Tundra") {
        typeLine = "Land — Plains Island"
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + sunder + tundra)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0, skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Cast the probe and answer every prompt with [pick]; returns the prompts seen. */
    fun GameTestDriver.castSunder(caster: EntityId, pick: (SelectCardsDecision) -> EntityId): List<SelectCardsDecision> {
        val spell = putCardInHand(caster, "Test Sunder")
        castSpell(caster, spell).outcome shouldBe Outcome.Done
        bothPass()
        val seen = mutableListOf<SelectCardsDecision>()
        var guard = 0
        while (isPaused && guard++ < 20) {
            val decision = pendingDecision as SelectCardsDecision
            seen += decision
            submitCardSelection(decision.playerId, listOf(pick(decision)))
        }
        return seen
    }

    test("the caster chooses among every player's lands, and only the chosen ones are destroyed") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val myPlains = driver.putLandOnBattlefield(me, "Plains")
        val theirPlains = driver.putLandOnBattlefield(opponent, "Plains")
        val theirIsland = driver.putLandOnBattlefield(opponent, "Island")

        val prompts = driver.castSunder(me) { theirPlains }

        withClue("one prompt — Plains; the lone Island is forced, the absent types are skipped") {
            prompts.size shouldBe 1
        }
        val plainsPrompt = prompts.single()
        plainsPrompt.playerId shouldBe me
        plainsPrompt.prompt shouldContain "to destroy"
        plainsPrompt.options shouldContainExactlyInAnyOrder listOf(myPlains, theirPlains)

        driver.getLands(me) shouldBe listOf(myPlains)
        driver.getLands(opponent).size shouldBe 0
        driver.getGraveyardCardNames(opponent).count { it == "Plains" } shouldBe 1
        driver.getGraveyardCardNames(opponent).count { it == "Island" } shouldBe 1
    }

    test("the only land of a type is chosen even when the chooser controls it") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putLandOnBattlefield(me, "Mountain")
        val theirForest = driver.putLandOnBattlefield(opponent, "Forest")

        driver.castSunder(me) { error("no prompt expected") }

        driver.getLands(me).size shouldBe 0
        driver.getLands(opponent).contains(theirForest) shouldBe false
    }

    test("a land with two basic land types may be chosen for both") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val dual = driver.putLandOnBattlefield(opponent, "Test Tundra")
        val otherPlains = driver.putLandOnBattlefield(opponent, "Plains")
        val otherIsland = driver.putLandOnBattlefield(opponent, "Island")

        val prompts = driver.castSunder(me) { dual }

        withClue("asked once for Plains and once for Island, the dual offered both times") {
            prompts.size shouldBe 2
            prompts.forEach { (dual in it.options) shouldBe true }
        }
        driver.getLands(opponent) shouldContainExactlyInAnyOrder listOf(otherPlains, otherIsland)
    }
})
