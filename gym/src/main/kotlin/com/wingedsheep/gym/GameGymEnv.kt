package com.wingedsheep.gym

import com.wingedsheep.ai.engine.AIPlayer
import com.wingedsheep.ai.engine.hidden.Determinizer
import com.wingedsheep.ai.engine.AiProfiles
import com.wingedsheep.ai.engine.GameSimulator
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.gym.contract.ActionParameterizer
import com.wingedsheep.gym.contract.ActionParams
import com.wingedsheep.gym.contract.ActionRegistry
import com.wingedsheep.gym.contract.ObservationBuilder
import com.wingedsheep.gym.contract.ObservationResult
import com.wingedsheep.gym.contract.PolicyActionBoundary
import com.wingedsheep.gym.contract.PolicyActionStager
import com.wingedsheep.gym.contract.ResolvedAction
import com.wingedsheep.gym.service.AgentSpec
import com.wingedsheep.gym.service.EnvLimits
import com.wingedsheep.gym.service.SnapshotCodec
import com.wingedsheep.gym.service.SnapshotHandle
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng

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
    private val policySimulator = GameSimulator(environment.cardRegistry)
    private val determinizer = Determinizer(environment.cardRegistry)

    /** Pilot actions and delegated learner decisions so far this episode. */
    private var autoAdvanced: Int = 0
    private var delegatedDecisions: Int = 0

    /** Priority actions [playout] took for a learner seat, which the caller did not choose. */
    private var playedOut: Int = 0

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
            it.autoAdvanced = autoAdvanced
            it.delegatedDecisions = delegatedDecisions
            it.playedOut = playedOut
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
        playedOut = playedOut,
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
        autoAdvanced = 0
        delegatedDecisions = 0
        playedOut = 0
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
        // No agent spec at all is the original contract: the caller drives every seat and answers
        // every decision itself, through /envs/{id}/decision. Search callers rely on that.
        if (agents.isEmpty()) return
        drive(learnerActions = 0)
    }

    /**
     * Play on with every seat — the learner's included — driven by its own AI.
     *
     * This is what a paired branch rollout needs. A forked state has to be finished by a *fixed*
     * player for the branches' outcomes to be comparable, and finishing it inside the JVM keeps
     * Python out of a loop it has nothing to contribute to: no learner decision leaves the env, so
     * a branch costs one call rather than one round trip per decision.
     *
     * The learner seat is played by its `decisionProfile` — the same AI that already answers its
     * structured decisions (§3.2) — so a playout introduces no third behaviour into the episode.
     * Point that profile at the frozen pilot and a playout is the frozen pilot finishing the game.
     *
     * @param maxLearnerActions stop once the learner's own AI has taken this many priority actions.
     *   The default runs to a terminal state or a limit. One is "advance the generating trajectory
     *   by a single pilot decision", which is how a sampler walks from one candidate state to the
     *   next without ever choosing an action itself.
     */
    fun playout(maxLearnerActions: Int = Int.MAX_VALUE): ObservationResult {
        check(truncation == null) { "Env was truncated ($truncation); reset it before playing on" }
        require(maxLearnerActions > 0) { "maxLearnerActions must be positive" }
        drive(learnerActions = maxLearnerActions)
        // Leave the env where a stepping caller expects to find it: on a learner decision.
        advanceToLearner()
        return build(defaultRevealAll)
    }

    /**
     * The one loop behind [advanceToLearner] and [playout]: resolve pending decisions and priority
     * until the learner has something to decide, having acted for the learner [learnerActions]
     * times on the way. Zero is "stop at the learner", which is what a `step` means.
     */
    private fun drive(learnerActions: Int) {
        var taken = 0
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
            if (isLearner(priority)) {
                if (taken >= learnerActions) return
                taken++
                playedOut++
            }
            environment.step(aiFor(priority).chooseAction(environment.state))
            autoAdvanced++
            checkLimits()
        }
    }

    /**
     * The action the learner seat's `decisionProfile` would take here, without taking it.
     *
     * A fresh AI instance answers, as the arena's shadow teacher does, so asking cannot disturb the
     * seat's own AI (its Strategist memory, or a Treasure float it has queued). It chooses from the
     * same enumeration the observation was built from, so the answer maps to the current IDs.
     * Null when the learner has no priority decision to make.
     */
    fun pilotChoice(): PilotChoice? {
        val learner = environment.agentToAct ?: return null
        if (isTerminal || environment.state.pendingDecision != null || !isLearner(learner)) return null
        val legal = environment.legalActions()
        if (legal.isEmpty()) return null
        val profile = when (val agent = agentAt(environment.playerIds.indexOf(learner))) {
            is AgentSpec.Pilot -> agent.profile
            is AgentSpec.Learner -> agent.decisionProfile
        }
        val ai = AIPlayer.create(environment.cardRegistry, learner, AiProfiles.parse(profile))
        val chosen = ai.chooseFrom(environment.state, legal)
        val key = templateKey(chosen)
        // The mask preserves order, so an index into `legal` is the registry's ID for it.
        val id = legal.indexOfFirst { templateKey(it) == key }.takeIf { it >= 0 }
        val registered = id?.let { (registry.resolve(it) as? ResolvedAction.Legal)?.legalAction }
        return PilotChoice(
            actionId = id?.takeIf { registered != null && templateKey(registered) == key },
            kind = chosen.actionType,
            description = chosen.description,
            isManaAbility = chosen.isManaAbility,
            callable = registered?.affordable == true,
        )
    }

    private fun templateKey(action: LegalAction): List<Any?> {
        val source = when (val a = action.action) {
            is CastSpell -> a.cardId
            is ActivateAbility -> a.sourceId
            is PlayLand -> a.cardId
            else -> null
        }
        return listOf(action.actionType, action.description, source, (action.action as? ActivateAbility)?.abilityId)
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

    /**
     * Resample everything the learner seat cannot see, consistent with what it can.
     *
     * Two branches forked from one state and played out are *identical*, because the library was
     * shuffled once into immutable state at setup — the deal is already fixed at the fork point.
     * That is what makes a branch comparison paired, and it is also why repeating a branch measures
     * nothing on its own: the repeat is the same game.
     *
     * A repeat has to vary what the player does not know. Determinizing first gives each repetition
     * a different hidden world drawn from the same information set, so a set of branches answers
     * "is this action better over the deals I cannot rule out" rather than "was it better in this
     * one deal". Hold [seed] fixed across the actions being compared and vary it between
     * repetitions, and the comparison is paired within a world and independent across worlds.
     *
     * Entities and zone membership are untouched; only hidden identities and opponent library
     * order are sampled, so pending decisions and continuations stay valid. The seats' AIs are
     * rebuilt, because their short-lived memory is of a world that no longer exists.
     */
    fun determinize(seed: Long): ObservationResult {
        check(truncation == null) { "Env was truncated ($truncation); reset it before determinizing" }
        check(!isTerminal) { "Env is terminal; there is no hidden world left to sample" }
        val viewer = environment.playerIds[perspectivePlayerIndex]
        val sampled = determinizer.sample(
            environment.state,
            viewer,
            emptyMap(),
            GameRng.seeded(seed),
        )
        environment.restore(sampled, environment.playerIds, environment.stepCount)
        players.clear()
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
        val legal = environment.legalActions()
        val learnerActions = if (agents.isNotEmpty() && environment.agentToAct?.let(::isLearner) == true) {
            PolicyActionBoundary.mask(legal, environment.state, policySimulator)
        } else legal
        val result = observationBuilder.build(
            environment.state, perspective, learnerActions, revealAll
        )
        registry = result.registry
        return result
    }

    private fun executeResolved(resolved: ResolvedAction, actionId: Int, params: ActionParams) {
        when (resolved) {
            is ResolvedAction.Legal -> {
                if (agents.isNotEmpty()) {
                    require(resolved.legalAction.affordable) {
                        "Action $actionId is not callable by the learner policy"
                    }
                    PolicyActionBoundary.requirePolicyPayment(resolved.legalAction, params)
                }
                // The enumerated action is a template for the action types that need a choice the
                // ID can't carry (attackers, blockers, targets, X); params complete it.
                val action = ActionParameterizer.apply(resolved.action, params, environment.state)
                if (agents.isNotEmpty()) {
                    require(PolicyActionStager(policySimulator).begin(environment.state, action) != null) {
                        "Action $actionId failed policy engine preflight"
                    }
                }
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
