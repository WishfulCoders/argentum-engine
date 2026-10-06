package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Default pipeline `storedNumbers` key a [RollDieEffect] writes its result to. */
const val DIE_ROLL_RESULT = "dieRoll"

/**
 * Roll one [sides]-sided die (CR 706.1) and publish the **result** — the natural result plus any
 * [modifier] printed in the same instruction (CR 706.2) — under [storeResultAs] in the pipeline's
 * `storedNumbers`.
 *
 * This is deliberately the whole primitive: it rolls and reports, and nothing else. What a card
 * then *does* with the number composes from existing vocabulary:
 *
 * - **A results table** (CR 706.3 — the AFR d20 cards, Comet's 1–2 / 3 / 4–5 / 6) is one
 *   `Gate.WhenCondition` per row over `VariableReference(storeResultAs)`. CR 706.3a reads each row
 *   as "If the result was in this range, [effect]", which is exactly a conditional on the stored
 *   number, so a results table needs no node of its own. Authors write it through
 *   [com.wingedsheep.sdk.dsl.MechanicPatterns.rollDie], which builds those rows from Kotlin ranges.
 *   A result that falls in no row (a modifier pushed it below 1, say) does nothing, as CR 706.3a
 *   requires.
 * - **A result used as a number** (CR 706.4 — "deals damage equal to the result") reads
 *   `DynamicAmount.VariableReference(storeResultAs)` directly.
 * - **"Roll again"** (CR 706.3c) is the same pattern nested inside a row.
 *
 * The die is rolled through the game's seeded RNG (`GameState.nextRandom`), so a game replayed from
 * the same seed rolls the same numbers. Every roll emits a die-rolled event carrying both the
 * natural result and the result, which is what a future "whenever you roll a die" / "whenever you
 * roll a natural 20" trigger matches on.
 *
 * Not modelled yet (no card in the corpus needs them): rolling several dice in one instruction,
 * modifiers from *other* sources (CR 706.2), rerolls (CR 706.2b), ignored rolls (CR 706.6), and stored
 * results (CR 706.8).
 *
 * @property sides The number of faces — `6` for "a six-sided die", `20` for "a d20". Must be ≥ 1
 *   (CR 706.1a: N is a positive integer).
 * @property storeResultAs Pipeline `storedNumbers` key the result is written to.
 * @property modifier A modifier printed in the rolling instruction itself, added to the natural
 *   result (CR 706.2) — e.g. The Deck of Many Things' "roll a d20 and subtract the number of cards
 *   in your hand" is a negated hand count. `null` means no modifier.
 */
@SerialName("RollDie")
@Serializable
data class RollDieEffect(
    val sides: Int,
    val storeResultAs: String = DIE_ROLL_RESULT,
    val modifier: DynamicAmount? = null
) : Effect {
    init {
        require(sides >= 1) { "RollDieEffect.sides must be at least 1, was $sides" }
    }

    override val description: String = buildString {
        append(if (sides == 6) "Roll a six-sided die" else "Roll a d$sides")
        if (modifier != null) append(" and add ${modifier.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newModifier = modifier?.applyTextReplacement(replacer)
        return if (newModifier !== modifier) copy(modifier = newModifier) else this
    }
}
