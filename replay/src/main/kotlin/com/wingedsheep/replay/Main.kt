package com.wingedsheep.replay

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * `replay <specs.jsonl> <results.jsonl> [maxGames] [beamWidth] [nodeBudget] [threads]`
 *
 * Reconstructs each game spec (from `mtgdraft replay-export`) and writes one [GameResult] per line,
 * then prints a summary: how many games were reproduced end to end, what share of half-turns were
 * reproduced in order, and the most common reasons a game stopped.
 *
 * `replay trace <specs.jsonl> <gameId> [halfTurn] [nodeBudget] [tracedNodes] [beamWidth]` prints the
 * search of one half-turn node by node ([Tracer]); without a half-turn, the one where the game fails.
 */
fun main(args: Array<String>) {
    if (args.firstOrNull() == "trace") return trace(args.drop(1))
    require(args.size >= 2) {
        "usage: replay <specs.jsonl> <results.jsonl> [maxGames] [beamWidth] [nodeBudget] [threads]"
    }
    val input = File(args[0])
    val output = File(args[1])
    val maxGames = args.getOrNull(2)?.toInt() ?: Int.MAX_VALUE
    val beamWidth = args.getOrNull(3)?.toInt() ?: 8
    val nodeBudget = args.getOrNull(4)?.toInt() ?: 20_000
    val threads = args.getOrNull(5)?.toInt() ?: Runtime.getRuntime().availableProcessors()

    val specs = input.readLines().filter { it.isNotBlank() }.take(maxGames)
        .map { specJson.decodeFromString<GameSpec>(it) }
    val (registry, snapshotter) = engineCards(specs.map { it.set }.distinct().single())
    println("replay: ${specs.size} games, beam $beamWidth, budget $nodeBudget nodes/half-turn, $threads threads")

    val local = ThreadLocal.withInitial { Reconstructor(registry, snapshotter, beamWidth, nodeBudget) }
    val pool = Executors.newFixedThreadPool(threads)
    val futures = specs.map { spec ->
        pool.submit(Callable {
            try {
                local.get().run(spec)
            } catch (e: Throwable) {
                GameResult(spec.gameId, "error", spec.halfTurns.size, 0, reason = e.toString().take(300))
            }
        })
    }
    val results = mutableListOf<GameResult>()
    output.parentFile?.mkdirs()
    output.bufferedWriter().use { w ->
        futures.forEachIndexed { i, f ->
            val r = f.get()
            results += r
            w.write(specJson.encodeToString(r))
            w.newLine()
            if ((i + 1) % 10 == 0 || i + 1 == futures.size) {
                println("  ${i + 1}/${futures.size} done")
            }
        }
    }
    pool.shutdown()
    summarize(results)
}

/**
 * Every set, so Special Guests and bonus sheets printed elsewhere resolve; [setCode] last so its
 * printings win. Plus the predefined tokens (Treasure, Food, Clue, ...): without them
 * `CreateTreasure` resolves to nothing.
 */
private fun engineCards(setCode: String): Pair<CardRegistry, Snapshotter> {
    val sets = MtgSetCatalog.all
    val main = MtgSetCatalog.requireByCode(setCode)
    val registry = CardRegistry().apply {
        for (set in sets) if (set.code != main.code) {
            register(set.cards)
            register(set.basicLands)
        }
        register(main.cards)
        register(main.basicLands)
        register(PredefinedTokens.allTokens)
    }
    return registry to Snapshotter(sets.flatMap { it.cards })
}

private fun trace(args: List<String>) {
    require(args.size >= 2) {
        "usage: replay trace <specs.jsonl> <gameId> [halfTurn|auto] [nodeBudget] [tracedNodes] [beamWidth]"
    }
    val line = File(args[0]).useLines { lines -> lines.firstOrNull { "\"${args[1]}\"" in it } }
        ?: error("game ${args[1]} not in ${args[0]}")
    val spec = specJson.decodeFromString<GameSpec>(line)
    val nodeBudget = args.getOrNull(3)?.toInt() ?: 40_000
    val tracedNodes = args.getOrNull(4)?.toInt() ?: 300
    val beamWidth = args.getOrNull(5)?.toInt() ?: 8
    val (registry, snapshotter) = engineCards(spec.set)
    val halfTurn = args.getOrNull(2)?.toIntOrNull()
        ?: Reconstructor(registry, snapshotter, beamWidth, nodeBudget).run(spec).let { r ->
            println("untraced run: ${r.status}, ${r.reproduced}/${r.halfTurns} half-turns; ${r.reason}")
            r.failedAt ?: return
        }
    val record = specJson.parseToJsonElement(line).jsonObject["half_turns"]!!.jsonArray[halfTurn].toString()
    val tracer = Tracer(halfTurn, record, maxNodes = tracedNodes)
    val r = Reconstructor(registry, snapshotter, beamWidth, nodeBudget, tracer = tracer).run(spec)
    println("result: ${r.status}, ${r.reproduced}/${r.halfTurns} half-turns; ${r.reason}")
}

private fun summarize(results: List<GameResult>) {
    val byStatus = results.groupingBy { it.status }.eachCount()
    val played = results.filter { it.status == "reproduced" || it.status == "failed" }
    val halfTurns = played.sumOf { it.halfTurns }
    val reproduced = played.sumOf { it.reproduced }
    println("status: $byStatus")
    if (halfTurns > 0) {
        println("half-turns reproduced in order: $reproduced / $halfTurns (${"%.1f".format(100.0 * reproduced / halfTurns)}%)")
    }
    val medianMs = results.map { it.millis }.sorted().let { if (it.isEmpty()) 0 else it[it.size / 2] }
    println("median time per game: $medianMs ms")
    println("top reasons:")
    results.filter { it.reason != null }
        .groupingBy { it.reason!!.substringBefore(' ').take(40) }.eachCount()
        .entries.sortedByDescending { it.value }.take(12)
        .forEach { (k, n) -> println("  $n  $k") }
}
