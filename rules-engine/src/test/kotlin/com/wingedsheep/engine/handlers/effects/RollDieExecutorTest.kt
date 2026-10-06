package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.DieRolledEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.composite.RollDieExecutor
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.GameRng
import com.wingedsheep.sdk.scripting.effects.DIE_ROLL_RESULT
import com.wingedsheep.sdk.scripting.effects.RollDieEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [RollDieExecutor] in isolation (CR 706): the natural result is a uniform draw from `1..sides`
 * taken from the game's seeded RNG (CR 706.1a), a printed modifier is added to make the result
 * (CR 706.2), the result is published to the pipeline, and every roll reports one
 * [DieRolledEvent] carrying both numbers.
 */
class RollDieExecutorTest : FunSpec({

    val executor = RollDieExecutor(PredicateEvaluator(cardRegistry = null).amounts)

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Island" to 40))
    }

    test("CR 706.1a: a d6 rolls each of 1..6, roughly equally often, and nothing else") {
        val driver = setup()
        val context = EffectContext(sourceId = null, controllerId = driver.activePlayer!!)
        val counts = IntArray(7)
        var state = driver.state.copy(rng = GameRng.seeded(7L))
        repeat(600) {
            val result = executor.execute(state, RollDieEffect(6), context)
            val rolled = result.updatedStoredNumbers.getValue(DIE_ROLL_RESULT)
            rolled shouldBeInRange 1..6
            counts[rolled]++
            state = result.state
        }
        counts[0] shouldBe 0
        (1..6).forEach { face -> counts[face] shouldBeInRange 60..140 }
    }

    test("the roll comes from the seeded RNG: same seed, same roll; the generator advances") {
        val driver = setup()
        val context = EffectContext(sourceId = null, controllerId = driver.activePlayer!!)
        val seeded = driver.state.copy(rng = GameRng.seeded(424242L))

        val first = executor.execute(seeded, RollDieEffect(20), context)
        val second = executor.execute(seeded, RollDieEffect(20), context)

        first.updatedStoredNumbers shouldBe second.updatedStoredNumbers
        first.state.rng shouldBe second.state.rng
        first.state.rng shouldNotBe seeded.rng
        first.events shouldBe second.events
    }

    test("a one-sided die always rolls 1") {
        val driver = setup()
        val context = EffectContext(sourceId = null, controllerId = driver.activePlayer!!)
        var state = driver.state
        repeat(20) {
            val result = executor.execute(state, RollDieEffect(1, "one"), context)
            result.updatedStoredNumbers shouldBe mapOf("one" to 1)
            state = result.state
        }
    }

    test("CR 706.2: the printed modifier makes the result; the event keeps the natural result") {
        val driver = setup()
        val player = driver.activePlayer!!
        val context = EffectContext(sourceId = null, controllerId = player)
        val state = driver.state.copy(rng = GameRng.seeded(99L))

        val plain = executor.execute(state, RollDieEffect(20), context)
        val natural = plain.updatedStoredNumbers.getValue(DIE_ROLL_RESULT)

        val modified = executor.execute(state, RollDieEffect(20, "r", DynamicAmount.Fixed(-25)), context)
        modified.updatedStoredNumbers shouldBe mapOf("r" to natural - 25)

        val event = modified.events.single() as DieRolledEvent
        event.playerId shouldBe player
        event.sides shouldBe 20
        event.naturalResult shouldBe natural
        event.result shouldBe natural - 25
        event.sourceId shouldBe player
        event.sourceName shouldBe "Unknown"
    }

    test("one DieRolledEvent per roll, and successive rolls are independent draws") {
        val driver = setup()
        val context = EffectContext(sourceId = null, controllerId = driver.activePlayer!!)
        var state = driver.state.copy(rng = GameRng.seeded(3L))
        val rolls = mutableListOf<Int>()
        repeat(30) {
            val result = executor.execute(state, RollDieEffect(20), context)
            result.events.map { (it as DieRolledEvent).naturalResult } shouldContainExactly
                listOf(result.updatedStoredNumbers.getValue(DIE_ROLL_RESULT))
            rolls += result.updatedStoredNumbers.getValue(DIE_ROLL_RESULT)
            state = result.state
        }
        // Thirty d20 rolls threaded through one generator are not all the same number.
        rolls.toSet().size shouldNotBe 1
    }
})
