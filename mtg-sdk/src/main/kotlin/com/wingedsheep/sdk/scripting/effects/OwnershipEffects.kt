package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Ownership Change Effects (ante — CR 407.3)
// =============================================================================

/**
 * [newOwner] becomes the owner of [card] — "You own target card in the ante" (Darkpact), "that
 * player owns this card and you own the other exiled card" (Bronze Tablet).
 *
 * Only the ante cards can change a card's owner (CR 407.3), and they do it "for keeps": the game
 * keeps the change for the rest of the game, and the server settles it into the players' pools
 * or spoils when the game ends. The card does not move — it stays the same object in the same
 * zone, refiled under its new owner — so a following step can move it as the new owner's card
 * ("…then exchange that card with the top card of your library").
 *
 * Closest existing types: [GainControlEffect] / [ExchangeControlEffect] change *control*, a
 * Layer 2 continuous effect on a permanent that lasts a duration; ownership is not a layer, isn't
 * limited to permanents (Darkpact's card is in the ante), and never reverts.
 *
 * @property card The card whose owner changes
 * @property newOwner The player who becomes its owner (default: the effect's controller)
 */
@SerialName("ChangeOwnership")
@Serializable
data class ChangeOwnershipEffect(
    val card: EffectTarget = EffectTarget.ContextTarget(0),
    val newOwner: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String = when (newOwner) {
        EffectTarget.Controller -> "you own ${card.description}"
        else -> "${newOwner.description} owns ${card.description}"
    }
}
