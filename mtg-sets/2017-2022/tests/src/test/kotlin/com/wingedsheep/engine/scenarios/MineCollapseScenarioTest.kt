package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh2.cards.MineCollapse
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Mine Collapse (MH2 #135) — {3}{R} Instant.
 *
 *   If it's your turn, you may sacrifice a Mountain rather than pay this spell's mana cost.
 *   Mine Collapse deals 5 damage to target creature or planeswalker.
 */
class MineCollapseScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MineCollapse))
        return driver
    }

    test("on your turn: sacrifice a Mountain instead of paying, 5 damage kills a 5/5") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opp = driver.getOpponent(you)

        val mountain = driver.putLandOnBattlefield(you, "Mountain")
        val beast = driver.putCreatureOnBattlefield(opp, "Force of Nature")
        val spell = driver.putCardInHand(you, "Mine Collapse")

        driver.submit(
            CastSpell(
                playerId = you,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(beast)),
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE,
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(mountain))
            )
        ).error shouldBe null
        while (driver.stackSize > 0) driver.bothPass()

        driver.getGraveyardCardNames(you).contains("Mountain") shouldBe true
        driver.state.getBattlefield(you).contains(mountain) shouldBe false
        driver.state.getBattlefield(opp).contains(beast) shouldBe false
    }

    test("not your turn: the Mountain alternative cost is refused") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20, startingPlayer = 1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!
        val you = driver.getOpponent(active)
        driver.passPriority(active)

        val mountain = driver.putLandOnBattlefield(you, "Mountain")
        val beast = driver.putCreatureOnBattlefield(active, "Force of Nature")
        val spell = driver.putCardInHand(you, "Mine Collapse")

        driver.submit(
            CastSpell(
                playerId = you,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(beast)),
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE,
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(mountain))
            )
        ).error shouldNotBe null
        driver.state.getBattlefield(you).contains(mountain) shouldBe true
        driver.state.getBattlefield(active).contains(beast) shouldBe true
    }
})
