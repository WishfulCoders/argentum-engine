package com.wingedsheep.gym

import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.gym.contract.ActionParameterizer
import com.wingedsheep.gym.contract.ActionParams
import com.wingedsheep.gym.contract.ActionRegistry
import com.wingedsheep.gym.contract.ObservationBuilder
import com.wingedsheep.gym.contract.ObservationResult
import com.wingedsheep.gym.contract.ResolvedAction
import com.wingedsheep.gym.service.EnvLimits
import com.wingedsheep.gym.service.SnapshotCodec
import com.wingedsheep.gym.service.SnapshotHandle
import com.wingedsheep.sdk.model.EntityId

/**
 * [GymEnv] adapter over a [GameEnvironment] — a game of Magic.
 *
 * Holds the per-env bookkeeping that used to live in `MultiEnvService.EnvEntry`
 * (perspective, default reveal flag, the live [ActionRegistry] from the last
 * observation) so the service layer can treat every env type the same. The
 * underlying [GameEnvironment] is left untouched, since the trainer SPI drives
 * it directly.
 */
class GameGymEnv(
    val environment: GameEnvironment,
    private val perspectivePlayerIndex: Int,
    private val defaultRevealAll: Boolean,
    private val observationBuilder: ObservationBuilder = ObservationBuilder(environment.cardRegistry),
    private val limits: EnvLimits = EnvLimits(),
) : GymEnv {

    @Volatile
    private var registry: ActionRegistry = ActionRegistry.EMPTY

    /** Non-null once a limit stopped the episode; see [EnvLimits]. */
    private var truncation: String? = null

    /** The active player and step count when it last changed, for the stuck detector. */
    private var lastActive: EntityId? = null
    private var lastProgress: Int = 0

    override val isTerminal: Boolean get() = environment.state.gameOver

    override fun observe(revealAll: Boolean?): ObservationResult =
        build(revealAll ?: defaultRevealAll)

    override fun step(actionId: Int, params: ActionParams): ObservationResult {
        check(truncation == null) { "Env was truncated (${truncation}); reset it before stepping" }
        executeResolved(registry.resolve(actionId), actionId, params)
        checkLimits()
        return build(defaultRevealAll)
    }

    override fun fork(): GymEnv =
        GameGymEnv(
            environment.fork(), perspectivePlayerIndex, defaultRevealAll, observationBuilder, limits,
        ).also {
            it.truncation = truncation
            it.lastActive = lastActive
            it.lastProgress = lastProgress
            it.build(defaultRevealAll)
        }

    override fun status(): EnvStatus = EnvStatus(
        terminated = isTerminal,
        truncated = truncation != null,
        truncationReason = truncation,
        stepCount = environment.stepCount,
        turnNumber = environment.turnNumber,
        seed = environment.seed,
        // A truncated episode pays nothing: its outcome was never decided.
        reward = if (isTerminal) {
            environment.terminalRewards().map { (playerId, value) -> PlayerReward(playerId, value) }
        } else {
            emptyList()
        },
    )

    // --- game-only operations (used by MultiEnvService via cast) -------------

    /** Re-initialise the underlying game in place. */
    fun reset(gameConfig: GameConfig): ObservationResult {
        environment.reset(gameConfig)
        truncation = null
        lastActive = environment.state.activePlayerId
        lastProgress = 0
        return build(defaultRevealAll)
    }

    /** Submit a raw `DecisionResponse` while paused on a complex decision. */
    fun submitDecision(response: DecisionResponse): ObservationResult {
        val pending = environment.state.pendingDecision
            ?: throw IllegalStateException("Env is not paused on a decision")
        check(response.decisionId == pending.id) {
            "Decision ID mismatch: response=${response.decisionId}, pending=${pending.id}"
        }
        environment.step(SubmitDecision(pending.playerId, response))
        return build(defaultRevealAll)
    }

    fun snapshot(codec: SnapshotCodec): SnapshotHandle =
        codec.save(state = environment.state, playerIds = environment.playerIds, stepCount = 0)

    fun restore(codec: SnapshotCodec, handle: SnapshotHandle): ObservationResult {
        val snap = codec.load(handle)
        environment.restore(snap.state, snap.playerIds, snap.stepCount)
        return build(defaultRevealAll)
    }

    // --- internals -----------------------------------------------------------

    /**
     * Decide whether this episode has run out of room. Progress is a change of active player, the
     * same definition the arena uses, so a turn that cannot be passed is caught long before the
     * action or turn caps are reached.
     */
    private fun checkLimits() {
        if (isTerminal) return
        val active = environment.state.activePlayerId
        if (active != lastActive) {
            lastActive = active
            lastProgress = environment.stepCount
        }
        val maxTurns = limits.maxTurnsPerSeat * environment.playerIds.size
        truncation = when {
            environment.turnNumber >= maxTurns -> "maxTurns(${limits.maxTurnsPerSeat})"
            environment.stepCount >= limits.maxActions -> "maxActions(${limits.maxActions})"
            environment.stepCount - lastProgress > limits.maxActionsWithoutProgress ->
                "stuck(turn=${environment.turnNumber},step=${environment.state.step.name})"
            else -> null
        }
    }

    private fun build(revealAll: Boolean): ObservationResult {
        val perspective = environment.playerIds.getOrNull(perspectivePlayerIndex)
            ?: throw IllegalStateException("Env has no player at index $perspectivePlayerIndex")
        val result = observationBuilder.build(
            environment.state, perspective, environment.legalActions(), revealAll
        )
        registry = result.registry
        return result
    }

    private fun executeResolved(resolved: ResolvedAction, actionId: Int, params: ActionParams) {
        when (resolved) {
            is ResolvedAction.Legal -> {
                // The enumerated action is a template for the action types that need a choice the
                // ID can't carry (attackers, blockers, targets, X); params complete it.
                val action = ActionParameterizer.apply(resolved.action, params, environment.state)
                environment.step(action)
                failOnRejection(actionId)
            }
            is ResolvedAction.Decision -> {
                require(params.isEmpty) {
                    "Action ID $actionId is a folded decision response and takes no step params"
                }
                val pending = environment.state.pendingDecision
                    ?: throw IllegalStateException("Registry has a decision response but env is not paused")
                environment.step(SubmitDecision(pending.playerId, resolved.response))
                failOnRejection(actionId)
            }
            ResolvedAction.Unknown ->
                throw IllegalArgumentException("Action ID $actionId is not valid for the current step")
        }
    }

    /**
     * An engine rejection leaves the state untouched, which would otherwise read as a successful
     * no-op — the exact way a mis-declared attack used to disappear. Applies to a submitted decision
     * for the same reason it applies to a played action: neither changes the state when refused.
     */
    private fun failOnRejection(actionId: Int) {
        environment.lastRejection?.let {
            throw IllegalArgumentException("Action $actionId rejected by the engine: $it")
        }
    }
}
