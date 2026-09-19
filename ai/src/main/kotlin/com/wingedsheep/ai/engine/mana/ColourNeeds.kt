package com.wingedsheep.ai.engine.mana

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.mana.LandManaColorInspector
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.AddDynamicManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfChoiceEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Which colours a player can already make, and which ones their own cards are asking for.
 *
 * The one thing no evaluator in this engine could see (mtg-draft-ai `docs/46`). It exists so the
 * two decisions whose whole content is colour — which basic a search takes, and which land to
 * drop — stop being decided by list order.
 *
 * **Own seat only.** Every field reads the player's own hand, and the AI is entitled to exactly
 * that; handing an opponent's [ColourNeeds] to anything would be the hidden-information cheat the
 * determinizer exists to remove.
 *
 * **No solver.** Availability is "could some permanent I control produce this colour", not "can I
 * pay this cost right now" — the distinction `BoardPresence.landSequencing` already documents
 * itself as making, and for its reason: a `ManaSolver` run per candidate is not affordable, and
 * ranking five basics against each other does not need one.
 */
class ColourNeeds private constructor(
    /**
     * Colours the player can already reach: anything a permanent they control could produce, plus
     * anything a land **in hand** could produce once played.
     *
     * The hand half is deliberate. A player holding a Forest does not need to fetch one, and the
     * question a fetch land asks is about the colours you are short of, not the ones that happen
     * not to be on the battlefield this second.
     */
    val available: Set<Color>,
    /** Coloured pips the cards in hand are asking for, counted with multiplicity: {B}{B} is two. */
    val handPips: Map<Color, Int>,
    /**
     * Coloured pips across the rest of the deck the player can legally see — battlefield and
     * graveyard.
     *
     * The library is left out on purpose even though a search decision does reveal it, because
     * this object is built once per decision and read from places that are not searches. What is
     * left is still the right shape of answer for "which colour is this deck actually built on":
     * a deck with a double-black card in the graveyard and a Swamp in play is a black deck.
     */
    val deckPips: Map<Color, Int>,
) {
    /**
     * How much a mana source producing [produces] is worth to this player right now, in `[0, 5)`.
     *
     * The ladder is the plain reading of what a fixing land is for, in the order a player would
     * say it out loud:
     *
     *  1. **It makes a colour I need and cannot make.** The whole point, and worth most.
     *  2. **How badly** — a card asking {B}{B} wants a Swamp more than one asking {B} does.
     *  3. **It makes a colour I cannot make at all** and something in the deck wants — the card
     *     may not be in hand yet, but the deck is built on it.
     *  4. **Otherwise, the colour my hand leans on**, then the one my deck leans on. This is the
     *     "no colour is missing, so take the one you have more of" case.
     *
     * Clause 3 is gated on the colour being *wanted* rather than merely absent, and that gate is
     * load-bearing. Ungated it outranks clause 4, so a red deck holding a {4}{R}{R} dragon and two
     * Mountains would fetch a Swamp — a colour it is missing and has no use for — over the third
     * red source it is actually short of. "Missing" is only interesting for a colour you play.
     *
     * Bounded on purpose. It is added to a card's existing rank, and a basic land must not
     * outrank a bomb in a decision that is choosing which *card* to keep — only other lands.
     */
    fun rank(produces: Set<Color>): Double {
        if (produces.isEmpty()) return 0.0
        val unmet = produces.filterNot { it in available }
        val unmetPips = unmet.maxOfOrNull { handPips[it] ?: 0 } ?: 0
        val pips = produces.maxOfOrNull { handPips[it] ?: 0 } ?: 0
        val deck = produces.maxOfOrNull { deckPips[it] ?: 0 } ?: 0

        var score = 0.0
        if (unmetPips > 0) score += FIXES_SOMETHING_HELD
        score += URGENCY * minOf(1.0, unmetPips / 2.0)
        if (unmet.any { (handPips[it] ?: 0) > 0 || (deckPips[it] ?: 0) > 0 }) score += NEW_COLOUR
        score += HAND_LEAN * minOf(1.0, pips / 3.0)
        score += DECK_LEAN * minOf(1.0, deck / 6.0)
        return score
    }

    companion object {
        private const val FIXES_SOMETHING_HELD = 3.0
        private const val URGENCY = 1.0
        private const val NEW_COLOUR = 0.5
        private const val HAND_LEAN = 0.25
        private const val DECK_LEAN = 0.1

        fun of(
            state: GameState,
            projected: ProjectedState,
            playerId: EntityId,
            cardRegistry: CardRegistry,
        ): ColourNeeds {
            val available = mutableSetOf<Color>()
            for (entityId in projected.getBattlefieldControlledBy(playerId)) {
                available += coloursProducedBy(state, projected, entityId, cardRegistry)
            }
            val hand = state.getZone(playerId, Zone.HAND)
            for (entityId in hand) {
                val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
                if (card.isLand) available += coloursProducedBy(state, projected, entityId, cardRegistry)
            }
            return ColourNeeds(
                available = available,
                handPips = pipsOf(state, hand),
                deckPips = pipsOf(
                    state,
                    projected.getBattlefieldControlledBy(playerId) + state.getZone(playerId, Zone.GRAVEYARD),
                ),
            )
        }

        /**
         * The colours [entityId] could produce, wherever it currently sits.
         *
         * Lands go through the engine's own [LandManaColorInspector], which already knows about
         * basic-land subtypes, granted abilities and abilities that have been turned off. Non-lands
         * are read off the card definition's mana abilities, which is the same question one level
         * simpler — a mana creature or a rock has no intrinsic subtype ability to reconcile.
         */
        fun coloursProducedBy(
            state: GameState,
            projected: ProjectedState,
            entityId: EntityId,
            cardRegistry: CardRegistry,
        ): Set<Color> {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: return emptySet()
            if (card.isLand) {
                return LandManaColorInspector.colorsLandCouldProduce(state, projected, entityId, cardRegistry)
            }
            val definition = cardRegistry.getCard(card.cardDefinitionId) ?: return emptySet()
            val colours = mutableSetOf<Color>()
            for (ability in definition.script.activatedAbilities) {
                if (ability.isManaAbility) collectColours(ability.effect, colours)
            }
            return colours
        }

        private fun collectColours(effect: Effect, out: MutableSet<Color>) {
            when (effect) {
                is AddManaEffect -> out += effect.color
                is AddDynamicManaEffect -> out += effect.allowedColors
                is AddManaOfChoiceEffect -> when (val set = effect.colorSet) {
                    is ManaColorSet.AnyColor -> out += Color.entries
                    is ManaColorSet.Specific -> out += set.colors
                    // Everything else needs game state this loop does not have. Reporting nothing
                    // is the conservative direction: it can make the AI fetch a colour it already
                    // had, never make it decline one it needs.
                    else -> Unit
                }
                is CompositeEffect -> effect.effects.forEach { collectColours(it, out) }
                else -> Unit
            }
        }

        /** Coloured pips in the mana costs of the non-land cards among [entityIds]. */
        private fun pipsOf(state: GameState, entityIds: Iterable<EntityId>): Map<Color, Int> {
            val pips = mutableMapOf<Color, Int>()
            for (entityId in entityIds) {
                val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
                if (card.isLand) continue
                for (color in Color.entries) {
                    val count = card.manaCost.colorCount[color] ?: 0
                    if (count > 0) pips[color] = (pips[color] ?: 0) + count
                }
            }
            return pips
        }
    }
}
