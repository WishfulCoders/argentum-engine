package com.wingedsheep.engine.loop

import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.GameLimits
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Loop shortcuts (Magic Tournament Rules 4.4): a player who has just performed a sequence of
 * actions that brings the game back to where it started may repeat it a stated number of times in
 * one decision, instead of clicking through every iteration.
 *
 * This is a *decision* shortcut, not a *computation* shortcut. Every repeated action still goes
 * through [ActionProcessor.process], so all rules, triggers and replacement effects apply exactly as
 * if the player had performed them by hand; nothing is extrapolated. That keeps the shortcut
 * outside the rules engine's trust boundary — the worst a wrong detection can do is offer a loop
 * whose replay then fails, which is refused and leaves the game untouched.
 *
 * The engine is stateless, so the caller keeps the history: one [LoopStep] (state before, action)
 * per action this turn, for every player. [detect] looks for the shortest recent segment of it that
 * is a loop for the acting player; [run] repeats a detected loop.
 *
 * **What counts as a loop.** A segment of the history, ending at the current state, where
 * - the acting player holds priority with an empty stack and nothing pending at both ends, in the
 *   same step of the same turn;
 * - every action in it is the acting player's, except the other players' priority passes (an
 *   opponent who does anything else has interrupted the loop, MTR 4.4: they may respond only by
 *   shortening it — here, by acting once it is done);
 * - every object the actions refer to is back where it was ([Role]), or was made anew by the
 *   segment itself;
 * - it changed something ([LoopDelta] is not zero) — a loop that does nothing is not worth repeating
 *   and a mandatory one is a draw (CR 104.4b), which the engine handles elsewhere;
 * - and one trial repetition from the current state replays cleanly and changes exactly the same
 *   things. That trial is what makes the detection trustworthy; the checks before it are only there
 *   to reject non-loops cheaply.
 *
 * **Mapping objects between iterations.** Cards keep their [EntityId] across zone changes in this
 * engine, so a card the loop uses is simply the same id next time ([Role.Persistent]). New objects —
 * tokens — get fresh ids from [GameState.nextEntityId], which counts up deterministically. An object
 * the segment created is therefore identified by its offset from the counter at the start of the
 * iteration ([Role.Minted]), and an older token the loop used up and replaced (sacrifice a token,
 * make a new one) is identified by the offset of its replacement in the previous iteration
 * ([Role.Successor]). Decision answers that name something other than an entity (the order of
 * triggers, an option id) are mapped by position in the new decision, see [rewriteResponse].
 */
class LoopShortcut(private val executor: LoopExecutor) {

    /** Replays each action straight through [processor] — the game server's way of playing. */
    constructor(processor: ActionProcessor) : this(LoopExecutor.of(processor))

    /**
     * Find a loop the acting player has just completed, or null. [history] is this turn's actions in
     * order, each with the state it was taken in; [now] is the state after the last of them.
     */
    fun detect(history: List<LoopStep>, now: GameState, playerId: EntityId): LoopCandidate? {
        if (!atRest(now, playerId) || history.isEmpty()) return null
        val nowTally = tally(now)
        // Shortest loop first: walk back from the most recent action.
        val earliest = (history.size - MAX_ACTIONS_PER_ITERATION).coerceAtLeast(0)
        for (start in history.indices.reversed()) {
            if (start < earliest) break
            val step = history[start]
            val from = step.before
            if (from.turnNumber != now.turnNumber) break
            // An opponent's real action ends every candidate that reaches back past it.
            if (step.action.playerId != playerId && step.action !is PassPriority) break
            if (step.action.playerId != playerId) continue
            if (!atRest(from, playerId) || !samePoint(from, now)) continue
            val segment = history.subList(start, history.size)
            if (segment.none { it.action.playerId == playerId && it.action !is PassPriority }) continue

            val delta = LoopDelta.between(tally(from), nowTally)
            if (delta.isZero) continue
            val roles = classify(segment, from, now) ?: continue
            val candidate = LoopCandidate(
                playerId = playerId,
                steps = segment.toList(),
                roles = roles,
                recordedBase = from.nextEntityId,
                mintedPerIteration = now.nextEntityId - from.nextEntityId,
                delta = delta,
                iterationsToWin = iterationsToWin(now, playerId, delta),
                maxIterations = maxIterations(now, segment.size, delta),
                offeredAt = now,
            )
            if (candidate.maxIterations < 1) continue
            val trial = run(candidate, now, 1)
            if (trial.iterations == 1 && (trial.stop == LoopStop.COMPLETED || trial.stop == LoopStop.GAME_OVER)) {
                return candidate
            }
        }
        return null
    }

    /**
     * Repeat [candidate] up to [iterations] more times from [state], which must be the state the
     * loop was offered in ([LoopCandidate.offeredAt]). Stops early when the game ends, when an
     * iteration cannot be replayed (that iteration is discarded, so the result is always a state the
     * game really reached at the end of a whole iteration), or when an iteration changes something
     * different from the first one (that iteration is kept — it was legal play — but the loop is no
     * longer the one the player stated).
     */
    fun run(candidate: LoopCandidate, state: GameState, iterations: Int): LoopRun {
        if (state.nextEntityId != candidate.offeredAt.nextEntityId || !atRest(state, candidate.playerId) ||
            !samePoint(state, candidate.offeredAt)
        ) {
            return LoopRun(state, emptyList(), 0, LoopStop.FAILED, "the game has moved on since the loop was offered")
        }
        val wanted = iterations.coerceIn(0, candidate.maxIterations)
        var current = state
        var prevBase = candidate.recordedBase
        val actions = ArrayList<GameAction>()
        var done = 0
        while (done < wanted) {
            val base = current.nextEntityId
            val before = tally(current)
            when (val r = replayIteration(candidate, current, base, prevBase)) {
                is Iteration.Failed -> return LoopRun(current, actions, done, LoopStop.FAILED, r.reason)
                is Iteration.Done -> {
                    actions += r.actions
                    done++
                    if (r.state.gameOver) return LoopRun(r.state, actions, done, LoopStop.GAME_OVER)
                    val same = LoopDelta.between(before, tally(r.state)) == candidate.delta &&
                        r.state.nextEntityId - base == candidate.mintedPerIteration
                    current = r.state
                    prevBase = base
                    if (!same) return LoopRun(current, actions, done, LoopStop.DIVERGED, "this iteration changed something different")
                }
            }
        }
        return LoopRun(current, actions, done, LoopStop.COMPLETED)
    }

    // ---------------------------------------------------------------------------------------------
    // Replay
    // ---------------------------------------------------------------------------------------------

    private sealed interface Iteration {
        data class Done(val state: GameState, val actions: List<GameAction>) : Iteration
        data class Failed(val reason: String) : Iteration
    }

    private fun replayIteration(c: LoopCandidate, start: GameState, base: Long, prevBase: Long): Iteration {
        val idMap = HashMap<String, String>(c.roles.size * 2)
        for ((id, role) in c.roles) {
            idMap[id] = when (role) {
                is Role.Persistent -> id
                is Role.Minted -> "e${base + role.offset}"
                is Role.Successor -> "e${prevBase + role.offset}"
            }
        }
        var s = start
        val done = ArrayList<GameAction>(c.steps.size)
        for (step in c.steps) {
            val recorded = step.action
            val action = if (recorded is SubmitDecision) {
                val now = s.pendingDecision ?: return Iteration.Failed("expected a decision, none is pending")
                val was = step.before.pendingDecision ?: return Iteration.Failed("recorded answer has no decision")
                if (now.playerId != recorded.playerId || now::class != was::class) {
                    return Iteration.Failed("a different decision is pending")
                }
                rewriteResponse(recorded, was, now, idMap) ?: return Iteration.Failed("could not map the answer")
            } else {
                if (s.pendingDecision != null) return Iteration.Failed("an unexpected decision is pending")
                rewrite(recorded, idMap) ?: return Iteration.Failed("could not map the action")
            }
            s = when (val result = executor.execute(s, action)) {
                is LoopExecution.Rejected -> return Iteration.Failed(result.reason)
                is LoopExecution.Accepted -> result.state
            }
            done += action
            if (s.gameOver) return Iteration.Done(s, done)
        }
        if (!atRest(s, c.playerId) || !samePoint(s, start)) {
            return Iteration.Failed("the iteration did not come back to where it started")
        }
        return Iteration.Done(s, done)
    }

    /** [action] with every entity id replaced through [idMap]; null if it does not survive the round trip. */
    private fun rewrite(action: GameAction, idMap: Map<String, String>): GameAction? {
        if (idMap.all { (k, v) -> k == v }) return action
        return try {
            val tree = json.encodeToJsonElement(GameAction.serializer(), action)
            json.decodeFromJsonElement(GameAction.serializer(), mapStrings(tree) { idMap[it] })
        } catch (e: Exception) {
            null
        }
    }

    /**
     * The recorded answer [recorded] to decision [was], re-aimed at the decision [now] pending in
     * this iteration. Entity ids go through [idMap]; any other string the answer names (a trigger to
     * order, an option id) is found in [was] and replaced by whatever [now] holds at the same place.
     */
    private fun rewriteResponse(
        recorded: SubmitDecision,
        was: PendingDecision,
        now: PendingDecision,
        idMap: Map<String, String>,
    ): SubmitDecision? = try {
        val wasTree = json.encodeToJsonElement(PendingDecision.serializer(), was)
        val nowTree = json.encodeToJsonElement(PendingDecision.serializer(), now)
        val wasPaths = HashMap<String, List<Any>>()
        indexStrings(wasTree, emptyList(), wasPaths)
        val response = recorded.response.withDecisionId(now.id)
        val tree = json.encodeToJsonElement(SubmitDecision.serializer(), recorded.copy(response = response))
        val mapped = mapStrings(tree) { v ->
            if (v == now.id) null
            else idMap[v] ?: wasPaths[v]?.let { path -> stringAt(nowTree, path) }
        }
        json.decodeFromJsonElement(SubmitDecision.serializer(), mapped)
    } catch (e: Exception) {
        null
    }

    // ---------------------------------------------------------------------------------------------
    // Detection helpers
    // ---------------------------------------------------------------------------------------------

    /**
     * The [Role] of every entity the segment's actions refer to, or null when one of them is not
     * accounted for (a card that left for good, an object that cannot be found next time).
     */
    private fun classify(segment: List<LoopStep>, from: GameState, to: GameState): Map<String, Role>? {
        val fromZones = zoneIndex(from)
        val toZones = zoneIndex(to)
        val base = from.nextEntityId
        val end = to.nextEntityId
        val minted = (base until end).map { "e$it" }
        val claimed = HashSet<String>()
        val roles = HashMap<String, Role>()
        // Ids of objects that existed only part-way through the iteration (an ability on the stack).
        val transient = HashSet<String>()
        for (step in segment) step.before.entities.keys.forEach { transient += it.value }

        for (step in segment) {
            val strings = HashSet<String>()
            collectStrings(json.encodeToJsonElement(GameAction.serializer(), step.action), strings)
            val inDecision = step.action is SubmitDecision
            for (v in strings) {
                if (v in roles) continue
                val id = EntityId(v)
                if (from.entities.containsKey(id)) {
                    if (to.turnOrder.contains(id) || signature(from, fromZones, id) == signature(to, toZones, id)) {
                        roles[v] = Role.Persistent
                        continue
                    }
                    val wanted = signature(from, fromZones, id)
                    val successor = minted.singleOrNull { it !in claimed && signature(to, toZones, EntityId(it)) == wanted }
                    if (successor != null) {
                        claimed += successor
                        roles[v] = Role.Successor(successor.drop(1).toLong() - base)
                    } else if (!inDecision) {
                        return null
                    }
                } else if (v.startsWith("e") && v.drop(1).toLongOrNull()?.let { it in base until end } == true) {
                    roles[v] = Role.Minted(v.drop(1).toLong() - base)
                } else if (v in transient && !inDecision) {
                    // Refers to something the loop made and that cannot be named next time.
                    return null
                }
            }
        }
        return roles
    }

    private data class Signature(
        val definition: String?,
        val zone: ZoneKey?,
        val controller: EntityId?,
        val tapped: Boolean,
        val faceDown: Boolean,
    )

    private fun signature(state: GameState, zones: Map<EntityId, ZoneKey>, id: EntityId): Signature? {
        val e = state.getEntity(id) ?: return null
        return Signature(
            definition = e.get<CardComponent>()?.cardDefinitionId,
            zone = zones[id],
            controller = e.get<ControllerComponent>()?.playerId,
            tapped = e.has<TappedComponent>(),
            faceDown = e.has<FaceDownComponent>(),
        )
    }

    private fun zoneIndex(state: GameState): Map<EntityId, ZoneKey> {
        val index = HashMap<EntityId, ZoneKey>()
        for ((key, ids) in state.zones) for (id in ids) index[id] = key
        for (id in state.stack) index[id] = ZoneKey(id, Zone.STACK)
        return index
    }

    private fun atRest(state: GameState, playerId: EntityId): Boolean =
        !state.gameOver && state.priorityPlayerId == playerId && state.stack.isEmpty() &&
            state.pendingDecision == null && state.continuationStack.isEmpty() && state.pendingTriggers.isEmpty()

    private fun samePoint(a: GameState, b: GameState): Boolean =
        a.turnNumber == b.turnNumber && a.phase == b.phase && a.step == b.step && a.activePlayerId == b.activePlayerId

    private fun iterationsToWin(state: GameState, playerId: EntityId, delta: LoopDelta): Int? {
        val opponents = state.turnOrder.filter { it != playerId }
        if (opponents.isEmpty()) return null
        var most = 0
        for (o in opponents) {
            val now = tallyOf(state, o)
            val d = delta.perPlayer[o] ?: return null
            val byLife = if (d.life < 0) ceilDiv(now.life.coerceAtLeast(0), -d.life) else null
            val byPoison = if (d.poison > 0) ceilDiv((POISON_TO_LOSE - now.poison).coerceAtLeast(0), d.poison) else null
            val need = listOfNotNull(byLife, byPoison).minOrNull() ?: return null
            most = maxOf(most, need)
        }
        return most
    }

    private fun maxIterations(state: GameState, actionsPerIteration: Int, delta: LoopDelta): Int {
        var max = minOf(GameLimits.MAX_LOOP_ITERATIONS, GameLimits.MAX_LOOP_ACTIONS / actionsPerIteration.coerceAtLeast(1))
        val tokensPerIteration = delta.perPlayer.values.sumOf { it.tokens }
        if (tokensPerIteration > 0) {
            val room = GameLimits.MAX_TOKENS_ON_BATTLEFIELD - GameLimits.tokensOnBattlefield(state)
            max = minOf(max, room.coerceAtLeast(0) / tokensPerIteration)
        }
        return max
    }

    companion object {
        /** Longest iteration [detect] looks for, in actions (both players' passes included). */
        const val MAX_ACTIONS_PER_ITERATION = 64

        private const val POISON_TO_LOSE = 10

        private val json = Json { serializersModule = engineSerializersModule }

        private fun ceilDiv(a: Int, b: Int): Int = (a + b - 1) / b

        internal fun tally(state: GameState): Map<EntityId, PlayerTally> =
            state.turnOrder.associateWith { tallyOf(state, it) }

        private fun tallyOf(state: GameState, playerId: EntityId): PlayerTally {
            val player = state.getEntity(playerId)
            val pool = player?.get<ManaPoolComponent>()
            val permanents = state.getBattlefield().filter {
                state.getEntity(it)?.get<ControllerComponent>()?.playerId == playerId
            }
            return PlayerTally(
                // Through the resolver: a Two-Headed Giant team shares one life total (CR 810.9a).
                life = state.lifeTotal(playerId),
                poison = player?.get<CountersComponent>()?.getCount(CounterType.POISON) ?: 0,
                hand = state.getHand(playerId).size,
                library = state.getLibrary(playerId).size,
                graveyard = state.getGraveyard(playerId).size,
                exile = state.getZone(playerId, Zone.EXILE).size,
                permanents = permanents.size,
                tokens = permanents.count { state.getEntity(it)?.has<TokenComponent>() == true },
                counters = permanents.sumOf { id ->
                    state.getEntity(id)?.get<CountersComponent>()?.counters?.values?.sum() ?: 0
                },
                mana = pool?.let { it.white + it.blue + it.black + it.red + it.green + it.colorless } ?: 0,
            )
        }

        private fun collectStrings(e: JsonElement, out: MutableSet<String>) {
            when (e) {
                is JsonObject -> { e.keys.forEach(out::add); e.values.forEach { collectStrings(it, out) } }
                is JsonArray -> e.forEach { collectStrings(it, out) }
                is JsonPrimitive -> if (e.isString) out += e.content
            }
        }

        /** Rewrite every string — values and object keys (a map keyed by entity id) — through [f]. */
        private fun mapStrings(e: JsonElement, f: (String) -> String?): JsonElement = when (e) {
            is JsonObject -> JsonObject(e.entries.associate { (k, v) -> (f(k) ?: k) to mapStrings(v, f) })
            is JsonArray -> JsonArray(e.map { mapStrings(it, f) })
            is JsonPrimitive -> if (e.isString) f(e.content)?.let(::JsonPrimitive) ?: e else e
        }

        /** First place each string value occurs, as a path of object keys and array indices. */
        private fun indexStrings(e: JsonElement, path: List<Any>, out: MutableMap<String, List<Any>>) {
            when (e) {
                is JsonObject -> e.entries.forEach { (k, v) -> indexStrings(v, path + k, out) }
                is JsonArray -> e.forEachIndexed { i, v -> indexStrings(v, path + i, out) }
                is JsonPrimitive -> if (e.isString) out.putIfAbsent(e.content, path)
            }
        }

        private fun stringAt(e: JsonElement, path: List<Any>): String? {
            var cur: JsonElement = e
            for (p in path) {
                cur = when {
                    p is String && cur is JsonObject -> cur[p] ?: return null
                    p is Int && cur is JsonArray -> cur.getOrNull(p) ?: return null
                    else -> return null
                }
            }
            return (cur as? JsonPrimitive)?.takeIf { it.isString }?.content
        }
    }
}

/**
 * How [LoopShortcut] plays one recorded action. The game server applies actions one at a time
 * ([of] an [ActionProcessor]); the gym's step also passes priority and answers forced decisions
 * until the game is quiet, so its history holds the agent's actions only and it replays them the
 * same way. Either way the history and the replay must use the same executor.
 */
fun interface LoopExecutor {
    fun execute(state: GameState, action: GameAction): LoopExecution

    companion object {
        fun of(processor: ActionProcessor) = LoopExecutor { state, action ->
            val result = processor.process(state, action).result
            result.error?.let { LoopExecution.Rejected(it) } ?: LoopExecution.Accepted(result.state)
        }
    }
}

/** What a [LoopExecutor] made of one action. */
sealed interface LoopExecution {
    data class Accepted(val state: GameState) : LoopExecution
    data class Rejected(val reason: String) : LoopExecution
}

/** One action of the history [LoopShortcut.detect] reads, with the state it was taken in. */
data class LoopStep(val before: GameState, val action: GameAction)

/** How an entity named by a recorded action is found again in a later iteration. */
sealed interface Role {
    /** The same object every time (cards keep their id across zone changes). */
    data object Persistent : Role

    /** Made during the iteration: the [offset]-th id minted after the iteration began. */
    data class Minted(val offset: Long) : Role

    /** Used up and remade: its replacement is the [offset]-th id minted in the previous iteration. */
    data class Successor(val offset: Long) : Role
}

/** A loop [LoopShortcut.detect] found, ready to [LoopShortcut.run]. */
class LoopCandidate internal constructor(
    /** The player repeating the loop. */
    val playerId: EntityId,
    /** One iteration as it was played, opponents' passes included. */
    val steps: List<LoopStep>,
    internal val roles: Map<String, Role>,
    /** [GameState.nextEntityId] when the recorded iteration began. */
    internal val recordedBase: Long,
    /** Ids the recorded iteration minted; every repetition must mint the same number. */
    internal val mintedPerIteration: Long,
    /** What one iteration changes. */
    val delta: LoopDelta,
    /** Further iterations that bring every opponent to 0 life or 10 poison, when the loop does that. */
    val iterationsToWin: Int?,
    /** The most further iterations [LoopShortcut.run] will perform (see [GameLimits]). */
    val maxIterations: Int,
    /** The state the loop was found in; [LoopShortcut.run] only starts from this point. */
    val offeredAt: GameState,
) {
    /** The player's own non-pass actions in one iteration — what the loop "is", for a label. */
    val playerActions: List<GameAction> get() = steps.map { it.action }.filter { it.playerId == playerId && it !is PassPriority }
}

/** The countable things about one player a loop can change. */
data class PlayerTally(
    val life: Int = 0,
    val poison: Int = 0,
    val hand: Int = 0,
    val library: Int = 0,
    val graveyard: Int = 0,
    val exile: Int = 0,
    val permanents: Int = 0,
    val tokens: Int = 0,
    /** Counters on the permanents they control, all kinds summed. */
    val counters: Int = 0,
    /** Mana in their pool, all colours summed. */
    val mana: Int = 0,
) {
    operator fun minus(o: PlayerTally) = PlayerTally(
        life - o.life, poison - o.poison, hand - o.hand, library - o.library, graveyard - o.graveyard,
        exile - o.exile, permanents - o.permanents, tokens - o.tokens, counters - o.counters, mana - o.mana,
    )

    val isZero: Boolean get() = this == PlayerTally()

    /** Non-zero fields as (name, change), for a label like "life −1, tokens +1". */
    fun changes(): List<Pair<String, Int>> = listOf(
        "life" to life, "poison" to poison, "cards in hand" to hand, "library" to library,
        "graveyard" to graveyard, "exile" to exile, "permanents" to permanents, "tokens" to tokens,
        "counters" to counters, "mana" to mana,
    ).filter { it.second != 0 }
}

/** What one iteration of a loop changes, per player. */
data class LoopDelta(val perPlayer: Map<EntityId, PlayerTally>) {
    val isZero: Boolean get() = perPlayer.values.all { it.isZero }

    companion object {
        fun between(before: Map<EntityId, PlayerTally>, after: Map<EntityId, PlayerTally>) =
            LoopDelta(after.mapValues { (id, t) -> t - (before[id] ?: PlayerTally()) })
    }
}

enum class LoopStop {
    /** Every requested iteration ran. */
    COMPLETED,

    /** The game ended during the last iteration. */
    GAME_OVER,

    /** An iteration ran but changed something different; it is kept, the rest are not run. */
    DIVERGED,

    /** An iteration could not be replayed; the state is that before it. */
    FAILED,
}

/** The outcome of [LoopShortcut.run]: the state reached and every action performed to reach it. */
data class LoopRun(
    val state: GameState,
    val actions: List<GameAction>,
    val iterations: Int,
    val stop: LoopStop,
    val reason: String? = null,
)
