package com.wingedsheep.replay

import com.wingedsheep.ai.engine.DecisionResponder
import com.wingedsheep.ai.engine.GameSimulator
import com.wingedsheep.ai.engine.StateProgress
import com.wingedsheep.ai.engine.TargetSelection
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.hidden.HiddenWorldMaterializationRequest
import com.wingedsheep.engine.hidden.HiddenWorldMaterializationResult
import com.wingedsheep.engine.hidden.HiddenWorldMaterializer
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gym.GameEnvironment
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import kotlin.random.Random

/**
 * Rebuilds one replay game in the engine, half-turn by half-turn.
 *
 * Setup: the user's real deck, with hand and library order forced to the recorded opening hand
 * and draws ([HiddenWorldMaterializer] rewrites the hidden slots); the opponent's known cards
 * padded with basics of their colours, with the card they are about to play written into a hidden
 * hand slot just in time. Nobody's library order has to be guessed.
 *
 * Search: from each kept state, a depth-first search over the actions the half-turn's [Plan]
 * allows (its land drops, casts, attackers and blockers) plus passing priority, with pending
 * decisions enumerated where that is cheap and answered by the AI's [DecisionResponder] where it
 * is not. A line succeeds when the turn ends with the plan done and the public state matching the
 * recorded snapshot ([Snapshotter.diff]). Up to [beamWidth] distinct matching states carry on to
 * the next half-turn; the first half-turn with none is where the game fails, and the closest
 * snapshot difference seen is the reason reported.
 *
 * The engine is driven one [ActionProcessor.process] at a time rather than through the AI's
 * [GameSimulator], which passes both players' priority until the stack is empty and so would never
 * let the opponent respond to a spell.
 */
class Reconstructor(
    private val registry: CardRegistry,
    private val snapshotter: Snapshotter,
    private val beamWidth: Int = 8,
    private val nodeBudget: Int = 20_000,
    private val seed: Long = 20260911L,
) {
    private val processor = ActionProcessor(EngineServices(registry), computeUndo = false)
    private val enumerator = LegalActionEnumerator.create(registry)
    private val responder =
        DecisionResponder(GameSimulator(registry, processor, enumerator), GameEnvironment.defaultEvaluator())
    private val materializer = HiddenWorldMaterializer(registry)

    private data class Node(val state: GameState, val plan: Plan)

    private class Search {
        val ends = mutableListOf<GameState>()
        var nodes = 0
        var closest: List<String>? = null

        fun near(diff: List<String>) {
            if (closest == null || diff.size < closest!!.size) closest = diff
        }
    }

    fun run(spec: GameSpec): GameResult {
        val t0 = System.currentTimeMillis()
        val beamSizes = mutableListOf<Int>()
        val nodeCounts = mutableListOf<Int>()
        fun result(status: String, reproduced: Int, failedAt: Int? = null, reason: String? = null) = GameResult(
            spec.gameId, status, spec.halfTurns.size, reproduced, failedAt, reason,
            beamSizes.toList(), nodeCounts.toList(), System.currentTimeMillis() - t0,
        )

        missingCard(spec)?.let { return result("skipped", 0, reason = "card not in engine: $it") }
        val init = try {
            GameInitializer(registry).initializeGame(
                GameConfig(
                    players = listOf(
                        PlayerConfig("user", Deck(spec.userDeck)),
                        PlayerConfig("oppo", Deck(oppoDeck(spec))),
                    ),
                    skipMulligans = true,
                    startingPlayerIndex = if (spec.onPlay) 0 else 1,
                    seed = seed,
                )
            )
        } catch (e: Exception) {
            return result("skipped", 0, reason = "init: ${e.message}")
        }
        val seats = Seats.of(init.state)
        val stacked = stackUser(init.state, seats, spec)
            ?: return result("skipped", 0, reason = "the recorded draws do not fit the user's deck")

        var beam = listOf(stacked)
        for ((i, ht) in spec.halfTurns.withIndex()) {
            val search = Search()
            for (start in beam) {
                if (search.nodes >= nodeBudget || search.ends.size >= beamWidth) break
                val revealed = revealOppo(start, seats, ht)
                if (revealed == null) {
                    search.near(listOf("could not write the opponent's cards into hidden slots"))
                    continue
                }
                searchHalfTurn(revealed, ht, i + 1, seats, search)
            }
            nodeCounts += search.nodes
            if (search.ends.isEmpty()) {
                val why = search.closest?.joinToString("; ") ?: "no line reached the end of the half-turn"
                return result("failed", i, i, why)
            }
            beam = search.ends.distinctBy { StateProgress.digest(it) }.take(beamWidth)
            beamSizes += beam.size
        }
        return result("reproduced", spec.halfTurns.size)
    }

    // =========================================================================
    // Search
    // =========================================================================

    private fun searchHalfTurn(start: GameState, ht: HalfTurnSpec, turn: Int, seats: Seats, search: Search) {
        val stack = ArrayDeque<Node>()
        stack.addLast(Node(start, Plan.of(ht)))
        while (stack.isNotEmpty() && search.nodes < nodeBudget && search.ends.size < beamWidth) {
            val node = stack.removeLast()
            search.nodes++
            val s = node.state
            if (s.gameOver) {
                if (ht.last) search.ends += s
                continue
            }
            // The last half-turn's snapshot was taken when the game ended, possibly mid-turn.
            if (ht.last && node.plan.done && s.pendingDecision == null) {
                val diff = snapshotter.diff(snapshotter.take(s, seats), ht.eot)
                if (diff.isEmpty()) {
                    search.ends += s
                    continue
                }
                search.near(diff)
            }
            val children = expand(node, ht, seats)
            // Push in reverse so the first child — a recorded action, passes come last — is
            // explored first.
            for (child in children.asReversed()) {
                if (child.state.turnNumber > turn && !child.state.gameOver) {
                    // This action ended the turn, so the parent is the end-of-turn position.
                    if (!child.plan.done) {
                        search.near(listOf("turn ended with recorded actions left: ${describe(child.plan)}"))
                        continue
                    }
                    val diff = snapshotter.diff(snapshotter.take(s, seats), ht.eot)
                    if (diff.isEmpty()) search.ends += child.state else search.near(diff)
                } else {
                    stack.addLast(child)
                }
            }
        }
    }

    private fun expand(node: Node, ht: HalfTurnSpec, seats: Seats): List<Node> {
        val s = node.state
        s.pendingDecision?.let { return decide(node, it, ht) }
        val player = s.priorityPlayerId ?: return emptyList()
        val side = seats.sideOf(player)
        val active = side == ht.active
        val plan = node.plan
        val legal = enumerator.enumerate(s, player, EnumerationMode.ACTIONS_ONLY)
        val out = mutableListOf<Node>()
        var pass: PassPriority? = null
        for (la in legal) {
            val action = la.action
            when {
                action is PassPriority -> pass = action
                action is PlayLand && active -> {
                    val name = snapshotter.name(s, action.cardId) ?: continue
                    if (plan.canPlayLand(name)) apply(s, action)?.let { out += Node(it, plan.playLand(name)) }
                }
                action is CastSpell && la.affordable -> {
                    val name = snapshotter.name(s, action.cardId) ?: continue
                    if (!plan.canCast(side, name)) continue
                    for (a in withTargets(s, la, player)) apply(s, a)?.let { out += Node(it, plan.cast(side, name)) }
                }
                la.actionType == "DeclareAttackers" && active && !plan.attacksDone ->
                    for (a in attacks(s, la, player, seats, plan)) {
                        apply(s, a)?.let { out += Node(it, plan.copy(attacksDone = true)) }
                    }
                la.actionType == "DeclareBlockers" && !active && !plan.blocksDone ->
                    for (a in blocks(s, la, player, plan)) {
                        apply(s, a)?.let { out += Node(it, plan.copy(blocksDone = true)) }
                    }
            }
        }
        pass?.let { p -> apply(s, p)?.let { out += Node(it, plan) } }
        if (out.isEmpty()) {
            // A mandatory action the plan does not model (damage assignment order, a second
            // combat's declaration): let each legal action through.
            for (la in legal.take(MAX_FALLBACK_ACTIONS)) apply(s, la.action)?.let { out += Node(it, plan) }
        }
        return out
    }

    private fun apply(s: GameState, action: GameAction): GameState? {
        val r = try {
            processor.process(s, action).result
        } catch (e: Exception) {
            return null
        }
        return if (r.error != null) null else r.state
    }

    // =========================================================================
    // Choices: targets, combat, decisions
    // =========================================================================

    private fun withTargets(s: GameState, la: LegalAction, player: EntityId): List<GameAction> {
        if (!la.requiresTargets) return listOf(la.action)
        val infos = TargetSelection.targetInfosFor(la) ?: return listOf(la.action)
        var combos: List<List<ChosenTarget>> = listOf(emptyList())
        for (info in infos) {
            val picks = subsets(info.validTargets, info.minTargets, info.maxTargets)
            combos = combos.flatMap { prefix ->
                picks.map { pick -> prefix + pick.map { TargetSelection.toChosenTarget(s, info, it, player) } }
            }.take(MAX_TARGET_COMBOS)
        }
        return combos.map { TargetSelection.applyTargets(la.action, it) }
    }

    private fun attacks(s: GameState, la: LegalAction, player: EntityId, seats: Seats, plan: Plan): List<GameAction> {
        if (plan.attacked.isEmpty()) return listOf(DeclareAttackers(player, emptyMap()))
        val defender = if (player == seats.user) seats.oppo else seats.user
        return pickByName(s, la.validAttackers.orEmpty(), plan.attacked)
            .map { ids -> DeclareAttackers(player, ids.associateWith { defender }) }
    }

    private fun blocks(s: GameState, la: LegalAction, player: EntityId, plan: Plan): List<GameAction> {
        if (plan.blocking.isEmpty()) return listOf(DeclareBlockers(player, emptyMap()))
        val attacking = s.getBattlefield().filter { s.getEntity(it)?.has<AttackingComponent>() == true }
        val targetSets = if (plan.blocked.isEmpty()) listOf(attacking) else pickByName(s, attacking, plan.blocked)
        val out = mutableListOf<GameAction>()
        for (blockers in pickByName(s, la.validBlockers.orEmpty(), plan.blocking)) {
            for (targets in targetSets) {
                for (assignment in ontoAssignments(blockers, targets)) {
                    out += DeclareBlockers(player, assignment.mapValues { listOf(it.value) })
                    if (out.size >= MAX_COMBAT_OPTIONS) return out
                }
            }
        }
        return out
    }

    private fun decide(node: Node, d: PendingDecision, ht: HalfTurnSpec): List<Node> {
        val s = node.state
        val responses: List<DecisionResponse> = when (d) {
            is YesNoDecision -> listOf(YesNoResponse(d.id, true), YesNoResponse(d.id, false))
            is ChooseModeDecision ->
                if (d.minModes == 1 && d.maxModes == 1) {
                    d.modes.filter { it.available }.map { ModesChosenResponse(d.id, listOf(it.index)) }
                } else heuristic(s, d)
            is ChooseNumberDecision ->
                (d.minValue..minOf(d.maxValue, d.minValue + MAX_NUMBER_OPTIONS)).map { NumberChosenResponse(d.id, it) }
            is ChooseColorDecision -> d.availableColors.map { ColorChosenResponse(d.id, it) }
            is ChooseOptionDecision -> d.options.indices.map { OptionChosenResponse(d.id, it) }
            is ChooseTargetsDecision -> targetResponses(d)
            is SelectCardsDecision ->
                if (d.minSelections == 1 && d.maxSelections == 1 && !d.ordered) {
                    d.options.map { CardsSelectedResponse(d.id, listOf(it)) }
                } else heuristic(s, d)
            is SearchLibraryDecision -> tutor(s, d, ht) ?: heuristic(s, d)
            else -> heuristic(s, d)
        }
        return responses.mapNotNull { r -> apply(s, SubmitDecision(d.playerId, r))?.let { Node(it, node.plan) } }
    }

    private fun targetResponses(d: ChooseTargetsDecision): List<DecisionResponse> {
        var combos: List<Map<Int, List<EntityId>>> = listOf(emptyMap())
        for (req in d.targetRequirements) {
            val picks = subsets(d.legalTargets[req.index].orEmpty(), req.minTargets, req.maxTargets)
            combos = combos.flatMap { m -> picks.map { m + (req.index to it) } }.take(MAX_TARGET_COMBOS)
        }
        return combos.map { TargetsResponse(d.id, it) }
    }

    /** A library search picks the recorded tutored card when the record names one. */
    private fun tutor(s: GameState, d: SearchLibraryDecision, ht: HalfTurnSpec): List<DecisionResponse>? {
        val wanted = ht.tutored.toMutableList()
        val picks = d.options.filter { id -> snapshotter.name(s, id)?.let { wanted.remove(it) } == true }
        if (picks.size < d.minSelections || picks.isEmpty()) return null
        return listOf(CardsSelectedResponse(d.id, picks.take(d.maxSelections)))
    }

    private fun heuristic(s: GameState, d: PendingDecision): List<DecisionResponse> =
        try {
            listOf(responder.respond(s, d, d.playerId))
        } catch (e: Exception) {
            emptyList()
        }

    /** Every way to pick entities from [candidates] whose names make up [names] (a multiset). */
    private fun pickByName(s: GameState, candidates: List<EntityId>, names: List<String>): List<List<EntityId>> {
        var combos: List<List<EntityId>> = listOf(emptyList())
        for ((name, k) in Snapshotter.counts(names)) {
            val pool = candidates.filter { snapshotter.name(s, it) == name }
            val picks = subsets(pool, k, k)
            if (picks.isEmpty()) return emptyList()
            combos = combos.flatMap { prefix -> picks.map { prefix + it } }.take(MAX_COMBAT_OPTIONS)
        }
        return combos
    }

    // =========================================================================
    // Setup and hidden cards
    // =========================================================================

    private fun missingCard(spec: GameSpec): String? =
        (spec.userDeck + spec.oppoKnown).firstOrNull { !registry.hasCard(it) }

    private fun oppoDeck(spec: GameSpec): List<String> {
        val basics = spec.oppColors.mapNotNull { BASICS[it] }.ifEmpty { listOf("Plains") }
        val deck = spec.oppoKnown.toMutableList()
        var i = 0
        while (deck.size < DECK_SIZE) deck += basics[i++ % basics.size]
        return deck
    }

    /** Force the user's opening hand and library to the recorded hand and draw order. */
    private fun stackUser(state: GameState, seats: Seats, spec: GameSpec): GameState? {
        val hand = state.getHand(seats.user)
        val library = state.getLibrary(seats.user)
        val draws = spec.halfTurns.filter { it.active == "user" }.flatMap { it.drawn }
        val rest = spec.userDeck.toMutableList()
        for (name in spec.openingHand + draws) if (!rest.remove(name)) return null
        if (hand.size != spec.openingHand.size || library.size != draws.size + rest.size) return null
        val order = draws + rest.shuffled(Random(seed))   // index 0 is the top of the library
        return materialize(state, hand.zip(spec.openingHand).toMap() + library.zip(order).toMap())
    }

    /**
     * Make sure the opponent holds the cards the record has them play this half-turn: needed cards
     * already in hand stay, the rest are written over filler hand slots, then over the top of the
     * library (a card drawn this turn can be the one played).
     */
    private fun revealOppo(state: GameState, seats: Seats, ht: HalfTurnSpec): GameState? {
        val own = if (ht.active == "oppo") ht.lands + ht.creatures + ht.noncreatures + ht.discarded else emptyList()
        val needed = Snapshotter.counts(own + ht.instants["oppo"].orEmpty() + ht.flash["oppo"].orEmpty()).toMutableMap()
        val free = mutableListOf<EntityId>()
        for (id in state.getHand(seats.oppo)) {
            val name = snapshotter.name(state, id)
            val left = name?.let { needed[it] } ?: 0
            if (name != null && left > 0) needed[name] = left - 1 else free += id
        }
        val missing = needed.flatMap { (name, k) -> List(k) { name } }
        if (missing.isEmpty()) return state
        val slots = free + state.getLibrary(seats.oppo).take(missing.size)
        if (slots.size < missing.size) return null
        return materialize(state, slots.zip(missing).toMap())
    }

    private fun materialize(state: GameState, names: Map<EntityId, String>): GameState? {
        val request = HiddenWorldMaterializationRequest(names.mapValues { registry.requireCard(it.value) }, state.rng)
        return when (val r = materializer.materialize(state, request)) {
            is HiddenWorldMaterializationResult.Materialized -> r.state
            is HiddenWorldMaterializationResult.Unsupported -> null
        }
    }

    private fun describe(plan: Plan): String = buildList {
        if (plan.lands.isNotEmpty()) add("lands ${plan.lands}")
        plan.spells.forEach { (side, left) -> if (left.isNotEmpty()) add("$side spells $left") }
        if (plan.attacked.isNotEmpty() && !plan.attacksDone) add("attack ${plan.attacked}")
        if (plan.blocking.isNotEmpty() && !plan.blocksDone) add("block ${plan.blocking}")
    }.joinToString(", ")

    companion object {
        const val DECK_SIZE = 40
        const val MAX_TARGET_COMBOS = 32
        const val MAX_COMBAT_OPTIONS = 24
        const val MAX_NUMBER_OPTIONS = 10
        const val MAX_FALLBACK_ACTIONS = 6
        val BASICS = mapOf('W' to "Plains", 'U' to "Island", 'B' to "Swamp", 'R' to "Mountain", 'G' to "Forest")

        /** Subsets of [items] with size in [min, max], smallest first; capped. */
        fun <T> subsets(items: List<T>, min: Int, max: Int, cap: Int = MAX_TARGET_COMBOS): List<List<T>> {
            val out = mutableListOf<List<T>>()
            fun go(start: Int, acc: List<T>) {
                if (out.size >= cap) return
                if (acc.size in min..max) out += acc
                if (acc.size == max) return
                for (i in start until items.size) go(i + 1, acc + items[i])
            }
            go(0, emptyList())
            return out.sortedBy { it.size }
        }

        /** Assignments of each blocker to one of [targets] in which every target is blocked. */
        fun <B, T> ontoAssignments(blockers: List<B>, targets: List<T>, cap: Int = MAX_COMBAT_OPTIONS): List<Map<B, T>> {
            val out = mutableListOf<Map<B, T>>()
            fun go(i: Int, acc: Map<B, T>) {
                if (out.size >= cap) return
                if (i == blockers.size) {
                    if (acc.values.toSet().size == targets.size) out += acc
                    return
                }
                for (t in targets) go(i + 1, acc + (blockers[i] to t))
            }
            if (targets.isNotEmpty()) go(0, emptyMap())
            return out
        }
    }
}
