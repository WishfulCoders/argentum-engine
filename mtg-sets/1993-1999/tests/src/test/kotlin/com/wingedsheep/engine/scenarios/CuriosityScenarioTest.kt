package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.exo.cards.Curiosity
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Curiosity (EXO #29) — "Enchant creature. Whenever enchanted creature deals damage to an opponent,
 * you may draw a card."
 *
 * The rulings make "you" and "an opponent" both relative to Curiosity's controller, not to the
 * enchanted creature's: on your own creature it draws when the creature hits your opponent; on an
 * opponent's creature it does nothing when that creature hits you. Damage to a planeswalker is not
 * damage to an opponent.
 */
class CuriosityScenarioTest : FunSpec({

    val testWalker = card("Test Walker") {
        manaCost = "{2}"
        typeLine = "Legendary Planeswalker — Tester"
        startingLoyalty = 3
        loyaltyAbility(1) {
            effect = Effects.GainLife(1)
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Curiosity, testWalker))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.enchant(caster: EntityId, creature: EntityId) {
        val aura = putCardInHand(caster, "Curiosity")
        giveMana(caster, Color.BLUE, 1)
        castSpell(caster, aura, listOf(creature)).error shouldBe null
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 10) bothPass()
    }

    /** Play out the rest of this combat, answering Curiosity's question yes; returns how often it asked. */
    fun GameTestDriver.finishCombat(): Int {
        var prompts = 0
        var guard = 0
        while (state.step != Step.POSTCOMBAT_MAIN && guard++ < 60) {
            val d = pendingDecision
            when {
                d is YesNoDecision && d.context.sourceName == "Curiosity" -> { prompts++; submitYesNo(d.playerId, true) }
                d != null -> autoResolveDecision()
                state.priorityPlayerId != null -> {
                    autoSubmitCombatDeclarationIfNeeded()
                    passPriority(state.priorityPlayerId!!)
                }
            }
        }
        return prompts
    }

    test("your enchanted creature damaging your opponent lets you draw") {
        val driver = newDriver()
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        val lions = driver.putCreatureOnBattlefield(you, "Savannah Lions")
        driver.removeSummoningSickness(lions)
        driver.enchant(you, lions)
        val handBefore = driver.getHand(you).size

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(lions), opponent).error shouldBe null
        val prompts = driver.finishCombat()

        prompts shouldBe 1
        driver.getLifeTotal(opponent) shouldBe 19
        driver.getHand(you).size shouldBe handBefore + 1
    }

    test("on an opponent's creature, that creature damaging you does not trigger it") {
        val driver = newDriver()
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        val lions = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")
        driver.removeSummoningSickness(lions)
        driver.enchant(you, lions)

        // Round to the opponent's combat; their Lions, wearing your Curiosity, attacks you.
        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.state.activePlayerId shouldBe opponent
        val handBefore = driver.getHand(you).size
        driver.declareAttackers(opponent, listOf(lions), you).error shouldBe null
        val prompts = driver.finishCombat()

        withClue("the damage was dealt to you, Curiosity's controller — not to an opponent of yours") {
            driver.getLifeTotal(you) shouldBe 19
            prompts shouldBe 0
            driver.getHand(you).size shouldBe handBefore
        }
    }

    test("damage to a planeswalker is not damage to an opponent") {
        val driver = newDriver()
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        val walker = driver.putPermanentOnBattlefield(opponent, "Test Walker")
        driver.replaceState(
            driver.state.updateEntity(walker) { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent()).withCounters(CounterType.LOYALTY, 3))
            }
        )
        val lions = driver.putCreatureOnBattlefield(you, "Savannah Lions")
        driver.removeSummoningSickness(lions)
        driver.enchant(you, lions)
        val handBefore = driver.getHand(you).size

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, mapOf(lions to walker)).error shouldBe null
        val prompts = driver.finishCombat()

        driver.state.getEntity(walker)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 2
        prompts shouldBe 0
        driver.getHand(you).size shouldBe handBefore
    }
})
