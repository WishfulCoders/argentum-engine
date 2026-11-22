package com.wingedsheep.engine.handlers.effects.linkedexile

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.LinkCollectionToPermanentEffect
import kotlin.reflect.KClass

/**
 * Executor for [LinkCollectionToPermanentEffect].
 *
 * Appends the collection's still-exiled cards to the target permanent's [LinkedExileComponent],
 * so that permanent's own leaves-the-battlefield trigger reads them through
 * `CardSource.FromLinkedExile` (the zone transition carries the pile into `departedLinkedExile`
 * when the permanent leaves). No-op when the permanent isn't on the battlefield.
 */
class LinkCollectionToPermanentExecutor : EffectExecutor<LinkCollectionToPermanentEffect> {

    override val effectType: KClass<LinkCollectionToPermanentEffect> = LinkCollectionToPermanentEffect::class

    override fun execute(
        state: GameState,
        effect: LinkCollectionToPermanentEffect,
        context: EffectContext
    ): EffectResult {
        val permanentId = TargetResolutionUtils.resolveTarget(effect.permanent, context, state)
            ?: return EffectResult.success(state)
        if (permanentId !in state.getBattlefield()) return EffectResult.success(state)

        val exiled = context.pipeline.storedCollections[effect.from].orEmpty().filter { cardId ->
            val ownerId = state.getEntity(cardId)?.get<CardComponent>()?.ownerId
            ownerId != null && cardId in state.getZone(ZoneKey(ownerId, Zone.EXILE))
        }
        if (exiled.isEmpty()) return EffectResult.success(state)

        val newState = state.updateEntity(permanentId) { container ->
            val existing = container.get<LinkedExileComponent>()?.exiledIds.orEmpty()
            container.with(LinkedExileComponent(existing + exiled.filterNot { it in existing }))
        }
        return EffectResult.success(newState)
    }
}
