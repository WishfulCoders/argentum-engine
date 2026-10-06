package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.`5dn`.cards.PentadPrism
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Pentad Prism — Sunburst on a noncreature artifact: it enters with a charge counter for each
 * *color* of mana spent to cast it (CR 702.44a), and each counter can be removed for one mana of any
 * color. Pins the first noncreature use of the colors-spent count at entry: colors not pips,
 * and colorless counts for nothing.
 */
class PentadPrismScenarioTest : FunSpec({

    val abilityId = PentadPrism.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(PentadPrism)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun charge(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) ?: 0

    fun pool(driver: GameTestDriver, player: EntityId): ManaPoolComponent =
        driver.state.getEntity(player)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    test("two colors spent: enters with two charge counters, each spent for one mana of any color") {
        val driver = createDriver()
        val p = driver.activePlayer!!
        val prism = driver.putCardInHand(p, "Pentad Prism")
        driver.giveMana(p, Color.WHITE, 1)
        driver.giveMana(p, Color.BLUE, 1)

        driver.castSpell(p, prism).outcome shouldBe Outcome.Done
        driver.bothPass()
        charge(driver, prism) shouldBe 2

        driver.submit(ActivateAbility(p, prism, abilityId, manaColorChoice = Color.RED)).outcome shouldBe Outcome.Done
        pool(driver, p).red shouldBe 1
        charge(driver, prism) shouldBe 1

        driver.submit(ActivateAbility(p, prism, abilityId, manaColorChoice = Color.BLACK)).outcome shouldBe Outcome.Done
        pool(driver, p).black shouldBe 1
        charge(driver, prism) shouldBe 0

        // Out of counters: the cost can't be paid, and the Prism stays on the battlefield.
        driver.submitExpectFailure(ActivateAbility(p, prism, abilityId, manaColorChoice = Color.GREEN))
        driver.findPermanent(p, "Pentad Prism") shouldBe prism
    }

    test("counts colors, not pips: two white mana gives one counter") {
        val driver = createDriver()
        val p = driver.activePlayer!!
        val prism = driver.putCardInHand(p, "Pentad Prism")
        driver.giveMana(p, Color.WHITE, 2)

        driver.castSpell(p, prism).outcome shouldBe Outcome.Done
        driver.bothPass()
        charge(driver, prism) shouldBe 1
    }

    test("colorless mana is not a color: no counters") {
        val driver = createDriver()
        val p = driver.activePlayer!!
        val prism = driver.putCardInHand(p, "Pentad Prism")
        driver.giveColorlessMana(p, 2)

        driver.castSpell(p, prism).outcome shouldBe Outcome.Done
        driver.bothPass()
        charge(driver, prism) shouldBe 0
    }
})
