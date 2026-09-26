package com.wingedsheep.arena

import com.wingedsheep.ai.engine.AiProfile
import com.wingedsheep.ai.engine.ResponseLookaheadStats
import com.wingedsheep.ai.engine.profileFromTokens
import com.wingedsheep.ai.engine.evaluation.EvalWeights
import com.wingedsheep.ai.engine.hidden.OpponentModel
import com.wingedsheep.ai.engine.rollout.HoldingGatedEvaluator
import com.wingedsheep.ai.engine.rollout.RolloutSettings
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * `arena <decks.jsonl> <results.jsonl> [gamesPerPair] [threads] [maxOpponents] [maxTargets] [seed]`
 *
 * `arena one <decks.jsonl> <targetId> <opponentId> <game> <seed>` replays one game from a results
 * line with stack traces printed, for debugging an `exception(...)` reason.
 *
 * `arena sealed <SET> <decks.jsonl> <targets> <opponents> [seed]` writes seeded sealed decks for a set
 * that has no 17Lands decks yet (see [writeSealedDecks]).
 *
 * Every target deck plays every opponent deck [gamesPerPair] times (alternating who is on the
 * play), scheduled opponent by opponent so a partial results file already has every target at
 * the same game count. Results are appended in completion order and flushed regularly; rerunning
 * with the same arguments skips games already in the results file. Decks with a card the engine
 * does not know are reported and skipped.
 */
fun main(args: Array<String>) {
    if (args.firstOrNull() == "one") return playOne(args.drop(1))
    if (args.firstOrNull() == "sealed") return writeSealedDecks(args.drop(1))
    require(args.size >= 2) {
        "usage: arena <decks.jsonl> <results.jsonl> [gamesPerPair] [threads] [maxOpponents] [maxTargets] [seed]"
    }
    val input = File(args[0])
    val output = File(args[1])
    val gamesPerPair = args.getOrNull(2)?.toInt() ?: 4
    val threads = args.getOrNull(3)?.toInt() ?: Runtime.getRuntime().availableProcessors()
    val maxOpponents = args.getOrNull(4)?.toInt() ?: Int.MAX_VALUE
    val maxTargets = args.getOrNull(5)?.toInt() ?: Int.MAX_VALUE
    val runSeed = args.getOrNull(6)?.toLong() ?: 0L
    // `-Darena.probeStranded=true`: is the target stranding cards it has the amount of mana for? (`docs/33` §12)
    val probeStranded = System.getProperty("arena.probeStranded").toBoolean()

    val registry = eclRegistry()
    val profile = profileFromTokens(System.getProperty("arena.profile") ?: "current")
    val targetProfile = System.getProperty("arena.targetProfile")?.let(::profileFromTokens) ?: profile

    val decks = input.readLines().filter { it.isNotBlank() }.map { arenaJson.decodeFromString<DeckSpec>(it) }
    val (known, unknown) = decks.partition { d -> d.cards.all { registry.hasCard(it) } }
    for (d in unknown) {
        println("skip ${d.role} ${d.id}: unknown cards ${d.cards.filter { !registry.hasCard(it) }.distinct()}")
    }
    val targets = known.filter { it.role == "target" }.take(maxTargets)
    val opponents = known.filter { it.role == "opponent" }.take(maxOpponents)
    val byId = known.associateBy { it.id }

    // `-Darena.opponentModel`: what the target's AI assumes about the opponent's deck when its profile
    // determinizes (mtg-draft-ai `docs/51_ptcg_style_selfplay` §9). `truth` (the default) is the real
    // decklist, as every arena run before it; `colours` is every other deck in the file with the
    // opponent's main colours; `set` is every other deck in the file. The target's and the opponent's
    // own decks are never in a mixture.
    val opponentModelMode = System.getProperty("arena.opponentModel") ?: "truth"
    require(opponentModelMode in setOf("truth", "colours", "set")) { "unknown arena.opponentModel $opponentModelMode" }
    val deckCounts = known.associate { it.id to it.cards.groupingBy { c -> c }.eachCount() }
    fun colourKey(d: DeckSpec) = requireNotNull(d.mainColors) { "deck ${d.id} has no main_colors" }
        .uppercase().toSortedSet().joinToString("")
    val byColour = if (opponentModelMode == "colours") known.groupBy(::colourKey) else emptyMap()
    fun opponentModelFor(target: DeckSpec, opponent: DeckSpec): OpponentModel? {
        if (opponentModelMode == "truth") return null
        val population = if (opponentModelMode == "colours") byColour.getValue(colourKey(opponent)) else known
        val lists = population.filter { it.id != target.id && it.id != opponent.id }.map { deckCounts.getValue(it.id) }
        require(lists.isNotEmpty()) { "no other ${colourKey(opponent)} deck for ${opponent.id}" }
        return OpponentModel.DecklistMixture(lists)
    }
    if (opponentModelMode == "colours") {
        println("opponent model colours: " + byColour.entries.sortedBy { it.key }.joinToString { "${it.key}=${it.value.size}" })
    }

    // Resume, tolerantly. A spot VM copies `out/` to the bucket once a minute while the arena is
    // still writing to it, so the copy that comes back after a preemption routinely ends in a
    // *partial line*. Parsing strictly threw there and killed the arm — on 2026-09-19 that cost
    // `phase0-fixing` 62,650 finished games and would have relaunch-crashed in a loop, because the
    // truncated file is what the next VM downloads too (mtg-draft-ai `docs/46` §11).
    //
    // The file is rewritten rather than only filtered, because the writer below opens it in append
    // mode: leaving a line with no newline on the end would glue the next record onto it and make
    // a second corrupt line out of a good one.
    val done = if (output.exists()) {
        val keys = HashSet<String>()
        val kept = mutableListOf<String>()
        var dropped = 0
        for (line in output.readLines()) {
            if (line.isBlank()) continue
            val key = runCatching { arenaJson.decodeFromString<GameRecord>(line).key }.getOrNull()
            if (key == null) dropped++ else { keys += key; kept += line }
        }
        if (dropped > 0) {
            println("resume: dropped $dropped unparseable line(s) from ${output.name}, kept ${kept.size}")
            output.writeText(kept.joinToString(separator = "\n", postfix = "\n"))
        }
        keys
    } else hashSetOf()

    data class Job(val target: DeckSpec, val opponent: DeckSpec, val game: Int, val seed: Long)
    val jobs = buildList {
        for ((o, opp) in opponents.withIndex()) for ((t, tgt) in targets.withIndex()) {
            for (g in 0 until gamesPerPair) {
                if ("${tgt.id}|${opp.id}|$g" in done) continue
                // Both games of a pair (target on the play, then on the draw) share a seed.
                add(Job(tgt, opp, g, mixSeed(runSeed, t.toLong(), o.toLong(), (g / 2).toLong())))
            }
        }
    }
    println(
        "arena: ${targets.size} targets x ${opponents.size} opponents x $gamesPerPair games, " +
            "${jobs.size} to play (${done.size} already done), profile ${profile.id}, target ${targetProfile.id}, opponent model $opponentModelMode, $threads threads",
    )
    if (jobs.isEmpty()) return

    val measureHolding = System.getProperty("arena.holding").toBoolean()
    val measureCards = System.getProperty("arena.cards").toBoolean()
    val local = ThreadLocal.withInitial {
        GameRunner(registry, profile, measureHolding = measureHolding, measureCards = measureCards)
    }
    val pool = Executors.newFixedThreadPool(threads)
    val completion = ExecutorCompletionService<GameRecord>(pool)
    val submitted = AtomicInteger(0)
    // Keep the queue bounded so a 200k-game schedule does not hold 200k futures at once.
    val window = threads * 4
    var next = 0
    fun submit() {
        val job = jobs[next++]
        completion.submit(Callable {
            val t0 = System.currentTimeMillis()
            val targetSeat = job.game % 2
            val seats = if (targetSeat == 0) listOf(job.target.cards, job.opponent.cards)
            else listOf(job.opponent.cards, job.target.cards)
            val base = GameRecord(job.target.id, job.opponent.id, job.game, targetSeat, job.seed)
            try {
                val o = local.get().play(
                    seats, job.seed,
                    if (targetSeat == 0) listOf(targetProfile, profile) else listOf(profile, targetProfile),
                    probeSeat = if (probeStranded) targetSeat else null,
                    opponentModel = opponentModelFor(job.target, job.opponent)?.let { targetSeat to it },
                )
                base.copy(
                    winnerSeat = o.winnerSeat,
                    targetWon = o.winnerSeat?.let { it == targetSeat },
                    turns = o.turns, actions = o.actions, illegal = o.illegal, life = o.life,
                    reason = o.reason, millis = System.currentTimeMillis() - t0, holding = o.holding,
                    cycle = o.cycle, casts = o.casts, tappedOut = o.tappedOut, cards = o.cards, lastWindow = o.lastWindow, gaps = o.gaps,
                    probeWindows = o.probe.windows, probeAffordable = o.probe.affordable,
                    probeStranded = o.probe.stranded, probeStrandedCards = o.probe.strandedCards,
                    probeFixable = o.probe.fixable, probeFixMissed = o.probe.fixMissed,
                )
            } catch (e: Throwable) {
                base.copy(reason = "init(${e::class.simpleName}: ${e.message?.take(200)})", millis = System.currentTimeMillis() - t0)
            }
        })
        submitted.incrementAndGet()
    }
    repeat(minOf(window, jobs.size)) { submit() }

    val start = System.currentTimeMillis()
    var finished = 0
    var wins = 0
    var decided = 0
    output.parentFile?.mkdirs()
    java.io.FileWriter(output, true).buffered().use { w ->
        while (finished < jobs.size) {
            val r = completion.take().get()
            finished++
            if (next < jobs.size) submit()
            w.write(arenaJson.encodeToString(r))
            w.newLine()
            if (r.targetWon != null) { decided++; if (r.targetWon) wins++ }
            if (finished % 100 == 0 || finished == jobs.size) {
                w.flush()
                val secs = (System.currentTimeMillis() - start) / 1000.0
                println(
                    "  $finished/${jobs.size} games, %.2f games/s, decided %d, target win rate %.3f".format(
                        finished / secs, decided, if (decided > 0) wins.toDouble() / decided else 0.0,
                    ),
                )
            }
        }
    }
    pool.shutdown()
    if (profile.rolloutsOnlyWhenHolding || targetProfile.rolloutsOnlyWhenHolding) println(HoldingGatedEvaluator.summary())
    // Only ever non-zero for an arm that switched the hook on (mtg-draft-ai `docs/27` §7.4).
    if (ResponseLookaheadStats.windows.get() > 0) println("  lookahead: $ResponseLookaheadStats")
}

/**
 * Every set, so Special Guests printed elsewhere resolve; ECL last so its printings win. Plus the
 * predefined tokens (Treasure, Food, Clue, ...): without them `CreateTreasure` resolves to nothing.
 */
private fun eclRegistry(): CardRegistry {
    val ecl = MtgSetCatalog.requireByCode("ECL")
    return CardRegistry().apply {
        for (set in MtgSetCatalog.all) if (set.code != ecl.code) {
            register(set.cards)
            register(set.basicLands)
        }
        register(ecl.cards)
        register(ecl.basicLands)
        register(PredefinedTokens.allTokens)
    }
}

private fun playOne(args: List<String>) {
    require(args.size == 5) { "usage: arena one <decks.jsonl> <targetId> <opponentId> <game> <seed>" }
    val decks = File(args[0]).readLines().filter { it.isNotBlank() }
        .map { arenaJson.decodeFromString<DeckSpec>(it) }.associateBy { it.id }
    val target = decks.getValue(args[1])
    val opponent = decks.getValue(args[2])
    val game = args[3].toInt()
    val seed = args[4].toLong()
    val targetSeat = game % 2
    val seats = if (targetSeat == 0) listOf(target.cards, opponent.cards) else listOf(opponent.cards, target.cards)
    val o = GameRunner(eclRegistry(), AiProfile.CURRENT, printTraces = true).play(seats, seed)
    println("winner seat ${o.winnerSeat} (target seat $targetSeat), turns ${o.turns}, actions ${o.actions}, " +
        "illegal ${o.illegal}, life ${o.life}, reason '${o.reason}'")
}

/** SplitMix64 over the run seed and the schedule coordinates, so a run is reproducible. */
private fun mixSeed(vararg parts: Long): Long {
    var z = 0x9E3779B97F4A7C15uL.toLong()
    for (p in parts) {
        z += p * -7046029254386353131L
        z = (z xor (z ushr 30)) * -4658895280553007687L
        z = (z xor (z ushr 27)) * -7723592293110705685L
        z = z xor (z ushr 31)
    }
    return z
}

/**
 * `arena sealed <SET> <decks.jsonl> <targets> <opponents> [seed]`: [targets] + [opponents] seeded
 * sealed decks for [SET], for a set with no 17Lands decks yet (Reality Fracture before release).
 *
 * The pool is every card the set can open — its own definitions plus its reprints resolved to their
 * canonical definition with the reprint's rarity, the rule `ai`'s arena benchmark
 * (`AiBenchmarkSupport.draftableCards`) and `game-server` use — opened as six boosters of 11 commons,
 * 3 uncommons and a rare (1 in 8 mythic), and built by the engine's heuristic sealed deckbuilder. It
 * deliberately ignores `metadata.inBooster`: Scryfall still flags most of FRA `booster: false`.
 */
private fun writeSealedDecks(args: List<String>) {
    require(args.size in 4..5) { "usage: arena sealed <SET> <decks.jsonl> <targets> <opponents> [seed]" }
    val set = MtgSetCatalog.requireByCode(args[0].uppercase())
    val out = File(args[1])
    val targets = args[2].toInt()
    val opponents = args[3].toInt()
    val seed = args.getOrNull(4)?.toLong() ?: 0L

    val canonical = MtgSetCatalog.all.sortedBy { it.code }.flatMap { it.cards }
        .groupBy { it.name }.mapValues { (_, defs) -> defs.first() }
    val ownNames = set.cards.mapTo(hashSetOf()) { it.name }
    val reprints = set.printings.filter { it.name !in ownNames }.groupBy { it.name }
        .mapNotNull { (_, treatments) ->
            val printing = treatments.firstOrNull { !it.isAlternateFrame && !it.isPromo } ?: treatments.first()
            canonical[printing.name]?.withPrinting(printing)?.let { it.copy(metadata = it.metadata.copy(rarity = printing.rarity)) }
        }
    val cards = (set.cards + reprints).filter { !it.typeLine.isBasicLand && !it.meldResult }.sortedBy { it.name }
    val byRarity = cards.groupBy { it.metadata.rarity }
    println("${set.code}: ${cards.size} openable non-basic cards " +
        byRarity.entries.sortedBy { it.key }.joinToString { "${it.key}=${it.value.size}" })

    val rng = kotlin.random.Random(seed)
    val seen = mutableSetOf<String>()
    out.printWriter().use { w ->
        for (i in 0 until targets + opponents) {
            val pool = mutableListOf<com.wingedsheep.sdk.model.CardDefinition>()
            repeat(6) {
                val used = mutableSetOf<String>()
                fun pick(r: com.wingedsheep.sdk.model.Rarity): com.wingedsheep.sdk.model.CardDefinition? =
                    byRarity[r].orEmpty().filter { it.name !in used }.takeIf { it.isNotEmpty() }
                        ?.let { it[rng.nextInt(it.size)] }?.also { used += it.name }
                repeat(11) { pick(com.wingedsheep.sdk.model.Rarity.COMMON)?.let(pool::add) }
                repeat(3) { pick(com.wingedsheep.sdk.model.Rarity.UNCOMMON)?.let(pool::add) }
                val mythic = if (rng.nextDouble() < 0.125) pick(com.wingedsheep.sdk.model.Rarity.MYTHIC) else null
                (mythic ?: pick(com.wingedsheep.sdk.model.Rarity.RARE) ?: pick(com.wingedsheep.sdk.model.Rarity.UNCOMMON))?.let(pool::add)
            }
            seen += pool.map { it.name }
            val deck = com.wingedsheep.ai.engine.buildHeuristicSealedDeck(pool)
                .flatMap { (name, n) -> List(n) { name } }.sorted()
            val colours = pool.filter { it.name in deck.toSet() && !it.typeLine.isLand }
                .flatMap { c -> c.colors.map { it.symbol } }.groupingBy { it }.eachCount()
                .entries.sortedByDescending { it.value }.take(2).map { it.key }.sorted().joinToString("")
            val role = if (i < targets) "target" else "opponent"
            w.println(arenaJson.encodeToString(DeckSpec("${set.code.lowercase()}-sealed-$seed-$i", role, deck, colours)))
        }
    }
    println("wrote ${targets + opponents} decks to $out; ${seen.size} distinct cards opened across the pools")
}
