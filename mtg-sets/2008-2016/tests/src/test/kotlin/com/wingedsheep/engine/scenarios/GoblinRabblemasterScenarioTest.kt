package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m15.cards.GoblinRabblemaster
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Goblin Rabblemaster (M15 #145) — {2}{R} Creature — Goblin Warrior 2/2.
 *
 *   Other Goblin creatures you control attack each combat if able.
 *   At the beginning of combat on your turn, create a 1/1 red Goblin creature token with haste.
 *   Whenever this creature attacks, it gets +1/+0 until end of turn for each other attacking Goblin.
 */
class GoblinRabblemasterScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GoblinRabblemaster))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.goblinTokens(player: EntityId): List<EntityId> =
        getPermanents(player).filter { getCardName(it) == "Goblin Token" }

    test("beginning of combat on your turn makes a 1/1 hasty Goblin token") {
        val driver = newDriver()
        val me = driver.player1
        driver.putCreatureOnBattlefield(me, "Goblin Rabblemaster")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        val tokens = driver.goblinTokens(me)
        withClue("tokens: ${driver.getPermanents(me).map { driver.getCardName(it) }}") { tokens.size shouldBe 1 }
        val token = tokens.single()
        driver.state.projectedState.getPower(token) shouldBe 1
        driver.state.projectedState.getToughness(token) shouldBe 1
        driver.state.projectedState.hasKeyword(token, Keyword.HASTE) shouldBe true
    }

    test("other Goblins must attack; Rabblemaster gets +1/+0 per other attacking Goblin") {
        val driver = newDriver()
        val me = driver.player1
        val opp = driver.player2
        val rabble = driver.putCreatureOnBattlefield(me, "Goblin Rabblemaster")
        val guide = driver.putCreatureOnBattlefield(me, "Goblin Guide")
        driver.removeSummoningSickness(rabble)
        driver.removeSummoningSickness(guide)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val token = driver.goblinTokens(me).single()

        withClue("leaving the other Goblins home is illegal") {
            driver.declareAttackers(me, listOf(rabble), opp).outcome shouldNotBe Outcome.Done
        }

        driver.declareAttackers(me, listOf(rabble, guide, token), opp).outcome shouldBe Outcome.Done
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        driver.state.projectedState.getPower(rabble) shouldBe 4
        driver.state.projectedState.getToughness(rabble) shouldBe 2
    }

    test("Rabblemaster itself is not forced to attack") {
        val driver = newDriver()
        val me = driver.player1
        val opp = driver.player2
        val rabble = driver.putCreatureOnBattlefield(me, "Goblin Rabblemaster")
        driver.removeSummoningSickness(rabble)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val token = driver.goblinTokens(me).single()

        driver.declareAttackers(me, listOf(token), opp).outcome shouldBe Outcome.Done
    }
})
