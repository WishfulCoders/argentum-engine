package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.EffectEntryCostContinuation
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.copy.EffectCopyEntry
import com.wingedsheep.engine.mechanics.cost.CostPaymentService
import com.wingedsheep.engine.mechanics.cost.PaymentResult
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EntersOnlyIfCostPaid
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * [EntersOnlyIfCostPaid] ("If this would enter, you may [cost] instead. If you do, put it onto the
 * battlefield. If you don't, put it into its owner's graveyard." — Mox Diamond) for a card an
 * *effect* puts onto the battlefield: a reanimation, Show and Tell, Tinker, a library search.
 *
 * CR 614.12a: the choice is made before the permanent enters. Each entrant's cost is asked and paid
 * against the pre-entry state, the outcome is recorded on the [EffectContext]
 * ([EffectContext.entryCostsPaid]), and the move instruction is replayed — the prepare-then-replay
 * shape of [EffectEntryChoices]. On the replay an unpaid entrant is put into its owner's graveyard
 * instead of onto the battlefield ([declinedEntrants]), and a paid one moves with
 * [ZoneEntryOptions.entryCostPaid] so the transition service lets it in.
 *
 * The payment itself is the shared [CostPaymentService] rail, with an [EffectEntryCostContinuation]
 * parked beneath it; the payment resumer stamps that frame with the outcome.
 */
object EffectEntryCosts {

    /** The entry cost [entityId] would pay as it enters, read off the card that would enter. */
    fun costFor(state: GameState, entityId: EntityId, context: EffectContext, registry: CardRegistry) =
        enteringCard(state, entityId, context)
            ?.let { registry.getCard(it.cardDefinitionId) }
            ?.script?.replacementEffects
            ?.filterIsInstance<EntersOnlyIfCostPaid>()
            ?.firstOrNull()
            ?.cost

    /** The card as it would enter — the copied card when it enters as a copy (CR 614.12). */
    private fun enteringCard(state: GameState, entityId: EntityId, context: EffectContext): CardComponent? =
        context.entryCopies[entityId]
            ?.let { EffectCopyEntry.apply(state, entityId, it).getEntity(entityId)?.get<CardComponent>() }
            ?: state.getEntity(entityId)?.get<CardComponent>()

    /** The prepared context, or the pause asking an entrant's controller to pay. */
    data class Preparation(val pause: EffectResult?, val context: EffectContext)

    /**
     * Ask, in APNAP order of the players the entrants would enter under, each entrant that carries an
     * [EntersOnlyIfCostPaid] and hasn't been asked yet. An entrant whose cost can't be paid
     * (CR 118.3) — or that has no payment service to ask through — is recorded unpaid on the spot.
     */
    fun prepare(
        state: GameState,
        effect: Effect,
        context: EffectContext,
        entrants: Map<EntityId, EntityId>,
        registry: CardRegistry,
        costPaymentService: CostPaymentService?,
    ): Preparation {
        var prepared = context
        val playerOrder = state.apnapOrder.withIndex().associate { it.value to it.index }
        for ((id, controller) in entrants.entries.sortedBy { playerOrder[it.value] ?: Int.MAX_VALUE }) {
            if (id in prepared.entryCostsPaid) continue
            // Battlefield → battlefield is not an entry (the transition service doesn't re-enter it).
            if (id in state.getBattlefield()) continue
            val cost = costFor(state, id, prepared, registry) ?: continue
            if (costPaymentService == null) {
                prepared = answered(prepared, id, paid = false)
                continue
            }
            val framed = state.pushContinuation(EffectEntryCostContinuation(effect, prepared, id))
            when (val payment = costPaymentService.pay(framed, controller, cost, id)) {
                is PaymentResult.Pending -> return Preparation(
                    EffectResult.from(ExecutionResult.propagatePause(payment.state, payment.events)), prepared,
                )
                // Unpayable, or settled with no prompt: the cost wasn't paid this way.
                else -> prepared = answered(prepared, id, paid = false)
            }
        }
        return Preparation(null, prepared)
    }

    /** [context] with [entityId]'s entry cost settled. */
    fun answered(context: EffectContext, entityId: EntityId, paid: Boolean): EffectContext =
        context.copy(entryCostsPaid = context.entryCostsPaid + (entityId to paid))

    /** Entrants whose entry cost was not paid: they go to their owner's graveyard instead. */
    fun declinedEntrants(context: EffectContext): Set<EntityId> =
        context.entryCostsPaid.filterValues { !it }.keys
}
