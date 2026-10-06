package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.HandLookedAtEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.LookAtCollectionEffect
import com.wingedsheep.sdk.scripting.effects.LookAudience
import kotlin.reflect.KClass

/**
 * Executor for [LookAtCollectionEffect] — "look at [those cards]" (CR 701.20e: a look follows the
 * rules for revealing, except the cards are shown only to the specified player).
 *
 * Each card in the collection is marked revealed to the audience (and to whoever controls an
 * audience member's turn) via [LibraryRevealUtils.markRevealed], so the client shows its face to
 * them and to no one else. A card in another player's hand also produces a [HandLookedAtEvent]
 * (one per viewer per hand) carrying only the cards looked at, which the event transformer shows
 * to the viewer and withholds from everyone else — the hand's owner included (CR 402.3: nobody
 * else may look at a hand). Cards already in the viewer's own hand are not shown again.
 *
 * Does not move or modify any card; an empty collection is a silent no-op.
 */
class LookAtCollectionExecutor : EffectExecutor<LookAtCollectionEffect> {

    override val effectType: KClass<LookAtCollectionEffect> = LookAtCollectionEffect::class

    override fun execute(
        state: GameState,
        effect: LookAtCollectionEffect,
        context: EffectContext
    ): EffectResult {
        val cards = context.pipeline.storedCollections[effect.from]
            ?: return EffectResult.error(state, "No collection named '${effect.from}' in storedCollections")
        if (cards.isEmpty()) return EffectResult.success(state)

        val viewers: List<EntityId> = when (effect.audience) {
            LookAudience.Controller -> listOf(context.controllerId)
            LookAudience.Opponent -> state.getOpponents(context.controllerId)
            LookAudience.None -> emptyList()
        }

        val handOwnerOf: Map<EntityId, EntityId?> = cards.associateWith { cardId ->
            state.turnOrder.firstOrNull { cardId in state.getHand(it) }
        }

        var newState = state
        val events = mutableListOf<GameEvent>()
        for (viewer in viewers) {
            // The viewer already sees their own hand (CR 402.3); everything else is news to them.
            val shown = cards.filter { handOwnerOf[it] != viewer }
            if (shown.isEmpty()) continue
            newState = LibraryRevealUtils.markRevealed(newState, shown, listOf(viewer))
            val identityViewers = setOf(state.actorFor(viewer)) - viewer
            shown.filter { handOwnerOf[it] != null }
                .groupBy { handOwnerOf.getValue(it)!! }
                .forEach { (owner, inHand) ->
                    events += HandLookedAtEvent(viewer, owner, inHand, identityViewers)
                }
        }
        return EffectResult.success(newState, events)
    }
}
