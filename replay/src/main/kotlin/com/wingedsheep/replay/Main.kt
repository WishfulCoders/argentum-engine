package com.wingedsheep.replay

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.mtg.sets.MtgSetCatalog
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * `replay <specs.jsonl> <results.jsonl> [maxGames] [beamWidth] [nodeBudget] [threads]`
 *
 * Reconstructs each game spec (from `mtgdraft replay-export`) and writes one [GameResult] per line,
 * then prints a summary: how many games were reproduced end to end, what share of half-turns were
 * reproduced in order, and the most common reasons a game stopped.
 */
fun main(args: Array<String>) {
    require(args.size >= 2) {
        "usage: replay <specs.jsonl> <results.jsonl> [maxGames] [beamWidth] [nodeBudget] [threads]"
    }
    val input = File(args[0])
    val output = File(args[1])
    val maxGames = args.getOrNull(2)?.toInt() ?: Int.MAX_VALUE
    val beamWidth = args.getOrNull(3)?.toInt() ?: 8
    val nodeBudget = args.getOrNull(4)?.toInt() ?: 20_000
    val threads = args.getOrNull(5)?.toInt() ?: Runtime.getRuntime().availableProcessors()

    // Every set, so Special Guests printed elsewhere resolve; ECL last so its printings win.
    val sets = MtgSetCatalog.all
    val ecl = MtgSetCatalog.requireByCode("ECL")
    val registry = CardRegistry().apply {
        for (set in sets) if (set.code != ecl.code) {
            register(set.cards)
            register(set.basicLands)
        }
        register(ecl.cards)
        register(ecl.basicLands)
    }
    val snapshotter = Snapshotter(sets.flatMap { it.cards })

    val specs = input.readLines().filter { it.isNotBlank() }.take(maxGames)
        .map { specJson.decodeFromString<GameSpec>(it) }
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
