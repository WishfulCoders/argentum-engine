package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.GrantsDiscardImmunityComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * "Spells and abilities your opponents control can't cause you to discard cards" (Tamiyo,
 * Collector of Tales) — the single read point for
 * [com.wingedsheep.sdk.scripting.OpponentsCantMakeYouDiscard], the discard twin of
 * [SacrificeImmunity].
 *
 * A "can't" beats the instruction (CR 101.2): a protected player's discard simply doesn't happen
 * and an optional discard offered by such a source can't be chosen. The effect discard sites —
 * pipeline discards (`MoveCollectionEffect` with `MoveType.Discard`), "unless you discard" punisher
 * choices and ward—discard costs — consult [appliesTo]; discard *costs* the player pays for their own
 * spells and abilities and the cleanup-step hand-size discard (CR 514.1, a game rule, not a spell or
 * ability — the Tamiyo ruling) never do.
 *
 * Scoped to *opponents*, team-aware through [GameState.getOpponents].
 */
object DiscardImmunity {

    /**
     * True when [discardingPlayerId] can't be made to discard by a spell or ability controlled by
     * [effectControllerId] — read it as `context.effectControllerId ?: context.controllerId`, so a
     * per-player iteration still reports the overall controller. A null controller is not an
     * opponent.
     */
    fun appliesTo(
        state: GameState,
        discardingPlayerId: EntityId,
        effectControllerId: EntityId?,
        predicateEvaluator: PredicateEvaluator
    ): Boolean {
        if (effectControllerId == null || effectControllerId == discardingPlayerId) return false
        if (effectControllerId !in state.getOpponents(discardingPlayerId)) return false
        return ControllerGrants.grantedTo<GrantsDiscardImmunityComponent>(state, discardingPlayerId, predicateEvaluator = predicateEvaluator)
    }
}
