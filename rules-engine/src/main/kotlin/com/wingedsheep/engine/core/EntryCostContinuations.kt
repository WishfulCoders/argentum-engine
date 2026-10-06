package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect
import kotlinx.serialization.Serializable

/**
 * Engine work parked *beneath* a [CostPaymentContinuation] that needs to know how the payment
 * ended — the "consumer whose follow-up is engine logic" shape
 * [com.wingedsheep.engine.mechanics.cost.CostPaymentContext] describes, for a follow-up that
 * branches on paid vs. not paid rather than running only on one side.
 *
 * The cost-payment resumer records the outcome on the frame directly beneath it ([settled]) before
 * it hands control back, so the frame's auto-resumer only ever runs with [paid] set.
 */
@Serializable
sealed interface AwaitsCostOutcome : AutomaticContinuation {
    /** `null` while the payment above is still open; then whether the cost was paid. */
    val paid: Boolean?
    fun settled(paid: Boolean): AwaitsCostOutcome
}

/**
 * A resolving permanent spell whose entry is replaced by
 * [com.wingedsheep.sdk.scripting.EntersOnlyIfCostPaid] (Mox Diamond): once the cost is settled, a
 * paid cost lets the remaining entry run, an unpaid one puts the card into its owner's graveyard.
 */
@Serializable
data class SpellEntryCostContinuation(
    val spellId: EntityId,
    override val paid: Boolean? = null,
) : AwaitsCostOutcome {
    override fun settled(paid: Boolean): AwaitsCostOutcome = copy(paid = paid)
}

/**
 * An effect putting [entityId] onto the battlefield (a reanimation, Show and Tell, a library
 * search) whose entry is replaced by [com.wingedsheep.sdk.scripting.EntersOnlyIfCostPaid]. Once the
 * cost is settled, the outcome is recorded on [context] and [effect] is replayed — the same
 * prepare-then-replay shape as [EffectEntryChoiceContinuation].
 */
@Serializable
data class EffectEntryCostContinuation(
    val effect: Effect,
    val context: EffectContext,
    val entityId: EntityId,
    override val paid: Boolean? = null,
) : AwaitsCostOutcome {
    override fun settled(paid: Boolean): AwaitsCostOutcome = copy(paid = paid)
}
