package com.wingedsheep.engine.core

import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * The rest of a step in which no player receives priority, parked beneath a choice that one of the
 * step's turn-based actions asked: a "may not untap" choice in the untap step (CR 502.3). Once the
 * choice is answered, the auto-resumer advances the game out of that step, exactly as the unpaused
 * path does. (The cleanup step parks [FinishCleanupStepContinuation] instead; a state saved with this
 * frame beneath a cleanup discard resumes into another cleanup step, which ends the same way.)
 */
@Serializable
data object AdvanceStepContinuation : AutomaticContinuation

/**
 * The rest of a cleanup step, parked beneath the hand-size discard (CR 514.1) its turn-based actions
 * asked for. Once the discard is answered (its resumer also removes marked damage, CR 514.2), the
 * auto-resumer finishes the step: the active player gets priority if a triggered ability is waiting
 * (CR 514.3a), and otherwise the turn ends. See `TurnManager.finishCleanupStep`.
 */
@Serializable
data object FinishCleanupStepContinuation : AutomaticContinuation

/**
 * The rest of starting [activePlayerId]'s turn, parked beneath a choice asked by its untap step.
 * Once the untap step is over, "until your next turn" effects, goad designations, and effects
 * that last until that player's next untap step end, then the game advances to the upkeep step.
 * See `TurnManager.finishUntapStep`.
 */
@Serializable
data class FinishUntapStepContinuation(
    val activePlayerId: EntityId,
    val skippedUntapStep: Set<EntityId>,
    val pendingSkipsToConsume: Set<EntityId>,
) : AutomaticContinuation

/** Select the replacement for a team's untap: at most one pending skip is spent. */
@Serializable
data class UntapStepSkipChoiceContinuation(val pendingPlayers: List<EntityId>) : AnswerContinuation
