package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.components.identity.CardComponent

/**
 * Applies a spell's "X can't be 0" floor ([com.wingedsheep.sdk.model.CardScript.minimumXValue]) to
 * its cast offers, the way [AdditionalManaForCountersOffer] applies its grant: once over every
 * enumerated action, because the floor belongs to the spell however it is cast — from hand, from
 * another zone, kicked or not.
 *
 * - A cast whose cost carries an {X} ([LegalAction.hasXCost] — the printed cost, or a kicker {X} on
 *   the kicked offer) gets [LegalAction.minX] raised to the floor, and is unaffordable when the
 *   caster can't reach it (`maxAffordableX` below the floor). The client's X picker and the AI's
 *   X choice both read `minX`.
 * - A cast without paying the mana cost of a spell with {X} in its mana cost may only announce
 *   X = 0 (CR 107.3b), which the floor forbids, so that offer is unaffordable.
 * - Everything else is untouched: an unkicked Thieving Skydiver has no X to floor.
 *
 * `CastValidator` enforces the same floor on whatever is submitted.
 */
internal object SpellMinimumXOffer {

    fun annotate(context: EnumerationContext, actions: List<LegalAction>): List<LegalAction> =
        actions.map { legal ->
            val cast = legal.action as? CastSpell ?: return@map legal
            if (!legal.hasXCost && !cast.useWithoutPayingManaCost) return@map legal
            val card = context.state.getEntity(cast.cardId)?.get<CardComponent>() ?: return@map legal
            val minimumX = context.cardRegistry.getCard(card)?.script?.minimumXValue ?: 0
            when {
                minimumX <= 0 -> legal
                cast.useWithoutPayingManaCost -> if (card.manaCost.hasX) legal.copy(affordable = false) else legal
                else -> legal.copy(
                    minX = maxOf(legal.minX, minimumX),
                    affordable = legal.affordable && (legal.maxAffordableX?.let { it >= minimumX } ?: true)
                )
            }
        }
}
