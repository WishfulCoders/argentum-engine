package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Who gets offered the copy of the chain spell.
 */
@Serializable
enum class CopyRecipient {
    /** Controller of the targeted permanent (Destroy, Bounce, PreventDamage) */
    TARGET_CONTROLLER,
    /** The target player directly (Discard — target IS a player) */
    TARGET_PLAYER,
    /** The "affected player" — the target if it's a player, or its controller if a permanent (Damage) */
    AFFECTED_PLAYER
}

/**
 * Unified chain copy effect for all "Chain of X" cards from Onslaught.
 *
 * Executes a primary action on the target, then offers a specific player the option
 * to copy the spell (optionally paying a cost) and choose a new target.
 *
 * The spell's name in prompts is read off the resolving card, so the effect never repeats it.
 *
 * @property action The primary effect to execute (any generic Effect)
 * @property target The target of the primary action
 * @property copyRecipient Who gets offered the copy
 * @property copyCost Cost the recipient pays, mid-resolution, before they may copy (null = free).
 *   The engine collects a sacrifice ("sacrifice a land", Chain of Vapor), a discard ("discard a
 *   card", Chain of Plasma) or a mana payment ("pay {R}{R}", Chain Lightning — paid through the
 *   resolution-time mana window, CR 605.3a). Any other cost is treated as unpayable, so the copy is
 *   never offered rather than offered for free.
 * @property copyTargetRequirement Target requirement for the copy's new target
 */
@SerialName("ChainCopy")
@Serializable
data class ChainCopyEffect(
    val action: Effect,
    val target: EffectTarget,
    val copyRecipient: CopyRecipient,
    val copyCost: PayCost? = null,
    val copyTargetRequirement: TargetRequirement
) : Effect {
    override val description: String = buildString {
        append(action.description)
        append(". Then ")
        when (copyRecipient) {
            CopyRecipient.TARGET_CONTROLLER -> append("that permanent's controller")
            CopyRecipient.TARGET_PLAYER -> append("that player")
            CopyRecipient.AFFECTED_PLAYER -> append("that player or that permanent's controller")
        }
        append(" may ")
        if (copyCost != null) {
            append("${costPhrase(copyCost)}. If the player does, they may ")
        }
        append("copy this spell and may choose a new target for that copy")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newAction = action.applyTextReplacement(replacer)
        val newCopyTargetReq = copyTargetRequirement.applyTextReplacement(replacer)
        val newCopyCost = copyCost?.applyTextReplacement(replacer)
        return if (newAction !== action || newCopyTargetReq !== copyTargetRequirement || newCopyCost !== copyCost)
            copy(
                action = newAction,
                copyTargetRequirement = newCopyTargetReq,
                copyCost = newCopyCost
            ) else this
    }

    companion object {
        /**
         * The imperative phrase for a chain copy's cost — "pay {R}{R}" for mana, whose bare
         * description is just the symbols, and the cost's own description ("discard a card",
         * "sacrifice a land") otherwise.
         */
        fun costPhrase(cost: PayCost): String =
            if ((cost as? PayCost.Atom)?.atom is CostAtom.Mana) "pay ${cost.description}" else cost.description
    }
}
