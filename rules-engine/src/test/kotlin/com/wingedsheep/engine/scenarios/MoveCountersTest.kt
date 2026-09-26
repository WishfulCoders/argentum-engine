package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for [com.wingedsheep.sdk.scripting.effects.MoveCountersEffect] — moving a dynamic count
 * of a single counter kind from one permanent onto another (the engine gap behind Tester of the
 * Tangential's "move X +1/+1 counters from this creature onto another target creature").
 *
 * An inline instant "Counter Mover" selects a target creature and moves a fixed number of +1/+1
 * counters from the caster's own source creature (modeled with a second targeted creature here for
 * test simplicity) onto it. It pins:
 *  - the happy path (N counters leave the source, N arrive on the destination),
 *  - the cap-at-present rule (you can't move more counters than the source has),
 *  - the no-op when the source has none of that kind.
 */
class MoveCountersTest : FunSpec({

    // "Move up to <amount> +1/+1 counters from the first target onto the second target."
    fun mover(amount: Int) = card("Counter Mover $amount") {
        manaCost = "{G}"
        typeLine = "Instant"
        oracleText = "Move $amount +1/+1 counters from target creature onto another target creature."
        spell {
            val source = target(TargetFilter.Creature)
            val dest = target(TargetFilter.OtherCreature)
            effect = Effects.MoveCounters(
                counterType = CounterType.PLUS_ONE_PLUS_ONE,
                amount = DynamicAmount.Fixed(amount),
                source = source,
                destination = dest,
            )
        }
    }

    fun createDriver(amount: Int): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(mover(amount)))
        return driver
    }

    fun plusCounters(driver: GameTestDriver, entityId: com.wingedsheep.sdk.model.EntityId): Int =
        driver.state.getEntity(entityId)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("moves N +1/+1 counters from source to destination") {
        val driver = createDriver(2)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20), startingLife = 20)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val spell = driver.putCardInHand(p1, "Counter Mover 2")
        val src = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val dst = driver.putCreatureOnBattlefield(p1, "Savannah Lions")
        driver.addComponent(src, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 4)))
        driver.giveMana(p1, Color.GREEN, 1)

        driver.castSpell(p1, spell, targets = listOf(src, dst))
        driver.bothPass()

        plusCounters(driver, src) shouldBe 2  // 4 - 2
        plusCounters(driver, dst) shouldBe 2  // 0 + 2
    }

    test("moving more than present is capped at the source's actual count") {
        val driver = createDriver(5)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20), startingLife = 20)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val spell = driver.putCardInHand(p1, "Counter Mover 5")
        val src = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val dst = driver.putCreatureOnBattlefield(p1, "Savannah Lions")
        driver.addComponent(src, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 3)))
        driver.giveMana(p1, Color.GREEN, 1)

        driver.castSpell(p1, spell, targets = listOf(src, dst))
        driver.bothPass()

        plusCounters(driver, src) shouldBe 0  // all 3 moved, not 5
        plusCounters(driver, dst) shouldBe 3
    }

    test("no-op when the source has no +1/+1 counters") {
        val driver = createDriver(2)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20), startingLife = 20)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val spell = driver.putCardInHand(p1, "Counter Mover 2")
        val src = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val dst = driver.putCreatureOnBattlefield(p1, "Savannah Lions")
        driver.giveMana(p1, Color.GREEN, 1)

        driver.castSpell(p1, spell, targets = listOf(src, dst))
        driver.bothPass()

        plusCounters(driver, src) shouldBe 0
        plusCounters(driver, dst) shouldBe 0
    }
})
