package com.wingedsheep.gym

import com.wingedsheep.ai.engine.AIPlayer
import com.wingedsheep.ai.engine.AiProfiles
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.gym.contract.ActionParameterizer
import com.wingedsheep.gym.contract.ActionParams
import com.wingedsheep.gym.contract.ActionRegistry
import com.wingedsheep.gym.contract.ObservationBuilder
import com.wingedsheep.gym.contract.ObservationResult
import com.wingedsheep.gym.contract.ResolvedAction
import com.wingedsheep.gym.service.AgentSpec
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
    private var perspectivePlayerIndex: Int,
    private var defaultRevealAll: Boolean,
    private val observationBuilder: ObservationBuilder = ObservationBuilder(environment.cardRegistry),
    private var limits: EnvLimits = EnvLimits(),
    /**
     * Who acts for each seat, in the env's player order. No spec at all — the default — is the
     * original behaviour: every decision leaves the env.
     */
    private var agents: List<AgentSpec> = emptyList(),
) : GymEnv {

    @Volatile
    private var registry: ActionRegistry = ActionRegistry.EMPTY

    /** Non-null once a limit stopped the episode; see [EnvLimits]. */
    private var truncation: String? = null

    /** The active player and step count when it last changed, for the stuck detector. */
    private var lastActive: EntityId? = null
    private var lastProgress: Int = 0

    /** The engine's AI for each seat that needs one: every pilot, and every learner's decisions. */
    private val players = mutableMapOf<EntityId, AIPlayer>()

    /** Engine actions the pilots took during the most recent call, and delegated decisions so far. */
    private var autoAdvanced: Int = 0
    private var delegatedDecisions: Int = 0

    private fun agentAt(index: Int): AgentSpec = agents.getOrElse(index) { AgentSpec.Learner() }

    private fun isLearner(playerId: EntityId): Boolean {
        val index = environment.playerIds.indexOf(playerId)
        return index < 0 || agentAt(index) is AgentSpec.Learner
    }

    /**
     * The AI acting for [playerId] — a pilot seat's own profile, or a learner seat's
     * `decisionProfile`. Built once per episode; the Strategist keeps short-lived per-player
     * memory, so seats must not share an instance.
     */
    private fun aiFor(playerId: EntityId): AIPlayer = players.getOrPut(playerId) {
        val profile = when (val agent = agentAt(environment.playerIds.indexOf(playerId))) {
            is AgentSpec.Pilot -> agent.profile
            is AgentSpec.Learner -> agent.decisionProfile
        }
        AIPlayer.create(environment.cardRegistry, playerId, AiProfiles.parse(profile))
    }

    override val isTerminal: Boolean get() = environment.state.gameOver

    override fun observe(revealAll: Boolean?): ObservationResult =
        build(revealAll ?: defaultRevealAll)

    override fun step(actionId: Int, params: ActionParams): ObservationResult {
        check(truncation == null) { "Env was truncated (${truncation}); reset it before stepping" }
        executeResolved(registry.resolve(actionId), actionId, params)
        checkLimits()
        advanceToLearner()
        return build(defaultRevealAll)
    }

    override fun fork(): GymEnv =
        GameGymEnv(
            environment.fork(), perspectivePlayerIndex, defaultRevealAll, observationBuilder, limits,
            agents,
        ).also {
            it.truncation = truncation
            it.lastActive = lastActive
            it.lastProgress = lastProgress
            it.delegatedDecisions = delegatedDecisions
            it.build(defaultRevealAll)
        }

    override fun status(): EnvStatus = EnvStatus(
        terminated = isTerminal,
        truncated = truncation != null,
        truncationReason = truncation,
        stepCount = environment.stepCount,
        turnNumber = environment.turnNumber,
        seed = environment.seed,
        autoAdvanced = autoAdvanced,
        delegatedDecisions = delegatedDecisions,
        // A truncated episode pays nothing: its outcome was never decided.
        reward = if (isTerminal) {
            environment.terminalRewards().map { (playerId, value) -> PlayerReward(playerId, value) }
        } else {
            emptyList()
        },
    )

    // --- game-only operations (used by MultiEnvService via cast) -------------

    /**
     * Re-initialise the underlying game in place, optionally under a new episode contract.
     *
     * A trainer resets into a fresh deck pair and seed every episode; letting it change the seats'
     * agents and limits at the same time is what keeps the env's behaviour a function of the config
     * it was last given, rather than of the one it happened to be created with.
     */
    fun reset(
        gameConfig: GameConfig,
        limits: EnvLimits = this.limits,
        agents: List<AgentSpec> = this.agents,
        perspectivePlayerIndex: Int = this.perspectivePlayerIndex,
        revealAll: Boolean = this.defaultRevealAll,
    ): ObservationResult {
        this.limits = limits
        this.agents = agents
        // Resetting into a config whose perspective the env then ignored would observe the game
        // from the wrong seat — the exact mistake EnvConfig refuses at construction.
        this.perspectivePlayerIndex = perspectivePlayerIndex
        this.defaultRevealAll = revealAll
        environment.reset(gameConfig)
        truncation = null
        lastActive = environment.state.activePlayerId
        lastProgress = 0
        players.clear()
        delegatedDecisions = 0
        advanceToLearner()
        return build(defaultRevealAll)
    }

    /** Submit a raw `DecisionResponse` while paused on a complex decision. */
    /**
     * Play every seat the caller does not own until a learner has something to decide.
     *
     * This is what makes a `step` mean "advance to my next decision" rather than "advance one
     * engine action". A learner's own structured decisions are answered here too, by its
     * `decisionProfile` AI: the policy has no heads for them, and leaving them to the caller would
     * mean training against an action space the arena does not use.
     */
    private fun advanceToLearner() {
        autoAdvanced = 0
        // No agent spec at all is the original contract: the caller drives every seat and answers
        // every decision itself, through /envs/{id}/decision. Search callers rely on that.
        if (agents.isEmpty()) return
        while (truncation == null && !isTerminal) {
            val decision = environment.state.pendingDecision
            if (decision != null) {
                if (isLearner(decision.playerId)) delegatedDecisions++
                val response = aiFor(decision.playerId).respondToDecision(environment.state, decision)
                environment.step(SubmitDecision(decision.playerId, response))
                autoAdvanced++
                checkLimits()
                continue
            }
            val priority = environment.state.priorityPlayerId
            if (priority == null) {
                truncation = "noPriority(turn=${environment.turnNumber})"
                return
            }
            if (isLearner(priority)) return
            environment.step(aiFor(priority).chooseAction(environment.state))
            autoAdvanced++
            checkLimits()
        }
    }

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
