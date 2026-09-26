package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyMillAmount
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Applies [ModifyMillAmount] replacement effects to an announced mill count (CR 701.13 / CR 616 —
 * the "mill N" instruction is modified by replacement effects that refer to the number of cards
 * milled, before any individual card moves). Called once at the mill announcement site
 * ([GatherCardsExecutor]'s `CardSource.TopOfLibrary(isMill = true)` branch), NOT per moved card,
 * so a paused-and-resumed mill pipeline never double-modifies.
 *
 * This is the mill twin of `DrawReplacementDispatcher.applyDrawAmountModifier`: it scans every
 * battlefield permanent's replacement effects, gates each by the [EventPattern.MillEvent]'s
 * `player` filter relative to [playerId] and the source's controller, checks `restrictions`, sums
 * the modifier, and clamps the result to `≥ 0`. A base mill of 0 is left untouched ("would mill
 * one or more cards" only fires when at least one card would be milled).
 */
object MillAmountModifier {

    fun apply(
        state: GameState,
        playerId: EntityId,
        originalCount: Int,
        predicateEvaluator: PredicateEvaluator
    ): Int {
        if (originalCount <= 0) return originalCount
        val conditionEvaluator = predicateEvaluator.conditions
        var adjusted = originalCount
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val replacementSource = container.get<ReplacementEffectSourceComponent>() ?: continue
            val sourceControllerId = container.get<ControllerComponent>()?.playerId

            for (effect in replacementSource.replacementEffects) {
                if (effect !is ModifyMillAmount) continue
                val millEvent = effect.appliesTo as? EventPattern.MillEvent ?: continue

                val matchesPlayer = when (millEvent.player) {
                    Player.Each -> true
                    Player.You -> sourceControllerId != null && playerId == sourceControllerId
                    Player.EachOpponent ->
                        sourceControllerId != null && playerId != sourceControllerId
                    else -> false
                }
                if (!matchesPlayer) continue

                val effectContext = EffectContext(
                    sourceId = entityId,
                    controllerId = playerId,
                )
                val restrictionsHold = effect.restrictions.all { restriction ->
                    conditionEvaluator.evaluate(state, restriction, effectContext)
                }
                if (!restrictionsHold) continue

                adjusted += effect.modifier
            }
        }
        return adjusted.coerceAtLeast(0)
    }
}
