package com.wingedsheep.engine.mechanics.cost.spell

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.AdditionalCost

/**
 * The value of X a cast announces (CR 107.3a, 601.2b) — the one number every cast-time reading of
 * X has to agree on: an X-driven target cap ("any number of target creatures", at most X of them),
 * a divided total ("X damage divided as you choose", CR 601.2d), a "mana value X or less" target
 * filter.
 *
 * It is the `{X}` of the mana cost when the action carries one. Otherwise, a spell whose additional
 * cost is "pay X life" ([AdditionalCost.PayXLife] — Vicious Rivalry, Fire Covenant) announces X as
 * the life it declares for that cost; such a card never also has an `{X}` (the two would share the
 * slot), and an undeclared payment is X = 0. Null for a spell with no X at all.
 *
 * The stack object carries the same value to resolution (`xValue ?: additionalCostPayXLifeAmount`),
 * so what is validated as the cast is announced is what resolves.
 */
object AnnouncedX {

    fun of(action: CastSpell, cardDef: CardDefinition?): Int? {
        action.xValue?.let { return it }
        val script = action.faceIndex?.let { cardDef?.cardFaces?.getOrNull(it)?.script } ?: cardDef?.script
        return if (paysXLife(script?.additionalCosts)) action.additionalCostPayment?.payXLifeAmount ?: 0 else null
    }

    /** Whether a spell's additional costs include "pay X life" — the cost that announces its X. */
    fun paysXLife(additionalCosts: List<AdditionalCost>?): Boolean =
        additionalCosts?.any { it is AdditionalCost.PayXLife } == true
}
