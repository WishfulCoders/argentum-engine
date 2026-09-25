package com.wingedsheep.gym

import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/** One player's terminal reward: +1 win, -1 loss, 0 draw. */
@Serializable
data class PlayerReward(val playerId: EntityId, val value: Double)

/**
 * Episode bookkeeping that travels beside an observation, never inside it.
 *
 * A `TrainingObservation` is an information set: what one player may know about the position. How
 * many actions an episode has taken, whether it was cut short and what it paid out are facts about
 * the *episode*, not about the position, and a learner needs them on every step. Keeping them here
 * also keeps the observation's schema hash — and therefore every fitted checkpoint — untouched
 * while the trainer's contract grows (`docs/43` §3.5-§3.6 in the training repository).
 *
 * [reward] is empty until the game ends naturally. A [truncated] episode pays nothing: it is not a
 * win, not a loss, and must not be mixed into either.
 */
@Serializable
data class EnvStatus(
    /** The game ended under its own rules — someone won, drew, or decked out. */
    val terminated: Boolean,

    /** The env cut the episode short at one of its [com.wingedsheep.gym.service.EnvLimits]. */
    val truncated: Boolean = false,

    /** Which limit stopped it, in the arena's vocabulary (`maxTurns(50)`, `stuck(turn=8,step=DRAW)`). */
    val truncationReason: String? = null,

    /** Actions submitted since the last reset. */
    val stepCount: Int = 0,

    /** The engine's turn counter, counting both seats' turns. */
    val turnNumber: Int = 0,

    /** The seed this game was initialized with; null for envs that have no game RNG. */
    val seed: Long? = null,

    /** Terminal rewards per player; empty while the episode is running or when it was truncated. */
    val reward: List<PlayerReward> = emptyList(),

    /**
     * Engine actions the env's own pilot seats have taken this episode. Cumulative, like
     * [stepCount], so differencing two statuses gives what one call cost.
     */
    val autoAdvanced: Int = 0,

    /**
     * Structured pending decisions answered for a learner seat by its `decisionProfile` AI, since
     * the last reset. This is the part of the learner's behaviour it is not learning, so it is
     * counted rather than hidden.
     */
    val delegatedDecisions: Int = 0,

    /**
     * Priority actions a `playout` took for a learner seat since the last reset. These are the
     * learner's decisions made by its own AI rather than by the caller, so an episode that mixes
     * stepped and played-out decisions says how many of each it holds instead of blending them.
     */
    val playedOut: Int = 0,

    /**
     * The seat the current observation was built for. It is the perspective seat except in a
     * learner-versus-learner env, where it is whichever learner has the decision
     * (mtg-draft-ai `docs/51` §4). A trainer maps it to the policy playing that seat.
     */
    val actingSeat: Int? = null,
) {
    /** True once the episode is over for either reason. A caller should reset or dispose it. */
    val done: Boolean get() = terminated || truncated
}
