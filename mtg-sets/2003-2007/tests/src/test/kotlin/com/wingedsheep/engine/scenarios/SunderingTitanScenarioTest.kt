package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dst.cards.SunderingTitan
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Sundering Titan (DST #146) — "When this creature enters or leaves the battlefield, choose a land
 * of each basic land type, then destroy those lands."
 *
 * The choice semantics are pinned at the engine level by `ChooseOnePerCategoryChooserTest`; here
 * the card's two triggers are exercised: the enter trigger with the controller choosing among
 * both players' lands, and the leave trigger firing again when the Titan dies.
 */
class SunderingTitanScenarioTest : FunSpec({

    val doom = card("Test Doom") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.Destroy(creature)
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + SunderingTitan + doom)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0, skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.resolveWith(pick: (SelectCardsDecision) -> EntityId): List<SelectCardsDecision> {
        val seen = mutableListOf<SelectCardsDecision>()
        var guard = 0
        while (guard++ < 40) {
            val decision = pendingDecision
            when {
                decision is SelectCardsDecision -> {
                    seen += decision
                    submitCardSelection(decision.playerId, listOf(pick(decision)))
                }
                isPaused -> autoResolveDecision()
                stackSize > 0 -> bothPass()
                else -> break
            }
        }
        return seen
    }

    test("enters: its controller picks a land of each basic type from every player's lands") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val myMountains = List(8) { driver.putLandOnBattlefield(me, "Mountain") }
        val myForest = driver.putLandOnBattlefield(me, "Forest")
        val theirMountain = driver.putLandOnBattlefield(opponent, "Mountain")
        val theirIsland = driver.putLandOnBattlefield(opponent, "Island")
        val theirSwamp = driver.putLandOnBattlefield(opponent, "Swamp")

        val titan = driver.putCardInHand(me, "Sundering Titan")
        driver.castSpell(me, titan).outcome shouldBe Outcome.Done
        driver.bothPass()
        val prompts = driver.resolveWith { decision ->
            withClue("only the Mountain choice has more than one candidate") {
                decision.options shouldContainExactlyInAnyOrder myMountains + theirMountain
            }
            decision.playerId shouldBe me
            theirMountain
        }

        prompts.size shouldBe 1
        withClue("the chosen opposing Mountain, the lone Island and Swamp, and my lone Forest are destroyed") {
            driver.getLands(opponent).size shouldBe 0
            driver.getLands(me) shouldContainExactlyInAnyOrder myMountains
        }
        driver.getGraveyard(me).contains(myForest) shouldBe true
        driver.getGraveyard(opponent) shouldContainExactlyInAnyOrder
            listOf(theirMountain, theirIsland, theirSwamp)
    }

    test("leaves: the trigger fires again when the Titan dies") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val titan = driver.putCreatureOnBattlefield(me, "Sundering Titan")
        val theirPlains = driver.putLandOnBattlefield(opponent, "Plains")
        val myMountain = driver.putLandOnBattlefield(me, "Mountain")

        val kill = driver.putCardInHand(me, "Test Doom")
        driver.castSpell(me, kill, listOf(titan)).outcome shouldBe Outcome.Done
        driver.bothPass()
        val prompts = driver.resolveWith { error("every basic land type has at most one land") }

        prompts.size shouldBe 0
        driver.getGraveyard(me).contains(titan) shouldBe true

        driver.getLands(opponent).contains(theirPlains) shouldBe false
        driver.getLands(me).contains(myMountain) shouldBe false
    }
})
