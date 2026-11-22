package com.wingedsheep.engine.handlers.effects.zones

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.OwnershipChangedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OriginalOwnerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.scripting.effects.ChangeOwnershipEffect
import kotlin.reflect.KClass

/**
 * Executor for [ChangeOwnershipEffect] — the ante cards' "you own that card" (CR 407.3).
 *
 * Ownership lives in three places that must agree: [OwnerComponent], [CardComponent.ownerId], and
 * the owner-keyed zone the card is filed under (exile, ante, graveyard, …). All three move
 * together; the card itself does not change zones, so it keeps its object identity. The first
 * owner is remembered in [OriginalOwnerComponent] so the server can settle the change when the
 * game ends — and forgotten if the card comes back to them.
 */
class ChangeOwnershipExecutor : EffectExecutor<ChangeOwnershipEffect> {

    override val effectType: KClass<ChangeOwnershipEffect> = ChangeOwnershipEffect::class

    override fun execute(
        state: GameState,
        effect: ChangeOwnershipEffect,
        context: EffectContext
    ): EffectResult {
        val cardId = context.resolveTarget(effect.card, state) ?: return EffectResult.success(state)
        val container = state.getEntity(cardId) ?: return EffectResult.success(state)
        val card = container.get<CardComponent>() ?: return EffectResult.success(state)
        val oldOwnerId = container.get<OwnerComponent>()?.playerId ?: card.ownerId
            ?: return EffectResult.success(state)
        val newOwnerId = context.resolvePlayerTarget(effect.newOwner, state)
            ?: return EffectResult.error(state, "No player to become the owner of ${card.name}")
        if (newOwnerId == oldOwnerId) return EffectResult.success(state)

        val originalOwnerId = container.get<OriginalOwnerComponent>()?.playerId ?: oldOwnerId
        val newState = state
            .updateEntity(cardId) { c ->
                val owned = c.with(OwnerComponent(newOwnerId)).with(card.copy(ownerId = newOwnerId))
                if (newOwnerId == originalOwnerId) owned.without<OriginalOwnerComponent>()
                else owned.with(OriginalOwnerComponent(originalOwnerId))
            }
            .refileUnderOwner(cardId, newOwnerId)

        val zone = state.logicalZone(cardId)?.zoneType
        val events = if (zone == null) emptyList() else listOf(
            OwnershipChangedEvent(cardId, card.name, zone, oldOwnerId, newOwnerId)
        )
        return EffectResult.success(newState, events)
    }
}
