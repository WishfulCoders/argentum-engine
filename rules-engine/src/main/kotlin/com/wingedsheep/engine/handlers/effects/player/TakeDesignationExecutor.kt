package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.initiative.InitiativeService
import com.wingedsheep.engine.mechanics.monarch.MonarchService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.PlayerDesignation
import com.wingedsheep.sdk.scripting.effects.TakeDesignationEffect
import kotlin.reflect.KClass

/**
 * Resolves [TakeDesignationEffect] — "you become the monarch" / "you take the initiative", and the
 * inherent combat-damage steals (CR 725.2, CR 726.2), which are those same effects aimed at the
 * damaging creatures' controller. The title moves through [MonarchService.become] (CR 725.3) or
 * [InitiativeService.take] (CR 726.3), which own the two designations' one difference: retaking the
 * initiative still counts as taking it (CR 726.5).
 *
 * A player who has left the game can't take either; that resolves to nothing rather than handing a
 * title to a seat that is gone.
 */
class TakeDesignationExecutor : EffectExecutor<TakeDesignationEffect> {

    override val effectType: KClass<TakeDesignationEffect> = TakeDesignationEffect::class

    override fun execute(
        state: GameState,
        effect: TakeDesignationEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state)
        if (targetId !in state.activePlayers) return EffectResult.success(state)

        // The inherent steals have the player entity as their source, so they have no card name.
        val cardName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        val (newState, events) = when (effect.designation) {
            PlayerDesignation.MONARCH ->
                MonarchService.become(state, targetId, cardName ?: MonarchService.SOURCE_NAME)
            PlayerDesignation.INITIATIVE ->
                InitiativeService.take(state, targetId, cardName ?: InitiativeService.SOURCE_NAME)
        }
        return EffectResult.success(newState, events)
    }
}
