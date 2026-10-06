package com.wingedsheep.sdk.dsl.mechanics

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.scripting.conditions.AllConditions
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.DIE_ROLL_RESULT
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.RollDieEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.Json

/**
 * [RollDieEffect] and the results-table recipe `Patterns.Mechanic.rollDie` (CR 706.3): the recipe
 * is a roll followed by one condition-gated row per printed range, the conditions read the stored
 * result exactly as CR 706.3a phrases a row, and the whole thing is serializable data.
 */
class RollDieDslTest : FunSpec({

    val result = DynamicAmount.VariableReference(DIE_ROLL_RESULT)
    fun cmp(op: ComparisonOperator, n: Int) = Compare(result, op, DynamicAmount.Fixed(n))

    test("RollDie stores under the default key and rejects a die with no faces (CR 706.1a)") {
        val roll = Effects.RollDie(20).shouldBeInstanceOf<RollDieEffect>()
        roll.sides shouldBe 20
        roll.storeResultAs shouldBe DIE_ROLL_RESULT
        roll.modifier shouldBe null
        roll.description shouldBe "Roll a d20"
        Effects.RollDie(6).description shouldBe "Roll a six-sided die"
        shouldThrow<IllegalArgumentException> { RollDieEffect(0) }
    }

    test("a results table is a roll then one gated row per range, in printed order") {
        val table = Patterns.Mechanic.rollDie(
            20,
            1..9 to Effects.DrawCards(1),
            10..19 to Effects.DrawCards(2),
            20..20 to Effects.DrawCards(3),
        )
        table.effects.size shouldBe 4
        table.effects[0] shouldBe RollDieEffect(20)

        val rows = table.effects.drop(1).map { it.shouldBeInstanceOf<GatedEffect>() }
        rows.map { (it.gate as Gate.WhenCondition).condition } shouldBe listOf(
            AllConditions(listOf(cmp(ComparisonOperator.GTE, 1), cmp(ComparisonOperator.LTE, 9))),
            AllConditions(listOf(cmp(ComparisonOperator.GTE, 10), cmp(ComparisonOperator.LTE, 19))),
            cmp(ComparisonOperator.EQ, 20),
        )
        rows.map { it.then } shouldBe listOf(Effects.DrawCards(1), Effects.DrawCards(2), Effects.DrawCards(3))
        rows[0].description shouldBe "1—9 | ${Effects.DrawCards(1).description}"
        rows[2].description shouldBe "20 | ${Effects.DrawCards(3).description}"
    }

    test("open-ended rows: 'N+' is GTE and 'N or less' is LTE (CR 706.3a)") {
        val table = Patterns.Mechanic.rollDie(
            20,
            Int.MIN_VALUE..0 to Effects.DrawCards(1),
            10..Int.MAX_VALUE to Effects.DrawCards(2),
            storeResultAs = "deck",
            modifier = DynamicAmount.Fixed(-3),
        )
        table.effects[0] shouldBe RollDieEffect(20, "deck", DynamicAmount.Fixed(-3))
        val deck = DynamicAmount.VariableReference("deck")
        val conditions = table.effects.drop(1).map { ((it as GatedEffect).gate as Gate.WhenCondition).condition }
        conditions shouldBe listOf(
            Compare(deck, ComparisonOperator.LTE, DynamicAmount.Fixed(0)),
            Compare(deck, ComparisonOperator.GTE, DynamicAmount.Fixed(10)),
        )
        table.effects[1].description shouldBe "0 or less | ${Effects.DrawCards(1).description}"
        table.effects[2].description shouldBe "10+ | ${Effects.DrawCards(2).description}"
    }

    test("overlapping or empty rows are rejected — a results table's rows are disjoint") {
        shouldThrow<IllegalArgumentException> {
            Patterns.Mechanic.rollDie(20, 1..10 to Effects.DrawCards(1), 10..20 to Effects.DrawCards(2))
        }
        shouldThrow<IllegalArgumentException> {
            Patterns.Mechanic.rollDie(20, 5..1 to Effects.DrawCards(1))
        }
    }

    test("a results table round-trips through serialization") {
        val table: Effect = Patterns.Mechanic.rollDie(
            6,
            1..2 to Effects.DrawCards(1),
            3..3 to Effects.GainLife(2),
            4..6 to Effects.LoseLife(1),
            modifier = DynamicAmount.Fixed(1),
        )
        Json.decodeFromString<Effect>(Json.encodeToString(table)) shouldBe table
    }
})
