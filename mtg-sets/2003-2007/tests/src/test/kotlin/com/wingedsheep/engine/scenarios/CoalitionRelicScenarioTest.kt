package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fut.cards.CoalitionRelic
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Coalition Relic — the charge ability stores a counter, and at the beginning of your first main
 * phase every charge counter is stripped and paid out as one mana of any color each, each pip its
 * own color (2021-03-19 ruling). Pins the cross-turn shape: the store, the strip, and the payout.
 */
class CoalitionRelicScenarioTest : FunSpec({

    val chargeAbilityId = CoalitionRelic.activatedAbilities[1].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(CoalitionRelic)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        return driver
    }

    fun charge(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) ?: 0

    fun pool(driver: GameTestDriver, player: EntityId): ManaPoolComponent =
        driver.state.getEntity(player)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    test("tapping for a charge counter uses the stack and stores one counter") {
        val driver = createDriver()
        val p = driver.player1
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val relic = driver.putPermanentOnBattlefield(p, "Coalition Relic")

        driver.submit(ActivateAbility(p, relic, chargeAbilityId)).outcome shouldBe Outcome.Done
        driver.isTapped(relic) shouldBe true
        charge(driver, relic) shouldBe 0
        driver.bothPass()
        charge(driver, relic) shouldBe 1
    }

    test("first main phase: strips every charge counter and adds a mana of any color for each") {
        val driver = createDriver()
        val p = driver.player1
        val relic = driver.putPermanentOnBattlefield(p, "Coalition Relic")
        driver.replaceState(
            driver.state.updateEntity(relic) { c -> c.with(CountersComponent(mapOf(CounterType.CHARGE to 2))) }
        )

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.bothPass()

        val first = driver.pendingDecision.shouldBeInstanceOf<ChooseColorDecision>()
        // One color per mana: the first choice pauses for the second.
        driver.submitDecision(p, ColorChosenResponse(first.id, Color.RED)).error shouldBe null
        val second = driver.pendingDecision.shouldBeInstanceOf<ChooseColorDecision>()
        driver.submitDecision(p, ColorChosenResponse(second.id, Color.BLUE)).outcome shouldBe Outcome.Done

        charge(driver, relic) shouldBe 0
        pool(driver, p).red shouldBe 1
        pool(driver, p).blue shouldBe 1
    }

    test("no charge counters: the trigger strips nothing and adds no mana") {
        val driver = createDriver()
        val p = driver.player1
        driver.putPermanentOnBattlefield(p, "Coalition Relic")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.bothPass()

        driver.pendingDecision shouldBe null
        val mana = pool(driver, p)
        (mana.white + mana.blue + mana.black + mana.red + mana.green + mana.colorless) shouldBe 0
    }
})
