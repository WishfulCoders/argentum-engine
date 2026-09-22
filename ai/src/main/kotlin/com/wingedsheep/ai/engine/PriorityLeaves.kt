package com.wingedsheep.ai.engine

import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.GameState

/**
 * One priority decision as the [Strategist] scored it: every candidate that reached scoring, the pass
 * first when there was one, and which of them it chose (mtg-draft-ai `docs/50`).
 *
 * @property evaluationState the position the candidates were simulated from (the sampled world
 *   under a determinizing profile, else the real state).
 * @property chosenIndex index into [leaves] of the choice; -1 only when there was no pass and no action.
 */
data class PriorityLeaves(
    val evaluationState: GameState,
    val leaves: List<PriorityLeaf>,
    val chosenIndex: Int,
)

/**
 * @property action the concrete action the AI would submit (targets, X and payment filled in).
 * @property state the quiet state the one-ply simulation reached.
 * @property rawScore the candidate evaluator's leaf score.
 * @property score what the decision compared: [rawScore] after the per-card timing adjustments and
 *   idle-mana bonuses for an action, the end-step-discounted threshold for the pass. The action-only
 *   correction is added to this, among actions, to pick which one.
 */
data class PriorityLeaf(
    val action: LegalAction,
    val state: GameState,
    val rawScore: Double,
    val score: Double,
    val isPass: Boolean,
)
