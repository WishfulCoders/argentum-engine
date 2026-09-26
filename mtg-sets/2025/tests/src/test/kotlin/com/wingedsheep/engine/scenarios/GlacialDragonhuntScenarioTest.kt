package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tdm.cards.GlacialDragonhunt
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import com.wingedsheep.engine.core.Outcome

/**
 * Glacial Dragonhunt — {U}{R} Sorcery.
 *   "Draw a card, then you may discard a card. When you discard a nonland card this way,
 *    Glacial Dragonhunt deals 3 damage to target creature."
 *
 * The "when you discard a nonland card this way" damage is a reflexive triggered ability
 * (CR 603.12): it exists only when a nonland card is actually discarded, and its target creature is
 * chosen as it goes on the stack — so hexproof applies and a target gone by resolution fizzles it
 * (CR 608.2b).
 */
class GlacialDragonhuntScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(GlacialDragonhunt)
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("discarding a nonland card lets you deal 3 damage to a target creature") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != player }

        val spell = driver.putCardInHand(player, "Glacial Dragonhunt")
        // A nonland card in hand to discard (a creature card is nonland).
        driver.putCardInHand(player, "Grizzly Bears")
        // Two creatures so the reflexive damage genuinely presents a target choice.
        val target = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears") // 2/2 — the one we damage
        val bystander = driver.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3 — left alone
        driver.putCreatureOnBattlefield(opponent, "Sacred Wolf") // hexproof — never a legal target
        driver.giveMana(player, Color.BLUE, 1)
        driver.giveMana(player, Color.RED, 1)

        driver.submit(
            CastSpell(player, spell, paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        // First decision: "you may discard a card" (up to one) — choose the Grizzly Bears card.
        val discardDecision = driver.pendingDecision as? SelectCardsDecision
        discardDecision shouldNotBe null
        val nonlandCard = discardDecision!!.options.first { cardId ->
            driver.state.getEntity(cardId)
                ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                ?.name == "Grizzly Bears"
        }
        driver.submitDecision(player, CardsSelectedResponse(discardDecision.id, listOf(nonlandCard)))

        // Reflexive (CR 603.12): the damage ability goes on the stack and targets as it does.
        val targetDecision = driver.pendingDecision as? ChooseTargetsDecision
        targetDecision shouldNotBe null
        withClue("any creature except the opponent's hexproof one") {
            targetDecision!!.legalTargets[0]?.toSet() shouldBe setOf(target, bystander)
        }
        driver.submitDecision(player, TargetsResponse(targetDecision!!.id, mapOf(0 to listOf(target)))).error shouldBe null
        withClue("the damage ability waits on the stack, so it can be responded to") {
            driver.stackSize shouldBe 1
        }
        driver.bothPass()

        // 2/2 takes 3 damage → dies; the untargeted 3/3 is untouched.
        driver.state.getBattlefield().contains(target) shouldBe false
        driver.state.getZone(ZoneKey(opponent, Zone.GRAVEYARD)).contains(target) shouldBe true
        driver.state.getBattlefield().contains(bystander) shouldBe true
        (driver.state.getEntity(bystander)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
    }

    test("declining to discard deals no damage") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != player }

        val spell = driver.putCardInHand(player, "Glacial Dragonhunt")
        driver.putCardInHand(player, "Grizzly Bears")
        val survivor = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears") // 2/2
        driver.giveMana(player, Color.BLUE, 1)
        driver.giveMana(player, Color.RED, 1)

        driver.submit(
            CastSpell(player, spell, paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        // "You may discard a card" — decline by selecting zero cards.
        val discardDecision = driver.pendingDecision as? SelectCardsDecision
        discardDecision shouldNotBe null
        discardDecision!!.minSelections shouldBe 0
        driver.submitDecision(player, CardsSelectedResponse(discardDecision.id, emptyList()))

        // No nonland discarded → no target prompt, no damage.
        driver.pendingDecision shouldBe null
        driver.state.getBattlefield().contains(survivor) shouldBe true
        (driver.state.getEntity(survivor)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
    }

    test("a target that leaves the battlefield before the reflexive ability resolves takes nothing with it") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != player }

        val spell = driver.putCardInHand(player, "Glacial Dragonhunt")
        driver.putCardInHand(player, "Grizzly Bears")
        val target = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val bystander = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        driver.giveMana(player, Color.BLUE, 1)
        driver.giveMana(player, Color.RED, 1)

        driver.submit(CastSpell(player, spell, paymentStrategy = PaymentStrategy.FromPool)).outcome shouldBe Outcome.Done
        driver.bothPass()
        val discardDecision = driver.pendingDecision as SelectCardsDecision
        val nonland = discardDecision.options.first { driver.getCardName(it) == "Grizzly Bears" }
        driver.submitDecision(player, CardsSelectedResponse(discardDecision.id, listOf(nonland)))
        val targetDecision = driver.pendingDecision as ChooseTargetsDecision
        driver.submitDecision(player, TargetsResponse(targetDecision.id, mapOf(0 to listOf(target)))).error shouldBe null

        driver.moveToGraveyard(target)
        driver.bothPass()

        withClue("the ability has no legal target and does nothing (CR 608.2b)") {
            (driver.state.getEntity(bystander)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
        }
    }

    test("discarding a land triggers nothing") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != player }

        val spell = driver.putCardInHand(player, "Glacial Dragonhunt")
        driver.putCardInHand(player, "Island")
        val survivor = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.giveMana(player, Color.BLUE, 1)
        driver.giveMana(player, Color.RED, 1)

        driver.submit(CastSpell(player, spell, paymentStrategy = PaymentStrategy.FromPool)).outcome shouldBe Outcome.Done
        driver.bothPass()
        val discardDecision = driver.pendingDecision as SelectCardsDecision
        val land = discardDecision.options.first { driver.getCardName(it) == "Island" }
        driver.submitDecision(player, CardsSelectedResponse(discardDecision.id, listOf(land)))

        driver.pendingDecision shouldBe null
        driver.stackSize shouldBe 0
        (driver.state.getEntity(survivor)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
    }
})
