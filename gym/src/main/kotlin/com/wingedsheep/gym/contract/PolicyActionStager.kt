package com.wingedsheep.gym.contract

import com.wingedsheep.ai.engine.GameSimulator
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.state.GameState

/** Preflights a policy action and emits any Treasure mana activations before the chosen action. */
class PolicyActionStager(private val simulator: GameSimulator) {
    data class Submission(val action: GameAction, val original: GameAction, val staged: Boolean)

    private var pending: Pair<List<GameAction>, GameAction>? = null

    /** Return null when neither the direct action nor a bounded mana-float plan is accepted. */
    fun begin(state: GameState, action: GameAction): Submission? {
        pending = null
        if (simulator.accepts(state, action)) return Submission(action, action, staged = false)
        val mana = simulator.floatSacrificeMana(state, action) ?: return null
        pending = mana.activations.drop(1) to action
        return Submission(mana.activations.first(), action, staged = true)
    }

    /** Recheck each queued action against the state after the preceding submission. */
    fun continuePending(state: GameState): Submission? {
        val (activations, original) = pending ?: return null
        pending = null
        val next = activations.firstOrNull() ?: original
        if (!simulator.accepts(state, next)) return null
        if (activations.isNotEmpty()) pending = activations.drop(1) to original
        return Submission(next, original, staged = true)
    }
}
