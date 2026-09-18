package com.wingedsheep.arena

import com.wingedsheep.ai.engine.AiProfile
import com.wingedsheep.ai.engine.ResponseLookaheadStats
import com.wingedsheep.ai.engine.evaluation.EvalWeights
import com.wingedsheep.ai.engine.rollout.HoldingGatedEvaluator
import com.wingedsheep.ai.engine.rollout.RolloutSettings
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
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
 * Every target deck plays every opponent deck [gamesPerPair] times (alternating who is on the
 * play), scheduled opponent by opponent so a partial results file already has every target at
 * the same game count. Results are appended in completion order and flushed regularly; rerunning
 * with the same arguments skips games already in the results file. Decks with a card the engine
 * does not know are reported and skipped.
 */
fun main(args: Array<String>) {
    if (args.firstOrNull() == "one") return playOne(args.drop(1))
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
    val profile = arenaProfile(System.getProperty("arena.profile") ?: "current")
    val targetProfile = System.getProperty("arena.targetProfile")?.let(::arenaProfile) ?: profile
    val policyCheckpoint = System.getProperty("arena.policyCheckpoint")?.let(::File)
    require(policyCheckpoint == null || policyCheckpoint.isFile) {
        "-Darena.policyCheckpoint is not a file: $policyCheckpoint"
    }

    val decks = input.readLines().filter { it.isNotBlank() }.map { arenaJson.decodeFromString<DeckSpec>(it) }
    val (known, unknown) = decks.partition { d -> d.cards.all { registry.hasCard(it) } }
    for (d in unknown) {
        println("skip ${d.role} ${d.id}: unknown cards ${d.cards.filter { !registry.hasCard(it) }.distinct()}")
    }
    val targets = known.filter { it.role == "target" }.take(maxTargets)
    val opponents = known.filter { it.role == "opponent" }.take(maxOpponents)
    val byId = known.associateBy { it.id }

    val done = if (output.exists()) {
        output.readLines().filter { it.isNotBlank() }
            .map { arenaJson.decodeFromString<GameRecord>(it).key }.toHashSet()
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
            "${jobs.size} to play (${done.size} already done), profile ${profile.id}, " +
                "target ${policyCheckpoint?.let { "bc:${it.name}" } ?: targetProfile.id}, $threads threads",
    )
    if (jobs.isEmpty()) return

    val measureHolding = System.getProperty("arena.holding").toBoolean()
    val measureCards = System.getProperty("arena.cards").toBoolean()
    val policyBridges = ConcurrentLinkedQueue<GameplayPolicyBridge>()
    val local = ThreadLocal.withInitial {
        val bridge = policyCheckpoint?.let {
            GameplayPolicyBridge(
                registry = registry,
                python = System.getProperty("arena.policyPython") ?: "python3",
                checkpoint = it,
                pythonPath = System.getProperty("arena.policyPythonPath"),
                device = System.getProperty("arena.policyDevice") ?: "cpu",
                deterministic = System.getProperty("arena.policyDeterministic", "true").toBoolean(),
                temperature = System.getProperty("arena.policyTemperature")?.toDouble() ?: 1.0,
            ).also(policyBridges::add)
        }
        GameRunner(
            registry, profile, measureHolding = measureHolding, measureCards = measureCards,
            gameplayPolicy = bridge,
        )
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
                    policySeat = targetSeat.takeIf { policyCheckpoint != null },
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
                    policyActions = o.policyActions,
                    policyPasses = o.policyPasses, policyCasts = o.policyCasts,
                    policyLandPlays = o.policyLandPlays, policyActivations = o.policyActivations,
                    policyOtherActions = o.policyOtherActions,
                    policyPaymentActions = o.policyPaymentActions,
                    policyAttacks = o.policyAttacks, policyAttackers = o.policyAttackers,
                    policyBlocks = o.policyBlocks, policyBlockers = o.policyBlockers,
                    policyOrderMismatches = o.policyOrderMismatches,
                    policyFailures = o.policyFailures,
                    policyFirstFailure = o.policyFirstFailure,
                    policyIllegal = o.policyIllegal,
                    policyFirstRejection = o.policyFirstRejection,
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
    policyBridges.forEach(GameplayPolicyBridge::close)
    if (profile.rolloutsOnlyWhenHolding || targetProfile.rolloutsOnlyWhenHolding) println(HoldingGatedEvaluator.summary())
    // Only ever non-zero for an arm that switched the hook on (mtg-draft-ai `docs/27` §7.4).
    if (ResponseLookaheadStats.windows.get() > 0) println("  lookahead: $ResponseLookaheadStats")
}

/**
 * `-Darena.profile` (both seats) and `-Darena.targetProfile` (the target's seat only): `+`-joined tokens,
 * applied left to right to [AiProfile.CURRENT], the default AI.
 *
 * - `current`: nothing.
 * - `raceclock`: the discounted race clock ([AiProfile.discountedRaceClock]; mtg-draft-ai `docs/28` §6).
 * - `intent`: card knowledge ([AiProfile.useCardIntent]); `timing`: that plus the upstream hold rules, no
 *   rollouts (`docs/27` §3, §7); `reserve`: `timing` plus [com.wingedsheep.ai.engine.evaluation.ManaReserve]
 *   at `-Darena.reserveWeight` (default 1.5), scaled by the deck's answers with `-Darena.reserveScaled=true`.
 * - `apprentice`: priority choices (not combat or decisions) scored by the linear model in
 *   `shared-apprentice.json` under `-Dargentum.ai.apprentice.dir` (the gameplay pilot, `docs/28`);
 *   `correction`: priority choices add the linear term in `shared-correction.json` (same directory) to the
 *   profile's own score; `correction-actions`: the same term choosing only which action once the
 *   uncorrected score has chosen to act (`docs/28` §5).
 *
 * - `rollout`: upstream's rollout evaluator on every decision; `holdup`: the same only where keeping mana up is
 *   the question ([AiProfile.rolloutsOnlyWhenHolding], `docs/28` §7), with the static leaf's share of a gated score at
 *   `-Darena.holdupStaticWeight` (default upstream's 0.75). Both sample the opponent's hidden cards
 *   ([AiProfile.determinizeHiddenInformation]), so a playout never plays their real hand.
 *
 * - `lookahead`: the one-response lookahead ([AiProfile.opponentRespondsInSimulation], `docs/27` §7.6). The
 *   measured arm was `timing+lookahead`, against `timing`.
 * - `idle`: in the last sorcery-speed window of its turn, a sorcery-speed cast needs to beat passing only by
 *   `-Darena.idleAllowance` (default 1.0) ([AiProfile.spendIdleManaAtSorcerySpeed], `docs/33` §13).
 *
 * - `eot`: the same for instant-speed casts in the opponent's end step, at `-Darena.eotAllowance` (default 3.0)
 *   ([AiProfile.spendIdleManaInTheirEndStep], `docs/33` §13.6).
 *
 * So `raceclock+timing+correction-actions` is the race clock, the hold rules and the correction together.
 * An apprentice or correction that did not load is an error, not a silent fallback to the default evaluator.
 */
fun arenaProfile(name: String): AiProfile =
    name.split('+').fold(AiProfile.CURRENT) { profile, token -> withToken(profile, token) }

private fun withToken(p: AiProfile, token: String): AiProfile {
    val id = if (p.id == AiProfile.CURRENT.id) "current-$token" else "${p.id}-$token"
    return when (token) {
        "current" -> p
        "raceclock" -> p.copy(id = id, discountedRaceClock = true)
        "intent" -> p.copy(id = id, useCardIntent = true)
        "timing" -> p.copy(
            id = id,
            useCardIntent = true,
            holdRemovalForBetterTargets = true,
            holdCountersForBetterSpells = true,
            cashCantripsInTheEndStep = true,
            holdFlashPermanentsForAmbush = true,
            holdExpiringGrantsForCombat = true,
            // combatTricksWaitForBlocks is left off: upstream pairs it with TieredBudgetPolicy and says the
            // two do not separate.
        )
        "reserve" -> {
            val weight = System.getProperty("arena.reserveWeight")?.toDouble() ?: 1.5
            val scaled = System.getProperty("arena.reserveScaled").toBoolean()
            withToken(p, "timing").copy(
                id = "${id}-$weight" + if (scaled) "-scaled" else "",
                manaReserveWeight = weight, manaReserveScalesWithDeck = scaled,
            )
        }
        "lookahead" -> p.copy(id = id, opponentRespondsInSimulation = true)
        "idle" -> {
            val allowance = System.getProperty("arena.idleAllowance")?.toDouble() ?: 1.0
            p.copy(id = "$id-$allowance", spendIdleManaAtSorcerySpeed = allowance)
        }
        "eot" -> {
            val allowance = System.getProperty("arena.eotAllowance")?.toDouble() ?: 3.0
            p.copy(id = "$id-$allowance", spendIdleManaInTheirEndStep = allowance)
        }
        "rollout" -> p.copy(id = id, rollouts = RolloutSettings.DEFAULT, determinizeHiddenInformation = true)
        "holdup" -> {
            val staticWeight = System.getProperty("arena.holdupStaticWeight")?.toDouble()
            p.copy(
                id = id + (staticWeight?.let { "-$it" } ?: ""),
                rollouts = staticWeight?.let { RolloutSettings.DEFAULT.copy(staticWeight = it) } ?: RolloutSettings.DEFAULT,
                rolloutsOnlyWhenHolding = true, determinizeHiddenInformation = true,
            )
        }
        "apprentice" -> {
            require(EvalWeights.isRawProfile("shared-apprentice")) {
                "no valid shared-apprentice.json under -Dargentum.ai.apprentice.dir"
            }
            p.copy(id = id, priorityEvalWeightsId = "shared-apprentice")
        }
        "correction", "correction-actions" -> {
            requireNotNull(EvalWeights.correction("shared-correction")) {
                "no valid shared-correction.json under -Dargentum.ai.apprentice.dir"
            }
            p.copy(
                id = id, priorityCorrectionId = "shared-correction",
                priorityCorrectionChoosesActionOnly = token == "correction-actions",
            )
        }
        else -> error("unknown profile token $token in -Darena.(target)profile")
    }
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
