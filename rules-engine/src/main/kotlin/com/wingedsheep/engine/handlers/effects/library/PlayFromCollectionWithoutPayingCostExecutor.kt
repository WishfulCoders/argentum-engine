package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.actions.land.PlayLandHandler
import com.wingedsheep.engine.handlers.actions.spell.CastSpellHandler
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.CastFromCollectionWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.PlayFromCollectionWithoutPayingCostEffect
import kotlin.reflect.KClass

class PlayFromCollectionWithoutPayingCostExecutor(
    castSpellHandlerProvider: () -> CastSpellHandler,
    private val playLandHandlerProvider: () -> PlayLandHandler,
    cardRegistry: CardRegistry,
    targetFinder: TargetFinder,
) : EffectExecutor<PlayFromCollectionWithoutPayingCostEffect> {
    override val effectType: KClass<PlayFromCollectionWithoutPayingCostEffect> =
        PlayFromCollectionWithoutPayingCostEffect::class
    private val castExecutor = CastFromCollectionWithoutPayingCostExecutor(
        castSpellHandlerProvider, cardRegistry, targetFinder
    )

    override fun execute(
        state: GameState,
        effect: PlayFromCollectionWithoutPayingCostEffect,
        context: EffectContext,
    ): EffectResult {
        val cardId = context.pipeline.storedCollections[effect.from]?.firstOrNull()
            ?: return EffectResult.success(state)
        val card = state.getEntity(cardId)?.get<CardComponent>() ?: return EffectResult.success(state)
        if (!card.typeLine.isLand) {
            return castExecutor.execute(state, CastFromCollectionWithoutPayingCostEffect(effect.from), context)
        }
        val (permissionId, grantedState) = CastFromCollectionWithoutPayingCostExecutor.grantFreeCast(
            state, cardId, context.controllerId, context.sourceId, withoutPayingCost = false
        )
        val result = playLandHandlerProvider().executeDuringResolution(
            grantedState.copy(priorityPlayerId = context.controllerId), PlayLand(context.controllerId, cardId)
        )
        return if (result.error == null) EffectResult.from(result) else EffectResult.success(
            CastFromCollectionWithoutPayingCostExecutor.revokeFreeCast(state, cardId, permissionId)
        )
    }

    companion object {
        /**
         * Could [playerId] play land card [cardId] right now through this executor — their turn, a
         * land play left, no lock? Asked against the same temporarily-granted permission the play
         * itself uses, and the granted state is thrown away, so the question has no side effects.
         */
        fun canPlayLandNow(
            state: GameState,
            playLandHandler: PlayLandHandler,
            playerId: com.wingedsheep.sdk.model.EntityId,
            sourceId: com.wingedsheep.sdk.model.EntityId?,
            cardId: com.wingedsheep.sdk.model.EntityId,
        ): Boolean {
            val (_, grantedState) = CastFromCollectionWithoutPayingCostExecutor.grantFreeCast(
                state, cardId, playerId, sourceId, withoutPayingCost = false
            )
            return playLandHandler.validateDuringResolution(grantedState, PlayLand(playerId, cardId)) == null
        }
    }
}
