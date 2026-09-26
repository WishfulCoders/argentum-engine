package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.values.DynamicAmount

// =============================================================================
// Arithmetic on DynamicAmount
// =============================================================================
//
// Card text does arithmetic on the numbers it reads ("X plus one", "twice the number of …",
// "half your life, rounded up"). These operators spell it the way Kotlin spells arithmetic and
// lower to the same Add / Subtract / Multiply / Divide nodes the data model always had — the
// serialized tree is unchanged, so the operand order is the node's operand order:
//
// ```kotlin
// DynamicAmounts.xValue() + 1                // Add(XValue, Fixed(1))
// 7 - DynamicAmounts.count(Player.You, Zone.GRAVEYARD)
// DynamicAmounts.creaturesYouControl() * 2   // Multiply(…, 2)
// -DynamicAmounts.xValue()                   // Multiply(XValue, -1) — "gets -X/-X"
// DynamicAmounts.lifeTotal(Player.You) / 2   // half, rounded DOWN
// DynamicAmounts.lifeTotal(Player.You) divRoundedUp 2
// ```
//
// Division states its rounding: `/` rounds down, like `Int` division; `divRoundedUp` rounds up.
// Card text always says which ("rounded up" / "rounded down"), so pick the matching spelling.

/** `this + other` ([DynamicAmount.Add]). */
operator fun DynamicAmount.plus(other: DynamicAmount): DynamicAmount = DynamicAmount.Add(this, other)

/** `this + n` ([DynamicAmount.Add] with a constant right operand). */
operator fun DynamicAmount.plus(n: Int): DynamicAmount = DynamicAmount.Add(this, DynamicAmount.Fixed(n))

/** `n + amount` ([DynamicAmount.Add] with a constant left operand). */
operator fun Int.plus(amount: DynamicAmount): DynamicAmount = DynamicAmount.Add(DynamicAmount.Fixed(this), amount)

/** `this - other` ([DynamicAmount.Subtract]); may go negative — wrap in [DynamicAmounts.nonNegative] if it mustn't. */
operator fun DynamicAmount.minus(other: DynamicAmount): DynamicAmount = DynamicAmount.Subtract(this, other)

/** `this - n` ([DynamicAmount.Subtract] with a constant right operand). */
operator fun DynamicAmount.minus(n: Int): DynamicAmount = DynamicAmount.Subtract(this, DynamicAmount.Fixed(n))

/** `n - amount` ([DynamicAmount.Subtract] with a constant left operand — "seven minus …"). */
operator fun Int.minus(amount: DynamicAmount): DynamicAmount = DynamicAmount.Subtract(DynamicAmount.Fixed(this), amount)

/** `this * multiplier` ([DynamicAmount.Multiply]) — "twice", "three times". */
operator fun DynamicAmount.times(multiplier: Int): DynamicAmount = DynamicAmount.Multiply(this, multiplier)

/** `-this` ([DynamicAmount.Multiply] by -1) — the negative of an amount, for "gets -X/-X". */
operator fun DynamicAmount.unaryMinus(): DynamicAmount = DynamicAmount.Multiply(this, -1)

/** `this / divisor`, **rounded down** ([DynamicAmount.Divide] with `roundUp = false`). */
operator fun DynamicAmount.div(divisor: Int): DynamicAmount =
    DynamicAmount.Divide(this, DynamicAmount.Fixed(divisor), roundUp = false)

/** `this / divisor`, **rounded down**, with a dynamic divisor. */
operator fun DynamicAmount.div(divisor: DynamicAmount): DynamicAmount =
    DynamicAmount.Divide(this, divisor, roundUp = false)

/** `this` divided by [divisor], **rounded up** ([DynamicAmount.Divide] with `roundUp = true`). */
infix fun DynamicAmount.divRoundedUp(divisor: Int): DynamicAmount =
    DynamicAmount.Divide(this, DynamicAmount.Fixed(divisor), roundUp = true)

/** `this` divided by a dynamic [divisor], **rounded up**. */
infix fun DynamicAmount.divRoundedUp(divisor: DynamicAmount): DynamicAmount =
    DynamicAmount.Divide(this, divisor, roundUp = true)
