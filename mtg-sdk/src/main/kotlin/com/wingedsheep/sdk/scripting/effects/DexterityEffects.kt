package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * "Flip this card onto the battlefield from a height of at least one foot. If it turns over
 * completely at least once during the flip, … each permanent it touches." — the dexterity flip of
 * Chaos Orb (and Falling Star's "lands on").
 *
 * The physical act becomes a decision: the engine lays the battlefield out as a table of
 * card-sized rectangles, the flipping player answers with an aim point and a release strength,
 * and the engine rolls the scatter, the landing rotation and whether the card turned over. Every
 * permanent matching [filter] whose rectangle the landed card overlaps is stored under
 * [storeTouchedAs] — **only if the card turned over completely**, since every printed dexterity
 * flip makes its whole result conditional on that. A flip that doesn't turn over (or lands off the
 * table) stores an empty collection, so the downstream step ("destroy", "deal 3 damage to each")
 * reads it as "nothing touched".
 *
 * The flipped card is the effect's source; it is never on the table it lands on, so it never
 * touches itself. Whether the flip may happen at all ("If this artifact is on the battlefield")
 * is the card's own gate, composed around this step.
 *
 * Closest types: [FlipCoinEffect] produces a won/lost bit, not a set of permanents;
 * [GatherCardsEffect] fills a collection from a deterministic [CardSource] with no player input.
 * Neither can take a "physical landing" axis without changing its decision and event contract.
 *
 * @property storeTouchedAs Pipeline collection receiving the touched permanents
 * @property filter Which touched permanents count ("nontoken permanents")
 */
@SerialName("FlipOntoBattlefield")
@Serializable
data class FlipOntoBattlefieldEffect(
    val storeTouchedAs: String,
    val filter: GameObjectFilter = GameObjectFilter.Permanent,
) : Effect {
    override val description: String =
        "Flip this card onto the battlefield from a height of at least one foot"

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}
