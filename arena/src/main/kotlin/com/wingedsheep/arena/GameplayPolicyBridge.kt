package com.wingedsheep.arena

import com.wingedsheep.ai.engine.GameSimulator
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.gym.contract.ActionParameterizer
import com.wingedsheep.gym.contract.ActionParams
import com.wingedsheep.gym.contract.ObservationBuilder
import com.wingedsheep.gym.contract.ResolvedAction
import com.wingedsheep.gym.contract.TrainingObservation
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** One policy action plus declaration telemetry that is not part of the atomic engine action. */
data class GameplayPolicyAction(
    val action: GameAction,
    val modelDecision: Boolean,
    val kind: String,
    val diagnostic: String,
    val declarationSize: Int,
    val combatOrderSize: Int,
    val combatOrderMatches: Boolean,
)

@Serializable
internal data class GameplayPolicyRequest(
    val observation: TrainingObservation,
    val deterministic: Boolean = true,
    val temperature: Double = 1.0,
)

@Serializable
internal data class GameplayPolicyResponse(
    @SerialName("action_id")
    val actionId: Int? = null,
    val params: ActionParams = ActionParams.EMPTY,
    @SerialName("combat_order")
    val combatOrder: List<List<EntityId>> = emptyList(),
    @SerialName("log_probability")
    val logProbability: Double = 0.0,
    val error: String? = null,
)

/**
 * Persistent subprocess adapter for ``mtgdraft.gameplay.arena_worker``.
 *
 * The JVM remains authoritative: the response's per-step ID is resolved against the registry built
 * with the observation and [ActionParameterizer] turns its parameters into the action the rules
 * engine validates.  Stdout is exactly one JSON response per request; worker diagnostics inherit
 * the arena's stderr so they cannot fill a pipe and deadlock a long run.
 */
class GameplayPolicyBridge(
    registry: CardRegistry,
    python: String,
    checkpoint: File,
    pythonPath: String? = null,
    failureDir: File? = null,
    device: String = "cpu",
    private val deterministic: Boolean = true,
    private val temperature: Double = 1.0,
) : AutoCloseable {
    private val enumerator = LegalActionEnumerator.create(registry)
    private val observations = ObservationBuilder(registry)
    private val simulator = GameSimulator(registry)
    private val failureDirectory = failureDir?.also {
        require(it.isDirectory || it.mkdirs()) {
            "could not create gameplay policy failure directory: $it"
        }
    }
    private val process = ProcessBuilder(buildList {
        addAll(listOf(python, "-m", "mtgdraft.gameplay.arena_worker", checkpoint.absolutePath))
        addAll(listOf("--device", device))
        failureDirectory?.let { addAll(listOf("--failure-dir", it.absolutePath)) }
    }).apply {
        redirectError(ProcessBuilder.Redirect.INHERIT)
        pythonPath?.let { environment()["PYTHONPATH"] = it }
    }.start()
    private val input = process.outputStream.bufferedWriter()
    private val output = process.inputStream.bufferedReader()
    private var pendingAfterManaFloat: Pair<List<GameAction>, GameAction>? = null

    init {
        require(temperature > 0) { "-Darena.policyTemperature must be positive" }
    }

    @Synchronized
    fun choose(state: GameState, playerId: EntityId): GameplayPolicyAction {
        check(state.pendingDecision == null) { "structured decisions stay with the arena AI" }
        check(state.priorityPlayerId == playerId) { "policy actor $playerId does not have priority" }
        pendingAfterManaFloat?.let { (activations, action) ->
            pendingAfterManaFloat = null
            val next = activations.firstOrNull() ?: action
            if (simulator.accepts(state, next)) {
                if (activations.isNotEmpty()) {
                    pendingAfterManaFloat = activations.drop(1) to action
                }
                return wrapped(
                    next, "staged mana payment for ${action::class.simpleName}", modelDecision = false,
                )
            }
        }
        // Keep unsupported actions as observation context but hard-mask them exactly like an
        // unaffordable action. ActionParams cannot carry these payment/crew selections yet; the
        // old path let the network pick one and the engine correctly rejected the incomplete cast.
        val legal = enumerator.enumerate(state, playerId, EnumerationMode.ACTIONS_ONLY).map {
            if (policyCallable(it)) it else it.copy(affordable = false)
        }
        val built = observations.build(state, playerId, legal)
        val observation = built.observation as TrainingObservation
        input.write(policyJson.encodeToString(GameplayPolicyRequest(observation, deterministic, temperature)))
        input.newLine()
        input.flush()
        val line = output.readLine() ?: error(
            "gameplay policy exited with code ${process.takeIf { !it.isAlive }?.exitValue()}"
        )
        val response = policyJson.decodeFromString<GameplayPolicyResponse>(line)
        response.error?.let { error("gameplay policy: $it") }
        val actionId = requireNotNull(response.actionId) { "gameplay policy response has no action_id" }
        val view = observation.legalActions.singleOrNull { it.actionId == actionId }
            ?: error("gameplay policy returned action ID $actionId absent from its observation")
        val resolved = built.registry.resolve(actionId)
        val action = when (resolved) {
            is ResolvedAction.Legal -> ActionParameterizer.apply(resolved.action, response.params, state)
            is ResolvedAction.Decision -> error("gameplay policy returned folded decision $actionId on a priority step")
            ResolvedAction.Unknown -> error("gameplay policy returned unknown action ID $actionId")
        }
        val declarationSize = when (action) {
            is DeclareAttackers -> action.attackers.size
            is DeclareBlockers -> action.blockers.values.sumOf { it.size }
            else -> 0
        }
        val diagnostic = "actionId=$actionId kind=${view.kind} description=${view.description} " +
            "affordable=${view.affordable} manaCost=${view.manaCost} params=${response.params}"
        if (!simulator.accepts(state, action)) {
            val mana = simulator.floatSacrificeMana(state, action)
                ?: error("policy action failed engine preflight; $diagnostic")
            pendingAfterManaFloat = mana.activations.drop(1) to action
            return wrapped(
                mana.activations.first(), "staged mana payment; $diagnostic", modelDecision = false,
            )
        }
        return wrapped(action, diagnostic, response.combatOrder, declarationSize)
    }

    private fun wrapped(
        action: GameAction,
        diagnostic: String,
        combatOrder: List<List<EntityId>> = emptyList(),
        declarationSize: Int = 0,
        modelDecision: Boolean = true,
    ) = GameplayPolicyAction(
        action = action,
        modelDecision = modelDecision,
        kind = action::class.simpleName ?: "?",
        diagnostic = diagnostic,
        declarationSize = declarationSize,
        combatOrderSize = combatOrder.size,
        combatOrderMatches = combatOrderMatches(action, combatOrder),
    )

    override fun close() {
        runCatching { input.close() }
        if (process.isAlive) process.destroy()
        runCatching { output.close() }
    }

    companion object {
        internal fun policyCallable(action: LegalAction): Boolean =
            action.actionType !in setOf("CrewVehicle", "SaddleMount") &&
                action.additionalCostInfo == null &&
                !action.hasConvoke &&
                !action.hasDelve &&
                !action.hasTapForGeneric &&
                !action.hasHarmonize &&
                !action.requiresManaColorChoice &&
                action.manaCostPerExtraTarget == null

        internal fun combatOrderMatches(action: GameAction, order: List<List<EntityId>>): Boolean {
            val pairs = order.map {
                require(it.size == 2) { "combat_order entry must contain exactly two entity IDs" }
                it[0] to it[1]
            }
            return when (action) {
                is DeclareAttackers ->
                    pairs.size == action.attackers.size && pairs.toMap() == action.attackers
                is DeclareBlockers -> {
                    val ordered = pairs.groupBy({ it.first }, { it.second })
                    pairs.size == action.blockers.values.sumOf { it.size } && ordered == action.blockers
                }
                else -> pairs.isEmpty()
            }
        }
    }
}

private val policyJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}
