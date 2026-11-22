package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.snc.cards.ObscuraAscendancy
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Obscura Ascendancy (SNC) —
 * Whenever you cast a spell, if its mana value is equal to 1 plus the number of soul counters on
 * this enchantment, put a soul counter on this enchantment, then create a 2/2 white Spirit
 * creature token with flying.
 * As long as there are five or more soul counters on this enchantment, Spirits you control get +3/+3.
 */
class ObscuraAscendancyScenarioTest : FunSpec({

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + ObscuraAscendancy)
        initMirrorMatch(deck = Deck.of("Plains" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.soul(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.SOUL) ?: 0

    fun GameTestDriver.ascendancyWith(you: EntityId, counters: Int): EntityId =
        putPermanentOnBattlefield(you, "Obscura Ascendancy").also {
            if (counters > 0) addComponent(it, CountersComponent(mapOf(CounterType.SOUL to counters)))
        }

    fun GameTestDriver.spirits(you: EntityId): List<EntityId> =
        getCreatures(you).filter { state.projectedState.hasSubtype(it, "Spirit") }

    /** Cast Lightning Bolt (mana value 1) at the opponent, holding priority. */
    fun GameTestDriver.castBolt(you: EntityId) {
        val bolt = putCardInHand(you, "Lightning Bolt")
        giveMana(you, Color.RED, 1)
        submit(
            CastSpell(
                you, bolt,
                targets = listOf(ChosenTarget.Player(getOpponent(you))),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).outcome shouldBe Outcome.Done
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (guard++ < 20 && state.stack.isNotEmpty()) bothPass()
    }

    test("a spell with mana value one more than its soul counters adds a counter and makes a 2/2 flying Spirit") {
        val d = setup()
        val you = d.activePlayer!!
        val ascendancy = d.ascendancyWith(you, 0)

        d.castBolt(you)
        d.resolveStack()

        d.soul(ascendancy) shouldBe 1
        val spirits = d.spirits(you)
        spirits shouldHaveSize 1
        val projected = d.state.projectedState
        projected.getPower(spirits.single()) shouldBe 2
        projected.getToughness(spirits.single()) shouldBe 2
        projected.hasKeyword(spirits.single(), Keyword.FLYING) shouldBe true
    }

    test("a spell with any other mana value doesn't trigger it") {
        val d = setup()
        val you = d.activePlayer!!
        val ascendancy = d.ascendancyWith(you, 1) // needs mana value 2

        d.castBolt(you)
        d.resolveStack()

        d.soul(ascendancy) shouldBe 1
        d.spirits(you) shouldHaveSize 0
    }

    test("two mana-value-1 spells cast back to back give only one counter and one token (intervening if)") {
        val d = setup()
        val you = d.activePlayer!!
        val ascendancy = d.ascendancyWith(you, 0)

        d.castBolt(you)
        d.castBolt(you)
        d.resolveStack()

        d.soul(ascendancy) shouldBe 1
        d.spirits(you) shouldHaveSize 1
    }

    test("Spirits you control get +3/+3 only once it has five soul counters") {
        val d = setup()
        val you = d.activePlayer!!
        val ascendancy = d.ascendancyWith(you, 0)
        d.castBolt(you)
        d.resolveStack()
        val spirit = d.spirits(you).single()

        d.addComponent(ascendancy, CountersComponent(mapOf(CounterType.SOUL to 4)))
        d.state.projectedState.getPower(spirit) shouldBe 2
        d.state.projectedState.getToughness(spirit) shouldBe 2

        d.addComponent(ascendancy, CountersComponent(mapOf(CounterType.SOUL to 5)))
        d.state.projectedState.getPower(spirit) shouldBe 5
        d.state.projectedState.getToughness(spirit) shouldBe 5
    }
})
