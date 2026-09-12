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
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.ConvokePayment
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

    private data class Node(val state: GameState, val plan: Plan)

    private class Search {
        val ends = mutableListOf<GameState>()
        var nodes = 0
        var closest: List<String>? = null
        var tracer: Tracer? = null

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
            val tracing = tracer?.halfTurn == i
            if (tracing) tracer!!.begin(spec, i, beam.size)
            search.tracer = tracer.takeIf { tracing }
            for (start in beam) {
                if (search.nodes >= nodeBudget || search.ends.size >= beamWidth) break
                val revealed = revealOppo(start, seats, ht)
                if (revealed == null) {
                    search.near(listOf("could not write the opponent's cards into hidden slots: $lastError"))
                    continue
                }
                val ready = if (ht.active == "user") restackUser(revealed, seats, ht) ?: revealed else revealed
                searchHalfTurn(ready, ht, i + 1, seats, search)
            }
            nodeCounts += search.nodes
            if (tracing) tracer!!.end(search.nodes, search.ends.size, search.closest)
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
            val notes = search.tracer?.takeIf { it.wants(search.nodes) }?.let { t ->
                t.line(t.header(search.nodes, s, seats, snapshotter, describe(node.plan)))
                mutableListOf<String>()
            }
            val children = expand(node, ht, seats, notes)
            notes?.forEach { search.tracer!!.line(it) }
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
                    if (diff.isEmpty()) {
                        search.tracer?.line("    end check: matches the snapshot")
                        search.ends += child.state
                    } else search.near(diff)
                } else {
                    stack.addLast(child)
                }
            }
        }
    }

    /**
     * The children of [node]. With [notes] (tracing), also says for every legal action whether the
     * plan allowed it and, for each variant tried, whether the engine took it or why it refused.
     */
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
        for (la in legal) {
            val action = la.action
            when {
                action is PassPriority -> pass = action
                action is PlayLand -> {
                    val name = snapshotter.name(s, action.cardId) ?: continue
                    when {
                        !active -> skip(la, "not the active player")
                        !plan.canPlayLand(name) -> skip(la, "not in the plan")
                        else -> tryAll(s, la, listOf(action), notes).forEach { out += Node(it, plan.playLand(name)) }
                    }
                }
                action is CastSpell -> {
                    val name = snapshotter.name(s, action.cardId) ?: continue
                    when {
                        !plan.canCast(side, name) -> skip(la, "not in the plan")
                        !la.affordable -> skip(la, "in the plan, NOT AFFORDABLE")
                        else -> {
                            val after = plan.cast(side, name)
                            // which lands pay only matters if this side casts again this half-turn
                            val more = after.spells[side].orEmpty().isNotEmpty()
                            val variants = withTargets(s, la, player)
                                .flatMap { withCostPayments(la, it) }
                                .flatMap { withManaChoices(s, player, la, it, more) }
                            tryAll(s, la, variants, notes).forEach { out += Node(it, after) }
                        }
                    }
                }
                la.actionType == "DeclareAttackers" -> when {
                    !active || plan.attacksDone -> skip(la, "attacks already declared or not the active player")
                    else -> {
                        val options = attacks(s, la, player, seats, plan)
                        if (options.isEmpty()) {
                            skip(la, "no attackers match ${plan.attacked} among ${names(s, la.validAttackers)}")
                        }
                        tryAll(s, la, options, notes).forEach { out += Node(it, plan.copy(attacksDone = true)) }
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
                        tryAll(s, la, options, notes).forEach { out += Node(it, plan.copy(blocksDone = true)) }
                    }
                }
                else -> skip(la, "not modelled")
            }
        }
        pass?.let { p ->
            val passed = apply(s, p)
            if (passed != null) out += Node(passed, plan) else notes?.add("  ! pass refused: $lastError")
        }
        if (out.isEmpty()) {
            // A mandatory action the plan does not model (damage assignment order, a second
            // combat's declaration): let each legal action through.
            notes?.add("  fallback: trying the first $MAX_FALLBACK_ACTIONS legal actions")
            for (la in legal.take(MAX_FALLBACK_ACTIONS)) {
                tryAll(s, la, listOf(la.action), notes).forEach { out += Node(it, plan) }
            }
        }
        return out
    }

    /** Applies each variant of [la]; with [notes], reports how many the engine took and why it refused the rest. */
    private fun tryAll(s: GameState, la: LegalAction, variants: List<GameAction>, notes: MutableList<String>?): List<GameState> {
        val taken = mutableListOf<GameState>()
        val refusals = mutableListOf<String>()
        for (v in variants) {
            val next = apply(s, v)
            if (next != null) taken += next else refusals += lastError ?: "?"
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
     * One action per way to pay the cast's additional cost ([LegalAction.additionalCostInfo]):
     * behold a Kithkin, blight a creature, sacrifice, discard. Unlike the AI, which takes the first
     * candidate, the search branches, because which creature gets the -1/-1 counter changes the
     * game. The "or pay {2}" alternative arrives as its own legal action.
     */
    private fun withCostPayments(la: LegalAction, action: GameAction): List<GameAction> {
        val info = la.additionalCostInfo ?: return listOf(action)
        val cast = action as? CastSpell ?: return listOf(action)
        val base = cast.additionalCostPayment ?: AdditionalCostPayment()
        val payments = when (info.costType) {
            "Blight" -> info.validBlightTargets.map { base.copy(blightTargets = listOf(it)) }
            "Behold" -> subsets(info.validBeholdTargets, info.beholdCount, info.beholdCount)
                .map { base.copy(beheldCards = it) }
            "TapPermanents" -> subsets(info.validTapTargets, info.tapCount, info.tapCount)
                .map { base.copy(tappedPermanents = it) }
            "DiscardCard" -> subsets(info.validDiscardTargets, info.discardCount, info.discardCount)
                .map { base.copy(discardedCards = it) }
            "SacrificePermanent" -> subsets(info.validSacrificeTargets, info.sacrificeCount, info.sacrificeCount)
                .map { base.copy(sacrificedPermanents = it) }
            "BouncePermanent" -> subsets(info.validBounceTargets, info.bounceCount, info.bounceCount)
                .map { base.copy(bouncedPermanents = it) }
            "ExileFromGraveyard" -> listOf(base.copy(exiledCards = info.validExileTargets.take(info.exileMinCount)))
            else -> return listOf(action)
        }
        return payments.take(MAX_TARGET_COMBOS).map { cast.copy(additionalCostPayment = it) }
    }

    /**
     * Ways to pay the mana: autopay first, then convoke with 1..N creatures (which creatures tap
     * decides who can block next turn), then — when [more] casts follow this half-turn — explicit
     * sets of lands, because autopay can tap the colour a later spell needed. One option per
     * distinct set of names.
     */
    private fun withManaChoices(
        s: GameState, player: EntityId, la: LegalAction, action: GameAction, more: Boolean,
    ): List<GameAction> {
        val cast = action as? CastSpell ?: return listOf(action)
        val cost = la.manaCostString?.let { runCatching { ManaCost.parse(it) }.getOrNull() }
        val out = mutableListOf<GameAction>(cast)
        val convoke = la.convokeCreatures.orEmpty()
        if (la.hasConvoke && convoke.isNotEmpty() && cost != null) {
            // The AI's policy — each coloured symbol by a creature of that colour, then generic —
            // is the only option here that pays coloured mana with creatures ({4}{W}{W}).
            val coloured = mutableMapOf<EntityId, ConvokePayment>()
            val unused = convoke.toMutableList()
            for (symbol in cost.symbols.filterIsInstance<ManaSymbol.Colored>()) {
                val i = unused.indexOfFirst { symbol.color in it.colors }
                if (i >= 0) coloured[unused.removeAt(i).entityId] = ConvokePayment(symbol.color)
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
        if (more && cost != null && cost.cmc > 0) {
            val lands = s.controlledBattlefield(player).filter {
                s.projectedState.hasType(it, "LAND") && s.getEntity(it)?.has<TappedComponent>() != true
            }
            if (cost.cmc <= lands.size) {
                for (pick in distinctByName(s, subsets(lands, cost.cmc, cost.cmc, cap = MAX_PAYMENT_OPTIONS * 4))) {
                    out += cast.copy(paymentStrategy = PaymentStrategy.Explicit(pick))
                }
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
                if (!d.ordered && d.options.size <= MAX_SELECT_OPTIONS && d.maxSelections <= 2) {
                    // Small picks (including "you may": min 0) are enumerated, the recorded tutored
                    // card first — Eclipsed Kithkin's "reveal a Kithkin, Forest, or Plains".
                    val wanted = ht.tutored.toSet()
                    subsets(d.options, d.minSelections, d.maxSelections)
                        .sortedByDescending { pick -> pick.count { snapshotter.name(s, it) in wanted } }
                        .map { CardsSelectedResponse(d.id, it) }
                } else heuristic(s, d)
            is SearchLibraryDecision -> tutor(s, d, ht) ?: heuristic(s, d)
            else -> heuristic(s, d)
        }
        val out = mutableListOf<Node>()
        val refusals = mutableListOf<String>()
        for (r in responses) {
            val next = apply(s, SubmitDecision(d.playerId, r))
            if (next != null) out += Node(next, node.plan) else refusals += lastError ?: "?"
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
     * Put this half-turn's recorded draws on top of the user's library, then its tutored cards (so a
     * "look at the top N" effect finds the card the record says it took). Library effects — Eclipsed
     * Kithkin sending cards to the bottom, surveil, a shuffle — move cards after [stackUser] ran, so
     * the order is re-forced every half-turn. Cards change places by swapping identities, which keeps
     * the library's contents; a card no longer in the library overwrites the slot instead.
     */
    private fun restackUser(state: GameState, seats: Seats, ht: HalfTurnSpec): GameState? {
        // Past the upkeep, this turn's draw-step draw has already happened.
        val drawn = if (state.step.ordinal > Step.UPKEEP.ordinal) ht.drawn.drop(1) else ht.drawn
        val want = drawn + ht.tutored
        if (want.isEmpty()) return state
        val library = state.getLibrary(seats.user)
        if (library.size < want.size) return null
        val current = library.map { snapshotter.name(state, it) }.toMutableList()
        val assignment = mutableMapOf<EntityId, String>()
        for ((k, name) in want.withIndex()) {
            if (current[k] == name) continue
            val j = (k + 1 until library.size).firstOrNull { current[it] == name }
            if (j != null) {
                val displaced = current[k] ?: continue
                assignment[library[j]] = displaced
                current[j] = displaced
            }
            assignment[library[k]] = name
            current[k] = name
        }
        if (assignment.isEmpty()) return state
        return materialize(state, assignment)
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
            is HiddenWorldMaterializationResult.Unsupported -> {
                val slot = r.reason.entityId?.let { id -> "${snapshotter.name(state, id)} (${id.value})" }
                lastError = "${r.reason.kind} $slot ${r.reason.details.joinToString()}"
                null
            }
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
        const val MAX_SELECT_OPTIONS = 8
        const val MAX_PAYMENT_OPTIONS = 12
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
