package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dgm.cards.VoiceOfResurgence
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Voice of Resurgence (DGM) — {G}{W} Creature — Elemental 2/2
 *
 * "Whenever an opponent casts a spell during your turn and when this creature dies, create a green
 *  and white Elemental creature token with "This token's power and toughness are each equal to the
 *  number of creatures you control.""
 *
 * The load-bearing checks: both halves of the "and when" trigger fire, the cast half is gated to
 * *your* turn, and the token's P/T is a live characteristic-defining ability — it shrinks when the
 * Voice dies and grows when another creature arrives, rather than freezing at creation.
 */
class VoiceOfResurgenceScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(VoiceOfResurgence)
        return driver
    }

    fun GameTestDriver.elementalTokens(player: com.wingedsheep.sdk.model.EntityId) =
        getPermanents(player).filter { getCardName(it) == "Elemental Token" }

    test("an opponent's spell on your turn and the Voice dying each make a live-P/T token") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 20, "Forest" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val voice = driver.putCreatureOnBattlefield(me, "Voice of Resurgence")

        // Opponent Bolts the Voice on my turn: the cast trigger resolves first (Voice + token = 2).
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.passPriority(me)
        driver.castSpell(opponent, bolt, listOf(voice))
        driver.bothPass()

        val first = driver.elementalTokens(me).single()
        driver.state.projectedState.getPower(first) shouldBe 2
        driver.state.projectedState.getToughness(first) shouldBe 2

        // Bolt resolves, the Voice dies, and its dies trigger makes a second token.
        driver.bothPass()
        driver.findPermanent(me, "Voice of Resurgence") shouldBe null
        driver.bothPass()

        val tokens = driver.elementalTokens(me)
        tokens shouldHaveSize 2
        tokens.forEach {
            driver.state.projectedState.getPower(it) shouldBe 2
            driver.state.projectedState.getToughness(it) shouldBe 2
        }

        // The CDA keeps counting: another creature grows both tokens.
        driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        tokens.forEach {
            driver.state.projectedState.getPower(it) shouldBe 3
            driver.state.projectedState.getToughness(it) shouldBe 3
        }
    }

    test("an opponent casting a spell on their own turn does not trigger") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 20, "Forest" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val opponent = driver.activePlayer!!
        val me = driver.getOpponent(opponent)

        driver.putCreatureOnBattlefield(me, "Voice of Resurgence")

        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.castSpell(opponent, bolt, listOf(me))
        driver.bothPass()

        driver.getLifeTotal(me) shouldBe 17
        driver.elementalTokens(me).shouldBeEmpty()
    }
})
