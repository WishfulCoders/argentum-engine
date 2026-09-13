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
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.PlottedComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.gym.GameEnvironment
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.ConvokePayment
import com.wingedsheep.sdk.scripting.KeywordAbility
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
 * recorded snapshot ([Snapshotter.diff]). The kept states' searches advance in turn, grouped by
 * [Snapshotter.hidden], until every tree is exhausted (or [MAX_ENDS] matching states, or the node budget), and up to [beamWidth] of
 * the matching states
 * carry on to the next half-turn, picked round-robin across what the snapshot cannot see
 * ([Snapshotter.hidden]) so that an aura on the wrong creature does not crowd out the right one.
 * The first half-turn with none is where the game fails, and the closest snapshot difference seen
 * is the reason reported.
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
    /** Prints the search of one half-turn; see [Tracer]. */
    private val tracer: Tracer? = null,
) {
    private val processor = ActionProcessor(EngineServices(registry), computeUndo = false)
    private val enumerator = LegalActionEnumerator.create(registry)
    private val responder =
        DecisionResponder(GameSimulator(registry, processor, enumerator), GameEnvironment.defaultEvaluator())
    private val materializer = HiddenWorldMaterializer(registry)

    /** Why the last [apply] or [materialize] was refused, for the trace and the failure reason. */
    private var lastError: String? = null
    /** The hidden slot the materializer last refused, if it named one. */
    private var lastRefused: EntityId? = null
    /** The side whose untapped lands this half-turn leaves matter: it casts in the next one. */
    private var keepMana: String? = null

    private data class Node(val state: GameState, val plan: Plan, val line: Move? = null)

    /** A matching end state and the line of actions that reached it. */
    class Kept(val state: GameState, val line: Move?)

    /** Half-turn being searched, stamped on each [Move]. */
    private var halfTurnIndex = 0

    /** The accepted line of the last [run]: a reproduced game's whole line, a failed one's up to [acceptedThrough]. */
    var acceptedLine: Move? = null
        private set
    /** Half-turns [acceptedLine] covers. */
    var acceptedThrough = 0
        private set

    private fun Move?.then(before: GameState, action: GameAction, how: String) =
        Move(before, action, halfTurnIndex, how, this)

    private fun Move?.then(steps: List<Pair<GameState, GameAction>>, how: String): Move? =
        steps.fold(this) { line, (before, action) -> line.then(before, action, how) }

    private class Search {
        val ends = mutableListOf<Kept>()
        var nodes = 0
        var closest: List<String>? = null
        var tracer: Tracer? = null
        var nodeCap = Int.MAX_VALUE
        /** The user's cards played from outside the hand this half-turn and the next two ([impulseTop]). */
        var outside: List<String> = emptyList()
        /** The user's recorded draws and outside-hand plays from this half-turn on: an impulse's spares must not be these. */
        var draws: List<String> = emptyList()
        /** What the opponent plays in the next two half-turns, which their impulse may have exiled. */
        var oppoLater: List<String> = emptyList()
        /** What the snapshot cannot see about an end state ([Reconstructor.unseen]); the beam is picked across it. */
        var groupOf: (GameState) -> List<String> = { emptyList() }
        var perGroup = 0
        private val groupSizes = HashMap<List<String>, Int>()
        private val digests = HashSet<Any>()

        /**
         * Keeps a matching end state, unless [perGroup] of its unseen-state group are kept already: a
         * subtree that differs only in what nobody can see (every colour an aura's land could be,
         * on the wrong host) must not fill [MAX_ENDS] before the other hosts are looked at.
         */
        fun end(s: GameState, line: Move?) {
            if (!digests.add(StateProgress.digest(s))) return
            val k = groupOf(s)
            val n = groupSizes[k] ?: 0
            if (n < perGroup) {
                groupSizes[k] = n + 1
                ends += Kept(s, line)
            }
        }

        /** States already given their opponent-impulse branch ([oppoImpulse]), by identity. */
        val branched: MutableSet<GameState> = java.util.Collections.newSetFromMap(java.util.IdentityHashMap())

        fun near(diff: List<String>) {
            tracer?.line("    end check: ${diff.joinToString("; ")}")
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

        acceptedLine = null
        acceptedThrough = 0
        missingCard(spec)?.let { return result("skipped", 0, reason = "card not in engine: $it") }
        val init = try {
            GameInitializer(registry).initializeGame(
                GameConfig(
                    players = listOf(
                        PlayerConfig("user", Deck(spec.userDeck.map(snapshotter::engineName))),
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

        var beam = listOf(Kept(stacked, null))
        // the previous half-turn's matching end states left out of the beam
        var spare = emptyList<Kept>()
        for (i in spec.halfTurns.indices) {
            var search = searchStep(spec, i, beam, seats, withDrawOrders = false)
            nodeCounts += search.nodes
            val ht = spec.halfTurns[i]
            if (search.ends.isEmpty() && ht.active == "user" && ht.drawn.distinct().size > 1) {
                // the recorded draw order first; the other orders only when it fails
                tracer?.takeIf { it.halfTurn == i }?.line("### retry with the half-turn's draws in other orders")
                val retry = searchStep(spec, i, beam, seats, withDrawOrders = true)
                nodeCounts[nodeCounts.lastIndex] += retry.nodes
                if (retry.ends.isNotEmpty()) search = retry
            }
            if (search.ends.isEmpty() && spare.isNotEmpty()) {
                // Backtrack one half-turn: the beam kept the wrong lines of the previous one (an
                // unseen choice diverged here), so the half-turn is searched again from the rest.
                tracer?.takeIf { it.halfTurn == i }?.line("### retry from the previous half-turn's other ${spare.size} matching states")
                val retry = searchStep(spec, i, spare, seats, withDrawOrders = true)
                nodeCounts[nodeCounts.lastIndex] += retry.nodes
                if (retry.ends.isNotEmpty()) search = retry
            }
            spare = emptyList()
            if (search.ends.isEmpty()) {
                val why = search.closest?.joinToString("; ") ?: "no line reached the end of the half-turn"
                return result("failed", i, i, why)
            }
            beam = pickBeam(search.ends, search.groupOf)
            spare = search.ends.filter { e -> beam.none { it === e } }
            beamSizes += beam.size
            acceptedLine = beam.first().line
            acceptedThrough = i + 1
        }
        return result("reproduced", spec.halfTurns.size)
    }

    /**
     * Searches half-turn [i] from the [beam]'s states; the [Search] holds the matching end states.
     * With [withDrawOrders], also from each other order of the half-turn's draws ([drawOrders]).
     */
    private fun searchStep(spec: GameSpec, i: Int, beam: List<Kept>, seats: Seats, withDrawOrders: Boolean): Search {
        val ht = spec.halfTurns[i]
        halfTurnIndex = i
        val search = Search()
        search.outside = spec.halfTurns.drop(i).take(3).flatMap { it.outsideHand }
        search.draws = spec.halfTurns.drop(i).flatMap { it.drawn + it.tutored + it.outsideHand }
        // The active player's lands stay tapped through the next half-turn: if they cast there,
        // which lands pay now matters, and end states differing in it are kept apart.
        keepMana = spec.halfTurns.getOrNull(i + 1)
            ?.takeIf { it.instants[ht.active].orEmpty().isNotEmpty() || it.flash[ht.active].orEmpty().isNotEmpty() }
            ?.let { ht.active }
        search.groupOf = { s -> unseen(s, seats) }
        search.perGroup = beamWidth
        search.oppoLater = spec.halfTurns.drop(i + 1).take(2).flatMap { h ->
            (if (h.active == "oppo") h.lands + h.creatures + h.noncreatures else emptyList()) +
                h.instants["oppo"].orEmpty() + h.flash["oppo"].orEmpty()
        }
        val tracing = tracer?.halfTurn == i
        if (tracing) tracer!!.begin(spec, i, beam.size)
        search.tracer = tracer.takeIf { tracing }
        // start states grouped by what the snapshot cannot see, in beam order
        val groups = linkedMapOf<List<String>, MutableList<ArrayDeque<Node>>>()
        var starts = 0
        for ((start, line) in beam.map { it.state to it.line }) {
            val written = revealOppo(start, seats, ht)
            if (written == null) {
                search.near(listOf("could not write the opponent's cards into hidden slots: $lastError"))
                continue
            }
            for ((v, revealed) in oppoLibraryVariants(written, seats, ht).withIndex()) {
                val readies = if (ht.active == "user" || ht.drawn.isNotEmpty()) {
                    val restacked = restackUser(revealed, seats, ht, laterDraws(spec, i))
                        ?: revealed.also { search.tracer?.line("restack failed: $lastError") }
                    listOf(restacked) + if (ht.active == "user") {
                        listOfNotNull(userLibraryVariant(restacked, seats, ht, laterDraws(spec, i))) +
                            (if (withDrawOrders) drawOrders(ht) else emptyList())
                                .mapNotNull { restackUser(revealed, seats, it, laterDraws(spec, i)) }
                                .filter { it.getLibrary(seats.user) != restacked.getLibrary(seats.user) }
                    } else emptyList()
                } else listOf(revealed)
                for ((u, ready) in readies.withIndex()) {
                    search.tracer?.line("start ${starts++}: user hand ${ready.getHand(seats.user).map { snapshotter.name(ready, it) }}, " +
                        "oppo hand ${ready.getHand(seats.oppo).map { snapshotter.name(ready, it) }}, " +
                        "user library top ${ready.getLibrary(seats.user).take(3).map { snapshotter.name(ready, it) }}")
                    search.tracer?.line("       unseen: ${snapshotter.hidden(ready, seats)}")
                    revealNote?.let { search.tracer?.line("       opponent: $it") }
                    // a library variant is a group of its own: its search must not wait on the other's;
                    // so are the non-active player's untapped lands when they cast this half-turn
                    val other = if (ht.active == "user") "oppo" else "user"
                    val lands = if (ht.instants[other].orEmpty().isNotEmpty() || ht.flash[other].orEmpty().isNotEmpty()) {
                        untappedLands(ready, seats.of(other))
                    } else emptyList()
                    groups.getOrPut(snapshotter.hidden(ready, seats) + lands + "library variant $v/$u") { mutableListOf() } +=
                        ArrayDeque(listOf(Node(ready, Plan.of(ht), line)))
                }
            }
        }
        // Groups of start states that differ in what the snapshot cannot see (a land's colour,
        // an aura's host) advance in turn, a slice of nodes each, so that one with many
        // equivalent lines cannot use up the budget or the end states before the others are
        // looked at. Within a group the starts are near-duplicates and are searched one by one.
        while (search.nodes < nodeBudget && search.ends.size < MAX_ENDS &&
            groups.values.any { g -> g.any { it.isNotEmpty() } }) {
            for ((k, group) in groups.values.withIndex()) {
                val stack = group.firstOrNull { it.isNotEmpty() } ?: continue
                search.tracer?.line("-- group $k")
                search.nodeCap = minOf(nodeBudget, search.nodes + SLICE_NODES)
                searchHalfTurn(stack, ht, i + 1, seats, search)
            }
        }
        if (tracing) tracer!!.end(search.nodes, search.ends.size, search.closest)
        return search
    }

    // =========================================================================
    // Search
    // =========================================================================

    /** Advances one start state's depth-first search until its [stack] empties or [Search.nodeCap]. */
    private fun searchHalfTurn(stack: ArrayDeque<Node>, ht: HalfTurnSpec, turn: Int, seats: Seats, search: Search) {
        while (stack.isNotEmpty() && search.nodes < search.nodeCap && search.ends.size < MAX_ENDS) {
            val node = stack.removeLast().let { n ->
                impulseTop(n.state, seats, search)?.let { n.copy(state = it) } ?: n
            }.let { n -> manifestTop(n.state, seats, ht, search)?.let { n.copy(state = it) } ?: n }
            // The opponent's impulse may or may not have exiled the cards they play next: the line
            // where it did is searched first, then this one.
            if (node.state !in search.branched) {
                oppoImpulse(node, ht, seats, search)?.let { exiled ->
                    search.branched += listOf(node.state, exiled)
                    stack.addLast(node)
                    stack.addLast(node.copy(state = exiled))
                    continue
                }
            }
            search.nodes++
            val s = node.state
            if (s.gameOver) {
                if (ht.last) search.end(s, node.line)
                continue
            }
            // The last half-turn's snapshot was taken when the game ended, possibly mid-turn.
            if (ht.last && node.plan.done && s.pendingDecision == null) {
                val diff = snapshotter.diff(snapshotter.take(s, seats), ht.eot)
                if (diff.isEmpty()) {
                    search.end(s, node.line)
                    continue
                }
                search.near(diff)
            }
            val notes = search.tracer?.takeIf { it.wants(search.nodes) }?.let { t ->
                t.line(t.header(search.nodes, s, seats, snapshotter, describe(node.plan)))
                mutableListOf<String>()
            }
            val children = expand(node, ht, seats, notes)
            notes?.forEach { search.tracer!!.line(it) }
            // Push in reverse so the first child — a recorded action, passes come last — is
            // explored first.
            val active = seats.of(ht.active)
            for (child in children.asReversed().map { effectLands(node, it, active) }) {
                if (child.state.turnNumber > turn && !child.state.gameOver) {
                    // This action ended the turn, so the parent is the end-of-turn position.
                    if (!child.plan.done) {
                        search.near(listOf("turn ended with recorded actions left: ${describe(child.plan)}"))
                        continue
                    }
                    val diff = snapshotter.diff(snapshotter.take(s, seats), ht.eot)
                    if (diff.isEmpty()) {
                        search.tracer?.line("    end check: matches the snapshot")
                        search.end(child.state, child.line)
                    } else {
                        search.near(diff)
                        search.tracer?.line("      board: ${snapshotter.board(s, seats)}")
                    }
                } else {
                    stack.addLast(child)
                }
            }
        }
    }

    /**
     * 17Lands counts a land an effect put onto the battlefield (Spelunking's "put a land card from
     * your hand onto the battlefield") among the lands played, so such a land uses up a planned land
     * drop of its name, as a [PlayLand] does.
     */
    private fun effectLands(parent: Node, child: Node, player: EntityId): Node {
        val before = parent.state.controlledBattlefield(player).toSet()
        val entered = child.state.controlledBattlefield(player).filter {
            it !in before && !snapshotter.isToken(child.state, it) && child.state.projectedState.hasType(it, "LAND")
        }
        val played = parent.plan.lands.values.sum() - child.plan.lands.values.sum()
        var plan = child.plan
        for (id in entered.drop(played)) {
            val name = snapshotter.name(child.state, id) ?: continue
            if (plan.canPlayLand(name)) plan = plan.playLand(name)
        }
        return if (plan === child.plan) child else child.copy(plan = plan)
    }

    /**
     * The children of [node]. With [notes] (tracing), also says for every legal action whether the
     * plan allowed it and, for each variant tried, whether the engine took it or why it refused.
     */
    /**
     * Up to [beamWidth] distinct states from [ends], one per [groupOf] group ([unseen]) in turn, in
     * the order the search found them.
     */
    private fun pickBeam(ends: List<Kept>, groupOf: (GameState) -> List<String>): List<Kept> {
        val groups = ends.distinctBy { StateProgress.digest(it.state) }.groupBy { groupOf(it.state) }.values
        val out = mutableListOf<Kept>()
        var round = 0
        while (out.size < beamWidth && groups.any { round < it.size }) {
            for (g in groups) if (round < g.size && out.size < beamWidth) out += g[round]
            round++
        }
        return out
    }

    /**
     * [Snapshotter.hidden], plus which lands [keepMana]'s side left untapped: hidden leaves tapped
     * lands out because they untap next turn, but that side's stay tapped through the next half-turn.
     */
    private fun unseen(s: GameState, seats: Seats): List<String> =
        snapshotter.hidden(s, seats) + keepMana?.let { untappedLands(s, seats.of(it)) }.orEmpty()

    private fun untappedLands(s: GameState, player: EntityId): List<String> =
        s.controlledBattlefield(player)
            .filter { s.projectedState.hasType(it, "LAND") && s.getEntity(it)?.has<TappedComponent>() != true }
            .mapNotNull { snapshotter.name(s, it) }.sorted().map { "untapped $it" }

    private fun expand(node: Node, ht: HalfTurnSpec, seats: Seats, notes: MutableList<String>? = null): List<Node> {
        val s = node.state
        s.pendingDecision?.let { return decide(node, it, ht, notes) }
        val player = s.priorityPlayerId ?: return emptyList<Node>().also { notes?.add("  no priority player") }
        val side = seats.sideOf(player)
        val active = side == ht.active
        val plan = node.plan
        val legal = enumerator.enumerate(s, player, EnumerationMode.ACTIONS_ONLY)
        val out = mutableListOf<Node>()
        var pass: PassPriority? = null
        fun skip(la: LegalAction, why: String) = notes?.add("  - ${la.description} [${la.actionType}]: $why")
        // untapped permanents with a mana ability, lands first
        val manaSources = legal.filter { it.isManaAbility }.mapNotNull { (it.action as? ActivateAbility)?.sourceId }
            .distinct().filter { s.getEntity(it)?.has<TappedComponent>() != true }
            .sortedBy { if (s.projectedState.hasType(it, "LAND")) 0 else 1 }
        // mana abilities that sacrifice their source (Treasure), which no payment strategy uses
        val sacrificeMana = legal.filter { it.isManaAbility && it.action is ActivateAbility && "Sacrifice" in it.description }
            .distinctBy { snapshotter.name(s, (it.action as ActivateAbility).sourceId) + it.description }
            .flatMap { la -> List(legal.count { it.description == la.description && it.isManaAbility }) { la } }
        // Copies of a card in hand are interchangeable: try one land drop or cast per name and mode.
        val hand = s.getHand(player).toSet()
        val tried = mutableSetOf<String>()
        var cast = false   // whether a planned spell was cast from this node
        for (la in legal) {
            val action = la.action
            val card = (action as? PlayLand)?.cardId ?: (action as? CastSpell)?.cardId
            if (card != null && card in hand && !tried.add("${snapshotter.name(s, card)}|${la.description}")) continue
            when {
                action is PassPriority -> pass = action
                action is PlayLand -> {
                    val name = snapshotter.name(s, action.cardId) ?: continue
                    when {
                        !active -> skip(la, "not the active player")
                        !plan.canPlayLand(name) -> skip(la, "not in the plan")
                        else -> tryAll(s, la, listOf(action), notes).forEach { (st, a) -> out += Node(st, plan.playLand(name), node.line.then(s, a, PLAN)) }
                    }
                }
                // The opponent's face-down casts are logged unnamed: [revealOppo] gave them the card
                // a later turn-up or death showed, or one that can be cast so. (The user's are logged
                // by name and planned as casts.)
                action is CastSpell && action.castFaceDown && side in plan.faceDown -> {
                    val name = snapshotter.name(s, action.cardId) ?: continue
                    when {
                        !plan.canCastFaceDown(side, name) -> skip(la, "not the planned face-down card")
                        !la.affordable -> skip(la, "face down, NOT AFFORDABLE")
                        else -> {
                            val after = plan.castFaceDown(side, name)
                            val more = after.spells[side].orEmpty().isNotEmpty() || after.faceDown[side].orEmpty().isNotEmpty() ||
                                after.turnUps[side].orEmpty().isNotEmpty() || side == keepMana
                            tryAll(s, la, withManaChoices(s, la, action, more, manaSources), notes)
                                .forEach { (st, a) -> out += Node(st, after, node.line.then(s, a, PLAN)) }
                        }
                    }
                }
                action is TurnFaceUp -> {
                    val name = snapshotter.name(s, action.sourceId) ?: continue
                    if (!plan.canTurnUp(side, name)) skip(la, "not in the plan")
                    else tryAll(s, la, withX(la, action), notes).forEach { (st, a) -> out += Node(st, plan.turnUp(side, name), node.line.then(s, a, PLAN)) }
                }
                action is CastSpell -> {
                    val name = snapshotter.name(s, action.cardId) ?: continue
                    when {
                        !plan.canCast(side, name) -> skip(la, "not in the plan")
                        !la.affordable -> skip(la, "in the plan, NOT AFFORDABLE")
                        else -> {
                            val after = plan.cast(side, name)
                            // which lands pay only matters if this side casts again this half-turn,
                            // or in the next one while these lands are still tapped, or if the spell
                            // asks what was spent (Wistfulness: "if {G}{G} was spent to cast it")
                            val more = after.spells[side].orEmpty().isNotEmpty() || side == keepMana ||
                                def(name)?.oracleText?.contains("was spent to cast") == true
                            val variants = withTargets(s, la, player)
                                .flatMap { withX(la, it) }
                                .flatMap { withCostPayments(s, plan, la, it) }
                                .flatMap { withManaChoices(s, la, it, more, manaSources) }
                            val taken = tryAll(s, la, variants, notes)
                            taken.forEach { (st, a) -> out += Node(st, after, node.line.then(s, a, PLAN)) }
                            // (no variants at all: no target or cost payment the engine accepts)
                            if (taken.isEmpty() && variants.isNotEmpty()) floatThenCast(s, la, variants.first(), sacrificeMana, notes).forEach { (st, steps) ->
                                out += Node(st, after, node.line.then(steps, PLAN))
                            }
                            if (out.any { it.plan === after }) cast = true
                        }
                    }
                }
                action is ActivateAbility || action is TypecycleCard || action is CycleCard -> when {
                    la.isManaAbility -> {}
                    else -> {
                        val i = plan.activation(side, la.description)
                        if (i < 0) skip(la, "not in the plan")
                        else {
                            val variants = withTargets(s, la, player).flatMap { withX(la, it) }
                                .flatMap { withCostPayments(s, plan, la, it) }
                            tryAll(s, la, variants, notes).forEach { (st, a) -> out += Node(st, plan.activate(side, i), node.line.then(s, a, PLAN)) }
                        }
                    }
                }
                action is PlotCard -> {
                    val name = snapshotter.name(s, action.cardId) ?: continue
                    if (!plan.canPlot(side, name)) skip(la, "not in the plan")
                    else tryAll(s, la, listOf(action), notes).forEach { (st, a) -> out += Node(st, plan.plot(side, name), node.line.then(s, a, PLAN)) }
                }
                action is UnlockRoomDoor -> {
                    // "Unlock Painter's Studio ({2}{R})"
                    val door = la.description.removePrefix("Unlock ").substringBeforeLast(" (")
                    if (!plan.canUnlock(side, door)) skip(la, "not in the plan")
                    else tryAll(s, la, listOf(action), notes).forEach { (st, a) -> out += Node(st, plan.unlock(side, door), node.line.then(s, a, PLAN)) }
                }
                action is CrewVehicle || action is SaddleMount -> {
                    // 17Lands logs the keyword with its number ("Crew 2"), not the Vehicle
                    val keyword = if (action is CrewVehicle) "Crew" else "Saddle"
                    val i = plan.activation(side, "$keyword ${la.tapForPowerRequired}")
                    if (i < 0) skip(la, "not in the plan")
                    else tryAll(s, la, tapForPower(s, la), notes).forEach { (st, a) -> out += Node(st, plan.activate(side, i), node.line.then(s, a, PLAN)) }
                }
                la.actionType == "DeclareAttackers" -> when {
                    !active || plan.attacksDone -> skip(la, "attacks already declared or not the active player")
                    else -> {
                        val options = attacks(s, la, player, seats, plan)
                        if (options.isEmpty()) {
                            skip(la, "no attackers match ${plan.attacked} among ${names(s, la.validAttackers)}")
                        }
                        tryAll(s, la, options, notes).forEach { (st, a) -> out += Node(st, plan.copy(attacksDone = true), node.line.then(s, a, PLAN)) }
                    }
                }
                la.actionType == "DeclareBlockers" -> when {
                    active || plan.blocksDone -> skip(la, "blocks already declared or the active player")
                    else -> {
                        val options = blocks(s, la, player, plan)
                        if (options.isEmpty()) {
                            skip(la, "no blockers match ${plan.blocking} -> ${plan.blocked} among " +
                                "${names(s, la.validBlockers)}")
                        }
                        tryAll(s, la, options, notes).forEach { (st, a) -> out += Node(st, plan.copy(blocksDone = true), node.line.then(s, a, PLAN)) }
                    }
                }
                else -> skip(la, "not modelled")
            }
        }
        // A planned spell nobody could cast here may need mana the engine's solver does not use —
        // a creature's "add one mana for each colour", Springleaf Drum's "tap a creature" — so the
        // search may activate such a source first and cast from the pool at the next node (in its
        // own main phase: mana floated elsewhere empties before a sorcery-speed spell can use it).
        val mainPhase = s.step == Step.PRECOMBAT_MAIN || s.step == Step.POSTCOMBAT_MAIN
        // (not while a recorded land drop is available: playing the land first never costs a line)
        val landFirst = legal.any { la ->
            (la.action as? PlayLand)?.let { snapshotter.name(s, it.cardId) }?.let(plan::canPlayLand) == true
        }
        // A Treasure is floated even when a cast went through: a modal spell picks its modes before
        // paying, and only then finds that autopay will not sacrifice.
        // Planned spells in hand the enumerator left out: no payment path it knows affords them.
        val offered = legal.mapNotNull { (it.action as? CastSpell)?.cardId?.let { id -> snapshotter.name(s, id) } }.toSet()
        val held = s.getHand(player).mapNotNull { snapshotter.name(s, it) }.toSet()
        val unoffered = plan.spells[side].orEmpty().keys.filter { it in held && it !in offered }
        // The non-active player's planned spells are instants and flash: they float only when
        // one of them is unaffordable to the engine (a filter is their only source of a colour).
        val floatFor = when {
            active && mainPhase && s.stack.isEmpty() && !landFirst -> plan.spells[side].orEmpty().keys
            !active -> unoffered
            else -> emptyList()
        }
        if (floatFor.isNotEmpty()) {
            val colours = floatFor.flatMap { spellColours(it) }.distinct()
            for (la in legal.filter { it.isManaAbility && it.action is ActivateAbility }
                .filterNot { s.projectedState.hasType((it.action as ActivateAbility).sourceId, "LAND") }
                .filter { !cast || "Sacrifice" in it.description }
                .distinctBy { snapshotter.name(s, (it.action as ActivateAbility).sourceId) + it.description }) {
                val base = la.action as ActivateAbility
                val choices = if (la.requiresManaColorChoice) {
                    colours.filter { la.availableManaColors?.contains(it) != false }.map { base.copy(manaColorChoice = it) }
                } else listOf(base)
                // a filter ("{1}: Add {B}, {G}, or {U}") pays with a land the spell may need: try each
                val cost = la.manaCostString?.let { runCatching { ManaCost.parse(it) }.getOrNull() }
                val lands = manaSources.filter { s.projectedState.hasType(it, "LAND") }
                val payments = listOf<PaymentStrategy>(PaymentStrategy.AutoPay) +
                    if (cost != null && cost.cmc in 1..lands.size) {
                        distinctByName(s, subsets(lands, cost.cmc, cost.cmc)).map { PaymentStrategy.Explicit(it) }
                    } else emptyList()
                val variants = choices.flatMap { c -> payments.map { c.copy(paymentStrategy = it) } }
                    .flatMap { withCostPayments(s, plan, la, it) }.take(MAX_PAYMENT_OPTIONS)
                tryAll(s, la, variants, notes).forEach { (st, a) -> out += Node(st, plan, node.line.then(s, a, SEARCH)) }
            }
        }
        if (notes != null && unoffered.isNotEmpty()) {
            val sources = legal.filter { it.isManaAbility }.mapNotNull { (it.action as? ActivateAbility)?.sourceId }
                .distinct().filter { s.getEntity(it)?.has<TappedComponent>() != true }.map { snapshotter.name(s, it) }
            notes.add("  ! planned $unoffered in hand but not offered (unaffordable to the engine); untapped mana sources $sources")
        }
        pass?.let { p ->
            val passed = apply(s, p)
            if (passed != null) out += Node(passed, plan, node.line.then(s, p, SEARCH)) else notes?.add("  ! pass refused: $lastError")
        }
        if (out.isEmpty()) {
            // A mandatory action the plan does not model (damage assignment order, a second
            // combat's declaration): let each legal action through.
            notes?.add("  fallback: trying the first $MAX_FALLBACK_ACTIONS legal actions")
            for (la in legal.take(MAX_FALLBACK_ACTIONS)) {
                tryAll(s, la, listOf(la.action), notes).forEach { (st, a) -> out += Node(st, plan, node.line.then(s, a, SEARCH)) }
            }
        }
        return out
    }

    /**
     * Casts [cast] after activating 1..N of the [floats] (Treasures) for mana. The engine's mana
     * solver never sacrifices a source, so autopay and explicit payments cannot use a Treasure,
     * while the enumerator counts it towards what is affordable; a player floats the mana first.
     * Each Treasure makes one of the cost's colours.
     */
    private fun floatThenCast(
        s: GameState, la: LegalAction, cast: GameAction, floats: List<LegalAction>, notes: MutableList<String>?,
    ): List<Pair<GameState, List<Pair<GameState, GameAction>>>> {
        if (floats.isEmpty()) return emptyList()
        val cost = la.manaCostString?.let { runCatching { ManaCost.parse(it) }.getOrNull() } ?: return emptyList()
        val colours = cost.symbols.flatMap { it.colors }.distinct()
            .ifEmpty { listOfNotNull(floats.first().availableManaColors?.firstOrNull()) }
        val out = mutableListOf<Pair<GameState, List<Pair<GameState, GameAction>>>>()
        val refusals = mutableListOf<String>()
        for (k in 1..minOf(floats.size, cost.cmc)) {
            var picks: List<List<com.wingedsheep.sdk.core.Color?>> = listOf(emptyList())
            repeat(k) { picks = picks.flatMap { p -> colours.ifEmpty { listOf(null) }.map { p + it } }.take(MAX_TARGET_COMBOS) }
            for (pick in picks) {
                var st: GameState? = s
                val steps = mutableListOf<Pair<GameState, GameAction>>()
                // the same Treasure's ability re-enumerated each time: it names the next untapped one
                for (c in pick) {
                    val cur = st ?: break
                    val next = enumerator.enumerate(cur, la.action.playerId, EnumerationMode.ACTIONS_ONLY)
                        .firstOrNull { it.isManaAbility && it.description == floats.first().description }
                    val activation = (next?.action as? ActivateAbility)?.copy(manaColorChoice = c)
                    st = activation?.let { apply(cur, it) }
                    if (activation != null) steps += cur to activation
                }
                val done = st?.let { apply(it, cast) }
                if (done != null) out += done to (steps + (st!! to cast)) else refusals += lastError ?: "?"
            }
            if (out.isNotEmpty()) break
        }
        notes?.add("  + (float Treasure mana) ${la.description}: ${out.size} taken" +
            if (out.isEmpty()) "; refused: ${refusals.distinct().take(3)}" else "")
        return out
    }

    /** Applies each variant of [la]; with [notes], reports how many the engine took and why it refused the rest. */
    private fun tryAll(s: GameState, la: LegalAction, variants: List<GameAction>, notes: MutableList<String>?): List<Pair<GameState, GameAction>> {
        val taken = mutableListOf<Pair<GameState, GameAction>>()
        val refusals = mutableListOf<String>()
        for (v in variants) {
            val next = apply(s, v)
            if (next != null) taken += next to v else refusals += lastError ?: "?"
        }
        notes?.add(buildString {
            append("  + ${la.description} [${la.actionType}]: ${taken.size}/${variants.size} taken")
            if (refusals.isNotEmpty()) {
                append("; refused: ")
                append(refusals.groupingBy { it }.eachCount().entries.joinToString("; ") { (why, n) ->
                    if (n > 1) "$why (x$n)" else why
                })
            }
        })
        return taken
    }

    /** The colours in [name]'s mana cost: hybrid halves, twobrid and Phyrexian symbols included. */
    private fun spellColours(name: String): List<com.wingedsheep.sdk.core.Color> =
        def(name)?.manaCost?.symbols.orEmpty().flatMap { it.colors }

    private fun names(s: GameState, ids: List<EntityId>?): List<String> =
        ids.orEmpty().map { id -> snapshotter.name(s, id)?.let { if (snapshotter.isToken(s, id)) "token:$it" else it } ?: "?" }

    private fun apply(s: GameState, action: GameAction): GameState? {
        val r = try {
            processor.process(s, action).result
        } catch (e: Exception) {
            lastError = "exception ${e::class.simpleName}: ${e.message?.take(160)}"
            return null
        }
        lastError = r.error
        return if (r.error != null) null else r.state
    }

    // =========================================================================
    // Choices: targets, combat, decisions
    // =========================================================================

    /** An {X} cost's values, largest affordable first (an activation left at no X cannot pay). */
    private fun withX(la: LegalAction, action: GameAction): List<GameAction> {
        if (!la.hasXCost) return listOf(action)
        val xs = (la.maxAffordableX ?: la.minX).downTo(la.minX).take(MAX_NUMBER_OPTIONS)
        return when (action) {
            is CastSpell -> if (action.xValue != null) listOf(action) else xs.map { action.copy(xValue = it) }
            is ActivateAbility -> if (action.xValue != null) listOf(action) else xs.map { action.copy(xValue = it) }
            is TurnFaceUp -> if (action.xValue != null) listOf(action) else xs.map { action.copy(xValue = it) }
            else -> listOf(action)
        }
    }

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

    /**
     * One action per way to pay a cast's additional cost, or an ability's ([LegalAction.additionalCostInfo]):
     * behold a Kithkin, blight a creature, sacrifice, discard. Unlike the AI, which takes the first
     * candidate, the search branches, because which creature gets the -1/-1 counter changes the
     * game. The "or pay {2}" alternative arrives as its own legal action.
     */
    private fun withCostPayments(s: GameState, plan: Plan, la: LegalAction, action: GameAction): List<GameAction> {
        val info = la.additionalCostInfo ?: return listOf(action)
        val base = when (action) {
            is CastSpell -> action.additionalCostPayment
            is ActivateAbility -> action.costPayment
            else -> return listOf(action)
        } ?: AdditionalCostPayment()
        // One choice per distinct set of names (copies are interchangeable), cheapest [rank] first.
        fun choose(ids: List<EntityId>, n: Int, rank: (EntityId) -> Int = { 0 }): List<List<EntityId>> =
            subsets(ids.sortedBy(rank), n, n, cap = MAX_TAP_SETS)
                .distinctBy { p -> p.map { "${snapshotter.name(s, it)}@${s.getBattlefield().contains(it)}" }.sorted() }
                .sortedBy { p -> p.sumOf(rank) }
        // Tap what nothing else needs first: lands still pay for spells, planned attackers attack.
        val attackers = plan.attacked.toSet()
        fun tapRank(id: EntityId) = when {
            !plan.attacksDone && snapshotter.name(s, id) in attackers -> 4
            s.projectedState.hasType(id, "LAND") -> 2
            else -> 1
        }
        val payments = when (info.costType) {
            "Blight" -> info.validBlightTargets.map { base.copy(blightTargets = listOf(it)) }
            // "choose a creature you control or reveal a creature card" (Monstrous Emergence) pays like behold
            "Behold", "ChooseEntity" -> choose(info.validBeholdTargets, info.beholdCount).map { base.copy(beheldCards = it) }
            "RevealCard" -> choose(info.validRevealTargets, info.revealCount).map { base.copy(revealedCards = it) }
            "Casualty" -> listOf(base) + choose(info.validSacrificeTargets, 1).map { base.copy(sacrificedPermanents = it) }
            "Craft" -> choose(info.validCraftMaterials, info.craftMinCount).map { base.copy(exiledCards = it) }
            "TapPermanents" -> choose(info.validTapTargets, info.tapCount, ::tapRank).map { base.copy(tappedPermanents = it) }
            "DiscardCard" -> choose(info.validDiscardTargets, info.discardCount).map { base.copy(discardedCards = it) }
            "SacrificePermanent" -> choose(info.validSacrificeTargets, info.sacrificeCount)
                .map { base.copy(sacrificedPermanents = it) }
            "BouncePermanent" -> choose(info.validBounceTargets, info.bounceCount).map { base.copy(bouncedPermanents = it) }
            "ExileFromGraveyard" -> listOf(base.copy(exiledCards = info.validExileTargets.take(info.exileMinCount)))
            else -> return listOf(action)
        }
        return payments.take(MAX_TARGET_COMBOS).map {
            if (action is CastSpell) action.copy(additionalCostPayment = it) else (action as ActivateAbility).copy(costPayment = it)
        }
    }

    /**
     * Crew or saddle payments: the minimal sets of creatures that reach the required power (tapping
     * more never matches a record better), one per distinct set of names. Which creatures tap
     * decides who can still attack or block, so each is a separate line.
     */
    private fun tapForPower(s: GameState, la: LegalAction): List<GameAction> {
        val need = la.tapForPowerRequired ?: return emptyList()
        val pool = la.tapForPowerCreatures.orEmpty().filter { it.power > 0 }
        return subsets(pool, 1, minOf(need, pool.size), cap = MAX_TAP_SETS)
            .filter { p -> p.sumOf { it.power }.let { sum -> sum >= need && p.none { sum - it.power >= need } } }
            .distinctBy { p -> p.map { snapshotter.name(s, it.entityId) ?: "" }.sorted() }
            .take(MAX_TARGET_COMBOS)
            .map { p ->
                val ids = p.map { it.entityId }
                when (val a = la.action) {
                    is CrewVehicle -> a.copy(crewCreatures = ids)
                    is SaddleMount -> a.copy(saddleCreatures = ids)
                    else -> a
                }
            }
    }

    /**
     * Ways to pay the mana: autopay first, then convoke with 1..N creatures (which creatures tap
     * decides who can block next turn), then explicit sets of [sources] (lands first) — when [more]
     * casts follow this half-turn, because autopay can tap the colour a later spell needed, and
     * whenever a non-land source is at hand, because autopay never sacrifices a Treasure (while the
     * enumerator counts it as affordable). One option per distinct set of names.
     */
    private fun withManaChoices(
        s: GameState, la: LegalAction, action: GameAction, more: Boolean, sources: List<EntityId>,
    ): List<GameAction> {
        val cast = action as? CastSpell ?: return listOf(action)
        val cost = la.manaCostString?.let { runCatching { ManaCost.parse(it) }.getOrNull() }
        val out = mutableListOf<GameAction>(cast)
        val convoke = la.convokeCreatures.orEmpty()
        if (la.hasConvoke && convoke.isNotEmpty() && cost != null) {
            // Coloured-first — each coloured symbol by a creature of that colour, then hybrid ones
            // by a creature of either colour, then generic — is the only option here that pays
            // coloured mana with creatures ({4}{W}{W}, Merrow Skyswimmer's {3}{W/U}{W/U}).
            val coloured = mutableMapOf<EntityId, ConvokePayment>()
            val unused = convoke.toMutableList()
            val pips = cost.symbols.filterIsInstance<ManaSymbol.Colored>().map { listOf(it.color) } +
                cost.symbols.filterIsInstance<ManaSymbol.Hybrid>().map { listOf(it.color1, it.color2) }
            for (colours in pips) {
                val i = unused.indexOfFirst { c -> colours.any { it in c.colors } }
                if (i >= 0) {
                    val creature = unused.removeAt(i)
                    coloured[creature.entityId] = ConvokePayment(colours.first { it in creature.colors })
                }
            }
            repeat(minOf(cost.genericAmount, unused.size)) { coloured[unused.removeAt(0).entityId] = ConvokePayment() }
            if (coloured.isNotEmpty()) {
                val payment = (cast.alternativePayment ?: AlternativePaymentChoice.NONE).copy(convokedCreatures = coloured)
                out += cast.copy(alternativePayment = payment)
            }
            val ids = convoke.map { it.entityId }
            for (k in 1..minOf(cost.cmc, ids.size)) {
                for (pick in distinctByName(s, subsets(ids, k, k, cap = MAX_PAYMENT_OPTIONS * 4))) {
                    val payment = (cast.alternativePayment ?: AlternativePaymentChoice.NONE)
                        .copy(convokedCreatures = pick.associateWith { ConvokePayment() })
                    out += cast.copy(alternativePayment = payment)
                }
            }
        }
        val nonLand = sources.any { !s.projectedState.hasType(it, "LAND") }
        if ((more || nonLand) && cost != null && cost.cmc in 1..sources.size) {
            for (pick in distinctByName(s, subsets(sources, cost.cmc, cost.cmc, cap = MAX_PAYMENT_OPTIONS * 4))) {
                out += cast.copy(paymentStrategy = PaymentStrategy.Explicit(pick))
            }
        }
        return out.take(MAX_PAYMENT_OPTIONS)
    }

    private fun distinctByName(s: GameState, picks: List<List<EntityId>>): List<List<EntityId>> =
        picks.distinctBy { pick -> pick.map { snapshotter.name(s, it) }.sortedBy { it ?: "" } }

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

    private fun decide(node: Node, d: PendingDecision, ht: HalfTurnSpec, notes: MutableList<String>? = null): List<Node> {
        val s = node.state
        // the AI's responder answers what the search does not enumerate
        var how = SEARCH
        fun heuristic(s: GameState, d: PendingDecision) = this@Reconstructor.heuristic(s, d).also { how = AI }
        val responses: List<DecisionResponse> = when (d) {
            is YesNoDecision -> listOf(YesNoResponse(d.id, true), YesNoResponse(d.id, false))
            is ChooseModeDecision ->
                if (d.minModes == 1 && d.maxModes == 1) {
                    d.modes.filter { it.available }.map { ModesChosenResponse(d.id, listOf(it.index)) }
                } else heuristic(s, d)
            is ChooseNumberDecision ->
                (d.minValue..minOf(d.maxValue, d.minValue + MAX_NUMBER_OPTIONS)).map { NumberChosenResponse(d.id, it) }
            is ChooseColorDecision -> d.availableColors.map { ColorChosenResponse(d.id, it) }
            is ChooseOptionDecision -> optionOrder(s, d, node.plan).map { OptionChosenResponse(d.id, it) }
            is ChooseTargetsDecision -> targetResponses(d)
            is SelectCardsDecision ->
                if (!d.ordered && d.maxSelections <= 2) {
                    // Small picks (including "you may": min 0) are enumerated, the recorded tutored
                    // card first — Eclipsed Kithkin's "reveal a Kithkin, Forest, or Plains" — then
                    // what the snapshot shows the chooser gaining (Midnight Tilling's returned card,
                    // Evolving Wilds' basic), unless the picks are binned (a surveil).
                    val where = (d.selectedLabel ?: d.prompt).lowercase()
                    val binned = BINNED.any { it in where }
                    val wanted = ht.tutored.toSet() + if (binned) emptySet() else gained(s, d.playerId, ht)
                    cardPicks(s, d.id, d.options, d.minSelections, d.maxSelections, wanted) ?: heuristic(s, d)
                } else heuristic(s, d)
            is SearchLibraryDecision -> tutor(s, d, ht)
                ?: d.takeIf { it.maxSelections <= 2 }
                    ?.let { cardPicks(s, d.id, d.options, d.minSelections, d.maxSelections, gained(s, d.playerId, ht)) }
                ?: heuristic(s, d)
            is CombatResolutionDecision -> combatSplits(d) ?: heuristic(s, d)
            else -> heuristic(s, d)
        }
        val out = mutableListOf<Node>()
        val refusals = mutableListOf<String>()
        for (r in responses) {
            val submit = SubmitDecision(d.playerId, r)
            val next = apply(s, submit)
            if (next != null) out += Node(next, node.plan, node.line.then(s, submit, how)) else refusals += lastError ?: "?"
        }
        notes?.add("  decision ${d::class.simpleName} (${d.context.sourceName}: ${d.prompt.take(80)}): " +
            "${out.size}/${responses.size} responses taken" +
            if (refusals.isEmpty()) "" else "; refused: ${refusals.distinct().joinToString("; ")}")
        return out
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

    /**
     * Every pick of [min]..[max] of [options], those with the most [wanted] names first. A short list
     * is enumerated as it is; a long one (a library searched for a basic land: 17Lands names no card
     * for an Evolving Wilds) one pick per distinct set of names, up to [MAX_SELECT_OPTIONS] picks.
     */
    private fun cardPicks(
        s: GameState, id: String, options: List<EntityId>, min: Int, max: Int, wanted: Set<String>,
    ): List<DecisionResponse>? {
        val picks = if (options.size <= MAX_SELECT_OPTIONS) subsets(options, min, max) else {
            val reduced = options.groupBy { snapshotter.name(s, it) }.values.flatMap { it.take(max) }
            distinctByName(s, subsets(reduced, min, max, cap = MAX_TAP_SETS))
        }
        return picks.sortedByDescending { pick -> pick.count { snapshotter.name(s, it) in wanted } }
            .take(maxOf(MAX_SELECT_OPTIONS, if (options.size <= MAX_SELECT_OPTIONS) picks.size else 0))
            .map { CardsSelectedResponse(id, it) }
            .ifEmpty { null }
    }

    /**
     * Combat damage: the engine's default split first, then for each creature blocked by (or
     * blocking) several others, one split per creature dealt lethal damage first, the rest in turn
     * (the validator takes any split some damage-assignment order allows). Which blocker of a double
     * block dies is otherwise the heuristic's call.
     */
    private fun combatSplits(d: CombatResolutionDecision): List<DecisionResponse>? {
        val mine = d.edges.filter { it.editableBy == d.playerId }
        val default = mine.associate { it.id to it.amount }
        var options = listOf(default)
        for (edges in mine.groupBy { it.sourceId }.values) {
            val blocks = edges.filter {
                it.direction == DamageEdgeDirection.ATTACKER_TO_BLOCKER || it.direction == DamageEdgeDirection.BLOCKER_TO_ATTACKER
            }
            if (blocks.size < 2) continue
            val drain = edges.firstOrNull { it.isTrampleDrain }
            val splits = blocks.map { first ->
                val order = listOf(first) + (blocks - first)
                var left = first.maximum
                val split = mutableMapOf<String, Int>()
                for (e in order) {
                    val x = minOf(e.lethal, left)
                    split[e.id] = x
                    left -= x
                }
                if (left > 0) split[drain?.id ?: first.id] = (split[drain?.id ?: first.id] ?: 0) + left
                split.toMap()
            }
            options = options.flatMap { o -> splits.map { o + it } }.distinct().take(MAX_COMBAT_OPTIONS)
        }
        val all = (listOf(default) + options).distinct()
        if (all.size == 1) return null
        return all.map { a -> CombatResolutionResponse(d.id, mine.map { DamageEdgeAmount(it.id, a[it.id] ?: 0) }) }
    }

    /** Names [player] has more of at the end of the half-turn than now: non-token permanents, and the user's hand. */
    private fun gained(s: GameState, player: EntityId, ht: HalfTurnSpec): Set<String> {
        val side = Seats.of(s).sideOf(player)
        val user = side == "user"
        val now = Snapshotter.counts(s.controlledBattlefield(player).filterNot { snapshotter.isToken(s, it) }
            .mapNotNull { snapshotter.name(s, it) } +
            if (user) s.getHand(player).mapNotNull { snapshotter.name(s, it) } else emptyList())
        val eot = Snapshotter.counts(ht.eot.battlefield[side].orEmpty() + if (user) ht.eot.userHand else emptyList())
        return eot.filter { (name, k) -> k > (now[name] ?: 0) }.keys
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
            val pool = candidates.filter { snapshotter.matches(s, it, name) }
            val picks = subsets(pool, k, k)
            if (picks.isEmpty()) return emptyList()
            combos = combos.flatMap { prefix -> picks.map { prefix + it } }.take(MAX_COMBAT_OPTIONS)
        }
        return combos
    }

    // =========================================================================
    // Setup and hidden cards
    // =========================================================================

    /**
     * 17Lands does not always list a half-turn's draws in the order they were drawn (Flaring Cinder's
     * rummage drew the Pummeler after the draw step's Forest, logged the other way round), so a
     * half-turn with several draws is also tried with each other card as the draw-step draw.
     */
    private fun drawOrders(ht: HalfTurnSpec): List<HalfTurnSpec> =
        ht.drawn.drop(1).distinct().filter { it != ht.drawn.first() }
            .map { d -> ht.copy(drawn = listOf(d) + (ht.drawn - d)) }

    /** The user's draws after half-turn [i], in order: their own turns' and those off their turn. */
    private fun laterDraws(spec: GameSpec, i: Int): List<String> =
        spec.halfTurns.drop(i + 1).flatMap { it.drawn }

    private fun missingCard(spec: GameSpec): String? =
        (spec.userDeck + spec.oppoKnown).firstOrNull { def(it) == null }

    private fun def(name: String) = registry.getCard(snapshotter.engineName(name))

    private fun oppoDeck(spec: GameSpec): List<String> {
        val basics = spec.oppColors.mapNotNull { BASICS[it] }.ifEmpty { listOf("Plains") }
        val deck = spec.oppoKnown.toMutableList()
        var i = 0
        while (deck.size < DECK_SIZE) deck += basics[i++ % basics.size]
        return deck.map(snapshotter::engineName)
    }

    /**
     * Force the user's opening hand and library to the recorded hand and draw order. A draw inferred
     * off their turn that the deck has no copy left for came from elsewhere (the graveyard) and is
     * left out.
     */
    private fun stackUser(state: GameState, seats: Seats, spec: GameSpec): GameState? {
        val hand = state.getHand(seats.user)
        val library = state.getLibrary(seats.user)
        val rest = spec.userDeck.toMutableList()
        // the user's own draws are certain and come out of the deck first; inferred ones take what is left
        val own = spec.halfTurns.filter { it.active == "user" }.flatMap { it.drawn }
        for (name in spec.openingHand + own) if (!rest.remove(name)) return null
        val draws = spec.halfTurns.flatMap { ht -> if (ht.active == "user") ht.drawn else ht.drawn.filter(rest::remove) }
        if (hand.size != spec.openingHand.size || library.size != draws.size + rest.size) return null
        val order = draws + rest.shuffled(Random(seed))   // index 0 is the top of the library
        return materialize(state, hand.zip(spec.openingHand).toMap() + library.zip(order).toMap())
    }

    /**
     * Put this half-turn's recorded draws on top of the user's library, then its tutored cards (so a
     * "look at the top N" effect finds the card the record says it took), then the draws of the
     * user's [later] half-turns (so such an effect sees what the user really saw). Library effects —
     * Eclipsed Kithkin sending cards to the bottom, surveil, a shuffle — move cards after [stackUser]
     * ran, so the order is re-forced every half-turn.
     *
     * Cards move by reordering the library, which keeps each card's runtime state: a card someone
     * has looked at carries it, and the materializer refuses to rewrite such a card's identity. Only a
     * card of this half-turn that is no longer in the library (surveilled away, say) is written over an
     * unseen slot near the bottom instead — on the user's own half-turns. On the opponent's, the
     * draws are inferred and may have come from the graveyard, so one not in the library is let be.
     */
    private fun restackUser(state: GameState, seats: Seats, ht: HalfTurnSpec, later: List<String>): GameState? {
        val own = ht.active == "user"
        // Past the upkeep, this turn's draw-step draw has already happened.
        val drawn = if (own && state.step.ordinal > Step.UPKEEP.ordinal) ht.drawn.drop(1) else ht.drawn
        val now = drawn + ht.tutored
        val library = state.getLibrary(seats.user)
        if (library.size < now.size) return null
        val rest = library.toMutableList()
        val top = mutableListOf<EntityId?>()
        val absent = mutableMapOf<Int, String>()
        for ((k, name) in (now + later).withIndex()) {
            val i = rest.indexOfFirst { snapshotter.name(state, it) == name }
            when {
                i >= 0 -> top += rest.removeAt(i)
                k < now.size && own -> { absent[top.size] = name; top += null }
                k < now.size -> {}
                else -> break   // a later draw no longer here is its own half-turn's business
            }
        }
        if (absent.size > rest.size) return null
        // unseen slots, bottom first, stand in for the cards that left the library
        val spare = rest.asReversed().filter { state.getEntity(it)?.has<RevealedToComponent>() != true }
        if (spare.size < absent.size) return null
        val fill = absent.keys.zip(spare).toMap()
        rest.removeAll(fill.values.toSet())
        val order = top.mapIndexed { k, id -> id ?: fill.getValue(k) } + rest
        val key = ZoneKey(seats.user, Zone.LIBRARY)
        val reordered = if (order == library) state else state.copy(zones = state.zones + (key to order))
        if (absent.isEmpty()) return reordered
        return materialize(reordered, absent.mapKeys { (k, _) -> fill.getValue(k) })
    }

    /**
     * Make sure the opponent holds the cards the record has them play this half-turn: needed cards
     * already in hand stay, the rest are written over filler hand slots, then over the top of the
     * library (a card drawn this turn can be the one played).
     *
     * A spell with a behold cost ("behold a Goblin or pay {2}") cast without the {2} needs a Goblin
     * in hand when the opponent controls none, and the filler hand holds basics; so one creature of
     * that type is written into a free slot too. It stays in their hand, as a revealed card would.
     * A slot the materializer refuses (a card already revealed, say by a tutor) is skipped.
     */
    private fun revealOppo(given: GameState, seats: Seats, ht: HalfTurnSpec): GameState? {
        val own = if (ht.active == "oppo") ht.lands + ht.creatures + ht.noncreatures + ht.discarded else emptyList()
        val casts = own + ht.instants["oppo"].orEmpty() + ht.flash["oppo"].orEmpty()
        val needed = Snapshotter.counts(casts + ht.plotted["oppo"].orEmpty()).toMutableMap()
        // a card they plotted earlier is cast from exile, not from their hand
        for (id in given.getExile(seats.oppo)) {
            if (given.getEntity(id)?.has<PlottedComponent>() != true) continue
            val name = snapshotter.name(given, id) ?: continue
            needed[name]?.let { if (it > 1) needed[name] = it - 1 else needed.remove(name) }
        }
        val state = unrevealFillers(given, seats.oppo, needed.keys)
        val free = mutableListOf<EntityId>()
        for (id in state.getHand(seats.oppo)) {
            val name = snapshotter.name(state, id)
            val left = name?.let { needed[it] } ?: 0
            if (name != null && left > 0) needed[name] = left - 1 else free += id
        }
        val board = state.controlledBattlefield(seats.oppo).mapNotNull { snapshotter.name(state, it) }
        val held = free.mapNotNull { snapshotter.name(state, it) }
        for ((i, card) in casts.withIndex()) {
            val type = beholdType(card) ?: continue
            // the spell cannot behold itself, but another card cast this turn may still be in hand;
            // a behold that exiles the card (Champion of the Path) may have taken one from hand though
            // one was on the board, and a card cast later cannot have been exiled
            val others = casts.filterIndexed { j, _ -> j != i }
            val exiles = def(card)?.oracleText?.contains("and exile it") == true
            if ((if (exiles) held else board + held + others).none { hasCreatureType(it, type) }) {
                beholdFiller(type)?.let { needed[it] = (needed[it] ?: 0) + 1 }
            }
        }
        // A spell of the user's that has the opponent reveal their hand and discard a nonland card of
        // the user's choice (Auntie's Sentence) finds only filler basics there: a vanilla creature
        // stands in for the card they discarded, which the record never names.
        val userCasts = ht.instants["user"].orEmpty() + if (ht.active == "user") ht.creatures + ht.noncreatures else emptyList()
        val filler = nonlandFiller
        if (filler != null && userCasts.any { name -> def(name)?.oracleText?.let { REVEALS_HAND.containsMatchIn(it) } == true } &&
            held.none { def(it)?.typeLine?.isLand == false }) {
            needed[filler] = (needed[filler] ?: 0) + 1
        }
        // A cycled card of theirs is never named ("Islandcycling {2}"): one that cycles the same way
        // stands in, unless a card in their hand already can.
        for (text in ht.activated["oppo"].orEmpty().filter { "cycling" in it.lowercase() }) {
            val keyword = text.substringAfter(" — ").trim()
            if ((held + needed.keys).none { def(it)?.oracleText?.contains(keyword) == true }) {
                cyclingFiller(keyword)?.let { needed[it] = (needed[it] ?: 0) + 1 }
            }
        }
        // A card they cast face down is never named: one that can be cast so stands in, unless
        // their hand already holds enough.
        if (ht.active == "oppo") {
            val named = ht.faceDownAs["oppo"].orEmpty().filter { it.isNotEmpty() }
            named.forEach { needed[it] = (needed[it] ?: 0) + 1 }
            val short = (ht.faceDown["oppo"] ?: 0) - named.size - held.count { castsFaceDown(it) && it !in named }
            if (short > 0) faceDownFiller?.let { needed[it] = (needed[it] ?: 0) + short }
        }
        val missing = needed.flatMap { (name, k) -> List(k) { name } }
        revealNote = null
        if (missing.isEmpty()) return state
        val slots = (free + state.getLibrary(seats.oppo)).toMutableList()
        val refused = mutableListOf<String>()
        repeat(MAX_SLOT_RETRIES) {
            if (slots.size < missing.size) return null
            val pick = slots.take(missing.size)
            materialize(state, pick.zip(missing).toMap())?.let { written ->
                val zones = pick.map { if (it in free) "hand" else "library ${state.getLibrary(seats.oppo).indexOf(it)}" }
                revealNote = "wrote $missing into $zones" + if (refused.isEmpty()) "" else "; refused: $refused"
                return written
            }
            refused += lastError ?: "?"
            if (lastRefused == null || !slots.remove(lastRefused)) return null
        }
        return null
    }

    /**
     * An opponent's explore or reveal can put one of the harness's own stand-ins (a filler basic, the
     * vanilla creature) into their hand as a revealed card, which the materializer will not rewrite,
     * so it would hold the slot a recorded card needs; so can their own surveil or scry, which leaves
     * a filler they kept on top of their library seen, where the card they draw next must go. Such
     * cards not among [keep], in hand or in the top [LIBRARY_VARIANT_DEPTH] of the library, trade
     * places with unseen cards from the bottom of it: their identity was made up in the first place.
     */
    private fun unrevealFillers(state: GameState, player: EntityId, keep: Set<String>): GameState {
        val made = BASICS.values.toSet() + listOfNotNull(nonlandFiller) - keep
        fun revealed(id: EntityId) = state.getEntity(id)?.has<RevealedToComponent>() == true
        val hand = state.getHand(player).toMutableList()
        val library = state.getLibrary(player).toMutableList()
        var j = library.lastIndex
        var swapped = false
        for (i in hand.indices) {
            if (!revealed(hand[i]) || snapshotter.name(state, hand[i]) !in made) continue
            while (j >= 0 && revealed(library[j])) j--
            if (j < 0) break
            hand[i] = library[j].also { library[j] = hand[i] }
            j--
            swapped = true
        }
        for (i in 0 until minOf(LIBRARY_VARIANT_DEPTH, library.size)) {
            if (!revealed(library[i]) || snapshotter.name(state, library[i]) !in made) continue
            while (j > i && revealed(library[j])) j--
            if (j <= i) break
            library[i] = library[j].also { library[j] = library[i] }
            j--
            swapped = true
        }
        if (!swapped) return state
        return state.copy(zones = state.zones + (ZoneKey(player, Zone.HAND) to hand) + (ZoneKey(player, Zone.LIBRARY) to library))
    }

    /** What [revealOppo] last wrote where, for the trace. */
    private var revealNote: String? = null

    /**
     * The opponent's unseen library is basic lands, so an effect of theirs that reads their top card
     * always finds a land: an explore hands them the land, Elven Farsight's "if it's a creature
     * card, draw a card" never draws. When one of their cards this half-turn, or one of their
     * permanents (a Map token included), explores or reveals the top of their library, the
     * half-turn is also searched with the top [LIBRARY_VARIANT_DEPTH] unseen cards a vanilla creature
     * instead (the draw, then a scry's window before Elven Farsight reveals).
     */
    private fun oppoLibraryVariants(state: GameState, seats: Seats, ht: HalfTurnSpec): List<GameState> {
        val filler = nonlandFiller ?: return listOf(state)
        val oppoCards = ht.instants["oppo"].orEmpty() + ht.flash["oppo"].orEmpty() +
            (if (ht.active == "oppo") ht.creatures + ht.noncreatures else emptyList()) +
            state.controlledBattlefield(seats.oppo).mapNotNull { snapshotter.name(state, it) }
        if (oppoCards.none { name -> def(name)?.oracleText?.let { READS_LIBRARY_TOP.containsMatchIn(it) } == true }) {
            return listOf(state)
        }
        val slots = state.getLibrary(seats.oppo)
            .filter { state.getEntity(it)?.has<RevealedToComponent>() != true && snapshotter.name(state, it) in BASICS.values }
            .take(LIBRARY_VARIANT_DEPTH)
        val variant = materialize(state, slots.associateWith { filler }) ?: return listOf(state)
        return listOf(state, variant)
    }

    /**
     * The user's library is stacked with their recorded draws, so their own explore or top-card
     * reveal finds the next recorded draw; when the card it found was a nonland they binned, the
     * record shows no trace of it. On a half-turn where one of the user's cards can do that, it is
     * also searched with an unseen nonland from the rest of their deck under this turn's draw.
     */
    private fun userLibraryVariant(state: GameState, seats: Seats, ht: HalfTurnSpec, later: List<String>): GameState? {
        val userCards = ht.creatures + ht.noncreatures + ht.instants["user"].orEmpty() +
            state.controlledBattlefield(seats.user).mapNotNull { snapshotter.name(state, it) }
        if (userCards.none { name -> def(name)?.oracleText?.let { READS_LIBRARY_TOP.containsMatchIn(it) } == true }) return null
        val library = state.getLibrary(seats.user)
        val recorded = Snapshotter.counts(ht.drawn + ht.tutored + later)
        val spare = library.lastOrNull { id ->
            val name = snapshotter.name(state, id)
            name != null && name !in recorded && state.getEntity(id)?.has<RevealedToComponent>() != true &&
                def(name)?.typeLine?.isLand == false
        } ?: return null
        val pos = if (state.step.ordinal > Step.UPKEEP.ordinal || ht.drawn.isEmpty()) 0 else 1
        val order = (library - spare).toMutableList().apply { add(minOf(pos, size), spare) }
        return state.copy(zones = state.zones + (ZoneKey(seats.user, Zone.LIBRARY) to order))
    }

    /**
     * Impulse draws. A card the user played from outside their hand ([Search.outside]) most often
     * came off the top of their library through an effect that exiles cards there and lets them be
     * played until the end of the next turn (Burning Curiosity, Kulrath Zealot, Sizzling Changeling
     * dying on the opponent's turn), where the harness had stacked later draws. When such an effect
     * of the user's is on top of the stack, those cards still in the library go on top, then unseen
     * cards no later draw needs (what it exiled that the user never played), up to the number of
     * cards the effect names; the recorded draws stay next. A draw-step draw still to come keeps its
     * place above them. Null when nothing moves.
     */
    /** An impulse draw on top of the stack: its controller, source card name and how many cards it exiles. */
    private data class Impulse(val controller: EntityId, val source: String, val count: Int)

    /** The top of the stack's controller and source card name (canonical). */
    private fun stackTop(s: GameState): Pair<EntityId, String>? {
        val e = s.stack.lastOrNull()?.let { s.getEntity(it) } ?: return null
        val (controller, source) = e.get<SpellOnStackComponent>()?.let { it.casterId to e.get<CardComponent>()?.name }
            ?: e.get<TriggeredAbilityOnStackComponent>()?.let { it.controllerId to it.sourceName }
            ?: e.get<ActivatedAbilityOnStackComponent>()?.let { it.controllerId to it.sourceName }
            ?: return null
        return source?.let { controller to snapshotter.canonical(it) }
    }

    /**
     * Manifest dread (DSK: look at the top two cards of your library, manifest one face down). The
     * user's face-down cards are logged by name, so the record says which card they manifested: a
     * permanent they have more of at the end of the half-turn than now. Unless one is in the top two
     * already, the first such card in their library (whose order past the recorded draws is made up)
     * goes on top before the manifest resolves. Null when nothing moves.
     */
    private fun manifestTop(s: GameState, seats: Seats, ht: HalfTurnSpec, search: Search): GameState? {
        val (controller, source) = stackTop(s) ?: return null
        if (controller != seats.user || def(source)?.oracleText?.contains("manifest dread", ignoreCase = true) != true) return null
        val now = Snapshotter.counts(s.controlledBattlefield(seats.user).filterNot { snapshotter.isToken(s, it) }
            .mapNotNull { snapshotter.name(s, it) })
        val wanted = Snapshotter.counts(ht.eot.battlefield["user"].orEmpty()).filter { (n, k) -> k > (now[n] ?: 0) }.keys
        val library = s.getLibrary(seats.user)
        if (wanted.isEmpty() || library.take(2).any { snapshotter.name(s, it) in wanted }) return null
        val pick = library.firstOrNull { id ->
            snapshotter.name(s, id) in wanted && s.getEntity(id)?.has<RevealedToComponent>() != true
        } ?: return null
        search.tracer?.line("  manifest dread ($source): library top ${snapshotter.name(s, pick)}")
        return s.copy(zones = s.zones + (ZoneKey(seats.user, Zone.LIBRARY) to listOf(pick) + (library - pick)))
    }

    private fun impulseOnTop(s: GameState): Impulse? {
        val (controller, name) = stackTop(s) ?: return null
        val text = def(name)?.oracleText ?: return null
        if (!IMPULSE_EXILE.containsMatchIn(text) || !IMPULSE_PLAY.containsMatchIn(text)) return null
        // "the top card" 1, "the top two cards ... three cards instead" 3, "that many cards" unknown
        val count = TOP_CARDS.findAll(text).map { m -> m.groupValues[1].let { if (it.isEmpty()) 1 else WORD_VALUES[it] } }
            .filterNotNull().maxOrNull() ?: IMPULSE_DEFAULT_COUNT
        return Impulse(controller, name, count)
    }

    /**
     * The opponent's impulse draw exiles filler from their unseen library, so a card the record has
     * them play from exile would have to come from their hand, one card too many out of it. The
     * branch this returns writes the cards they still play this half-turn ([Plan.spells], land
     * drops) and in the next two ([Search.oppoLater]) over the top unseen cards of their library,
     * as many as the impulse exiles; the search also keeps the line where it exiled filler.
     */
    private fun oppoImpulse(node: Node, ht: HalfTurnSpec, seats: Seats, search: Search): GameState? {
        val s = node.state
        val impulse = impulseOnTop(s)?.takeIf { it.controller == seats.oppo } ?: return null
        val now = node.plan.spells["oppo"].orEmpty().flatMap { (n, k) -> List(k) { n } } +
            if (ht.active == "oppo") node.plan.lands.flatMap { (n, k) -> List(k) { n } } else emptyList()
        val soon = (now + search.oppoLater).toMutableList().apply { remove(impulse.source) }.take(impulse.count)
        if (soon.isEmpty()) return null
        val slots = s.getLibrary(seats.oppo).filter { s.getEntity(it)?.has<RevealedToComponent>() != true }.take(soon.size)
        if (slots.size < soon.size) return null
        return materialize(s, slots.zip(soon).toMap())?.also {
            search.tracer?.line("  opponent impulse (${impulse.source}): library top written $soon")
        }
    }

    private fun impulseTop(s: GameState, seats: Seats, search: Search): GameState? {
        if (search.outside.isEmpty()) return null
        val (controller, source, count) = impulseOnTop(s) ?: return null
        if (controller != seats.user) return null
        val library = s.getLibrary(seats.user)
        val keep = if (s.activePlayerId == seats.user && s.step.ordinal < Step.DRAW.ordinal) 1 else 0
        val rest = library.drop(keep).toMutableList()
        val first = mutableListOf<EntityId>()
        for (name in search.outside) {
            val i = rest.indexOfFirst { snapshotter.name(s, it) == name }
            if (i >= 0) first += rest.removeAt(i)
        }
        if (first.isEmpty()) return null
        val needed = Snapshotter.counts(search.draws).toMutableMap()
        val spares = rest.asReversed().filter { id ->
            val name = snapshotter.name(s, id) ?: return@filter false
            val left = needed[name] ?: 0
            if (left > 0) needed[name] = left - 1
            left == 0 && s.getEntity(id)?.has<RevealedToComponent>() != true
        }.take(maxOf(0, count - first.size))
        rest.removeAll(spares.toSet())
        val order = library.take(keep) + first + spares + rest
        if (order == library) return null
        search.tracer?.line("  impulse ($source): library top ${order.take(keep + count).map { snapshotter.name(s, it) }}")
        return s.copy(zones = s.zones + (ZoneKey(seats.user, Zone.LIBRARY) to order))
    }

    /** A vanilla creature card, to stand in for an unseen nonland (and creature) card of the opponent's. */
    private val nonlandFiller: String? by lazy {
        registry.allCardNames().sorted().firstOrNull { n ->
            registry.getCard(n)?.let { it.creatureStats != null && it.oracleText.isBlank() && it.backFace == null } == true
        }
    }

    /**
     * The options of [d] to try: all of a short list; of a long one (a creature type to note or
     * name), those that are a subtype of a card the chooser still casts this half-turn, holds or
     * controls, most likely first, then the rest up to [MAX_SELECT_OPTIONS].
     */
    private fun optionOrder(s: GameState, d: ChooseOptionDecision, plan: Plan): List<Int> {
        if (d.options.size <= MAX_SELECT_OPTIONS) return d.options.indices.toList()
        val weight = mutableMapOf<String, Int>()
        fun count(names: Iterable<String>, w: Int) = names.forEach { n ->
            def(n)?.typeLine?.subtypes?.forEach { weight.merge(it.value, w, Int::plus) }
        }
        count(plan.spells[Seats.of(s).sideOf(d.playerId)].orEmpty().keys, 3)
        count(s.getHand(d.playerId).mapNotNull { snapshotter.name(s, it) }, 1)
        count(s.controlledBattlefield(d.playerId).mapNotNull { snapshotter.name(s, it) }, 1)
        val ranked = d.options.indices.filter { weight.containsKey(d.options[it]) }
            .sortedByDescending { weight.getValue(d.options[it]) }
        return (ranked + d.options.indices.filterNot { it in ranked }).take(maxOf(ranked.size, MAX_SELECT_OPTIONS))
    }

    /** A card with [keyword] ("Islandcycling {2}") to stand in for a card an opponent cycled. */
    private val cyclingFillers = mutableMapOf<String, String?>()
    private fun cyclingFiller(keyword: String): String? = cyclingFillers.getOrPut(keyword) {
        registry.allCardNames().sorted().firstOrNull { registry.getCard(it)?.oracleText?.contains(keyword) == true }
    }

    private fun castsFaceDown(name: String, keyword: (KeywordAbility) -> Boolean = { it is KeywordAbility.Morph || it is KeywordAbility.Disguise }) =
        def(name)?.keywordAbilities?.any(keyword) == true

    /**
     * A card to stand in for one an opponent cast face down: a disguise card (MKM, where face-down
     * spells have ward {2}), else a morph card.
     */
    private val faceDownFiller: String? by lazy {
        val names = registry.allCardNames().sorted()
        names.firstOrNull { castsFaceDown(it) { k -> k is KeywordAbility.Disguise } } ?: names.firstOrNull { castsFaceDown(it) }
    }

    /** The creature type a card's behold cost asks for ("behold a Goblin"), or null. */
    private fun beholdType(name: String): String? {
        val def = def(name) ?: return null
        return BEHOLD.find(def.oracleText)?.groupValues?.get(1)
    }

    private fun hasCreatureType(name: String, type: String): Boolean {
        val def = def(name) ?: return false
        return def.typeLine.subtypes.any { it.value == type } || Keyword.CHANGELING in def.keywords
    }

    /** A creature of [type] to stand in for the card an opponent beheld from hand. */
    private val beholdFillers = mutableMapOf<String, String?>()
    private fun beholdFiller(type: String): String? = beholdFillers.getOrPut(type) {
        registry.allCardNames().sorted().firstOrNull { n ->
            val def = registry.getCard(n)
            def != null && def.typeLine.subtypes.any { it.value == type } &&
                def.creatureStats != null && def.backFace == null
        }
    }

    private fun materialize(state: GameState, names: Map<EntityId, String>): GameState? {
        lastRefused = null
        val request = HiddenWorldMaterializationRequest(names.mapValues { registry.requireCard(snapshotter.engineName(it.value)) }, state.rng)
        return when (val r = materializer.materialize(state, request)) {
            is HiddenWorldMaterializationResult.Materialized -> r.state
            is HiddenWorldMaterializationResult.Unsupported -> {
                lastRefused = r.reason.entityId
                val slot = r.reason.entityId?.let { id -> "${snapshotter.name(state, id)} (${id.value})" }
                lastError = "${r.reason.kind} $slot ${r.reason.details.joinToString()}"
                null
            }
        }
    }

    private fun describe(plan: Plan): String = buildList {
        if (plan.lands.isNotEmpty()) add("lands ${plan.lands}")
        plan.spells.forEach { (side, left) -> if (left.isNotEmpty()) add("$side spells $left") }
        plan.activations.forEach { (side, left) -> if (left.isNotEmpty()) add("$side abilities $left") }
        plan.plots.forEach { (side, left) -> if (left.isNotEmpty()) add("$side plots $left") }
        plan.unlocks.forEach { (side, left) -> if (left.isNotEmpty()) add("$side unlocks $left") }
        plan.faceDown.forEach { (side, left) -> if (left.isNotEmpty()) add("$side face-down casts ${left.map { it.ifEmpty { "?" } }}") }
        plan.turnUps.forEach { (side, left) -> if (left.isNotEmpty()) add("$side turn-ups $left") }
        if (plan.attacked.isNotEmpty() && !plan.attacksDone) add("attack ${plan.attacked}")
        if (plan.blocking.isNotEmpty() && !plan.blocksDone) add("block ${plan.blocking}")
    }.joinToString(", ")

    companion object {
        const val DECK_SIZE = 40
        const val MAX_TARGET_COMBOS = 32
        const val MAX_COMBAT_OPTIONS = 24
        const val MAX_NUMBER_OPTIONS = 10
        const val MAX_FALLBACK_ACTIONS = 6
        const val MAX_SELECT_OPTIONS = 8
        /** Words in a card selection's prompt or label that say the picked cards are lost, not gained. */
        private val BINNED = listOf("graveyard", "bottom", "discard", "sacrifice", "exile")
        /** Matching end states collected per half-turn before the beam is picked from them. */
        const val MAX_ENDS = 128
        /** Nodes one group of start states' search runs before the next group's turn. */
        const val SLICE_NODES = 256
        const val MAX_SLOT_RETRIES = 8
        private val BEHOLD = Regex("""\bbehold an? ([A-Z][a-z]+)""")
        const val LIBRARY_VARIANT_DEPTH = 4
        private val READS_LIBRARY_TOP = Regex("""\bexplores?\b|\btop card of your library\b""")
        /** An impulse draw: exiles cards off the top of its controller's library, which may then be played. */
        private val IMPULSE_EXILE =
            Regex("""(?i)\bexile (?:the top|(?:cards|a number of cards|that many cards) from the top)[^.]*\byour library""")
        private val IMPULSE_PLAY = Regex("""(?i)\byou may (?:play|cast)\b""")
        /** A hand attack that has the opponent reveal their hand and discard a card the caster chooses. */
        private val REVEALS_HAND = Regex("""(?i)\breveals (?:their|his or her) hand\b[^.]*\. You choose""")
        private val TOP_CARDS = Regex("""\btop (?:(\w+) cards|card)\b""")
        private val WORD_VALUES = mapOf("two" to 2, "three" to 3, "four" to 4, "five" to 5)
        /** Cards an impulse exiles when its text gives no number ("that many"). */
        const val IMPULSE_DEFAULT_COUNT = 3
        const val MAX_PAYMENT_OPTIONS = 12
        /** How a [Move] was chosen: it used up a recorded action; the search chose it; the AI's responder did. */
        const val PLAN = "plan"
        const val SEARCH = "search"
        const val AI = "ai"
        /** Creature sets enumerated for a crew or saddle cost before the minimal ones are kept. */
        const val MAX_TAP_SETS = 512
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
