package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GraveyardCardsHaveEscape
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Single source of truth for "can this card escape, and at what cost?" — used by the
 * cast-from-graveyard enumerator, the cast zone resolver, the cost totaller and the additional-cost
 * collector, so every read site agrees.
 *
 * Escape (CR 702.138a) is a static ability that functions while the card is in its owner's
 * graveyard: "You may cast this card from your graveyard by paying [cost] rather than paying its
 * mana cost." A card can have it from two sources:
 *  1. **Printed** on the card ([KeywordAbility.Escape] — Uro, Phlage, Ox of Agonas).
 *  2. **Whole-graveyard group grant** from a battlefield static ([GraveyardCardsHaveEscape],
 *     Underworld Breach: "Each nonland card in your graveyard has escape. The escape cost is equal
 *     to the card's mana cost plus exile three other cards from your graveyard."), controlled by
 *     the player whose graveyard holds the card (CR 109.5 — "your graveyard" is the granter's
 *     controller's). A card with no mana cost gets an unpayable escape cost from a mana-cost-based
 *     grant (CR 118.6; Underworld Breach ruling), so no option is synthesized for it.
 *
 * When more than one escape ability applies, the caster chooses which to apply (CR 601.2b;
 * Underworld Breach ruling 2020-01-24 — "If a card has multiple abilities giving you permission to
 * cast it, such as two escape abilities … you choose which one to apply"). [escapeOptions] lists
 * them in a stable order (printed first, then grants in battlefield order, duplicates collapsed —
 * two Breaches grant the same ability) and `CastSpell.escapeChoice` indexes into it. Whichever one
 * is applied, the spell was cast "with an escape ability", so it escaped (CR 702.138b).
 */
object EscapeCasts {

    /** The printed escape keyword on [cardDef], or null when it has none. */
    fun printedEscape(cardDef: CardDefinition?): KeywordAbility.Escape? =
        cardDef?.keywordAbilities?.filterIsInstance<KeywordAbility.Escape>()?.firstOrNull()

    /**
     * Every distinct escape ability [cardId] currently has, printed first. Group grants are only
     * consulted when [controllerId] / [cardRegistry] / [predicateEvaluator] are supplied; without
     * them only the printed escape is reported.
     */
    fun escapeOptions(
        state: GameState,
        cardId: EntityId,
        cardDef: CardDefinition?,
        controllerId: EntityId? = null,
        cardRegistry: CardRegistry? = null,
        predicateEvaluator: PredicateEvaluator? = null,
    ): List<KeywordAbility.Escape> {
        val printed = printedEscape(cardDef)
        // A split card's halves are cast one at a time for that half's cost (Underworld Breach
        // ruling); the escape path casts the whole card, so a split card gets no granted escape
        // rather than one charged its combined mana cost (CR 709.4b).
        if (controllerId == null || cardRegistry == null || predicateEvaluator == null || cardDef?.isSplit == true) {
            return listOfNotNull(printed)
        }
        val granted = groupGrantEscapes(state, cardId, controllerId, cardRegistry, predicateEvaluator)
        if (granted.isEmpty()) return listOfNotNull(printed)
        return (listOfNotNull(printed) + granted).distinct()
    }

    /**
     * The escape ability a cast applies: option [choice] of [escapeOptions] (the first when [choice]
     * is null), or null when the card has no such escape — an out-of-range choice is rejected
     * rather than falling back to another option.
     */
    fun chosenEscape(
        state: GameState,
        cardId: EntityId,
        cardDef: CardDefinition?,
        choice: Int?,
        controllerId: EntityId? = null,
        cardRegistry: CardRegistry? = null,
        predicateEvaluator: PredicateEvaluator? = null,
    ): KeywordAbility.Escape? =
        escapeOptions(state, cardId, cardDef, controllerId, cardRegistry, predicateEvaluator)
            .getOrNull(choice ?: 0)

    /**
     * Scan [controllerId]'s battlefield for [GraveyardCardsHaveEscape] statics whose filter matches
     * [cardId], synthesizing a [KeywordAbility.Escape] for each — the grant's fixed cost, or the
     * card's own mana cost when the grant leaves it null. Mirrors `FlashbackGrants.groupGrantFlashback`.
     */
    private fun groupGrantEscapes(
        state: GameState,
        cardId: EntityId,
        controllerId: EntityId,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator,
    ): List<KeywordAbility.Escape> {
        val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: return emptyList()
        var result: MutableList<KeywordAbility.Escape>? = null
        val context = PredicateContext(controllerId = controllerId)
        // Controlled view, so the grant follows whoever controls the granter (CR 109.5).
        for (granterId in state.controlledBattlefield(controllerId)) {
            val def = state.getEntity(granterId)?.get<CardComponent>()
                ?.let { cardRegistry.getCard(it) } ?: continue
            for (ability in def.script.staticAbilities) {
                if (ability !is GraveyardCardsHaveEscape) continue
                // CR 118.6: "equal to the card's mana cost" on a card with no mana cost is unpayable.
                val manaCost = ability.cost
                    ?: cardComponent.manaCost.takeUnless { it.isEmpty() }
                    ?: continue
                if (!predicateEvaluator.matches(state, state.projectedState, cardId, ability.filter, context)) continue
                val escape = KeywordAbility.Escape(manaCost, ability.additionalCost)
                val list = result ?: mutableListOf<KeywordAbility.Escape>().also { result = it }
                list.add(escape)
            }
        }
        return result ?: emptyList()
    }
}
