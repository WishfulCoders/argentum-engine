package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Utility functions for finding entities on the battlefield matching filters.
 *
 * Always uses projected state to account for type-changing, color-changing,
 * and control-changing continuous effects.
 */
object BattlefieldFilterUtils {

    /**
     * Find all entities on the battlefield matching the given filter using projected state.
     *
     * This is the canonical way to filter battlefield permanents — it ensures projected state
     * is always used (accounting for type-changing, color-changing, and control-changing effects).
     *
     * @param excludeSelfId If non-null, excludes this entity from results (for GroupFilter.excludeSelf).
     */
    fun findMatchingOnBattlefield(
        state: GameState,
        filter: GameObjectFilter,
        context: PredicateContext,
        excludeSelfId: EntityId? = null,
        predicateEvaluator: PredicateEvaluator
    ): List<EntityId> {
        val projected = state.projectedState
        return state.getBattlefield().filter { entityId ->
            if (excludeSelfId != null && entityId == excludeSelfId) return@filter false
            predicateEvaluator.matches(state, projected, entityId, filter, context)
        }
    }

    /**
     * Convenience overload that builds a [PredicateContext] from an [EffectContext].
     */
    fun findMatchingOnBattlefield(
        state: GameState,
        filter: GameObjectFilter,
        context: EffectContext,
        excludeSelfId: EntityId? = null,
        predicateEvaluator: PredicateEvaluator
    ): List<EntityId> {
        return findMatchingOnBattlefield(state, filter, PredicateContext.fromEffectContext(context), excludeSelfId, predicateEvaluator = predicateEvaluator)
    }
}
