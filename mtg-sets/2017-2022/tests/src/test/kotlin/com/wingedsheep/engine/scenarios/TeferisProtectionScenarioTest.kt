package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.c17.cards.TeferisProtection
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Teferi's Protection {2}{W} — Instant (C17).
 *
 * "Until your next turn, your life total can't change and you gain protection from everything.
 *  All permanents you control phase out. … Exile Teferi's Protection."
 *
 * Composes the life locks, player protection and a per-permanent phase-out, all bounded by
 * "until your next turn" — this pins that the pieces start and stop together.
 */
class TeferisProtectionScenarioTest : FunSpec({

    test("phases out your permanents and protects you until your next turn, then exiles itself") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TeferisProtection))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val courser = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        val plains = driver.putLandOnBattlefield(me, "Plains")
        val theirBears = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")

        val protection = driver.putCardInHand(me, "Teferi's Protection")
        driver.giveMana(me, Color.WHITE, 3)
        driver.castSpell(me, protection).error shouldBe null
        driver.bothPass()

        withClue("every permanent you control phased out; the opponent's did not") {
            driver.state.getEntity(courser)?.has<PhasedOutComponent>() shouldBe true
            driver.state.getEntity(plains)?.has<PhasedOutComponent>() shouldBe true
            driver.state.getBattlefield().contains(courser) shouldBe false
            driver.state.getEntity(theirBears)?.has<PhasedOutComponent>() shouldBe false
        }
        withClue("Teferi's Protection exiles itself instead of going to the graveyard") {
            driver.getExileCardNames(me) shouldBe listOf("Teferi's Protection")
            driver.getGraveyardCardNames(me).contains("Teferi's Protection") shouldBe false
        }

        // Opponent's turn: you have protection from everything, so you can't be targeted.
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, opponent)
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        withClue("you can't be the target of a spell while you have protection from everything") {
            driver.castSpell(opponent, bolt, targets = listOf(me)).error shouldNotBe null
        }
        driver.getLifeTotal(me) shouldBe 20

        // Your next turn: everything phases back in and the protection is gone.
        driver.passPriorityUntil(Step.UPKEEP, me)
        withClue("your permanents phase in before your untap step") {
            driver.state.getEntity(courser)?.has<PhasedOutComponent>() shouldBe false
            driver.state.getEntity(plains)?.has<PhasedOutComponent>() shouldBe false
            driver.state.getBattlefield().contains(courser) shouldBe true
        }
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, me)
        val secondBolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        withClue("protection ended with your next turn, so you can be targeted again") {
            driver.castSpell(me, secondBolt, targets = listOf(me)).error shouldBe null
        }
        driver.bothPass()
        withClue("your life total can change again") {
            driver.getLifeTotal(me) shouldBe 17
        }
    }
})
