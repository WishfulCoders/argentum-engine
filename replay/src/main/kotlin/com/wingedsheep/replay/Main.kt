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
import java.util.zip.GZIPOutputStream

/**
 * `replay <specs.jsonl> <results.jsonl> [maxGames] [beamWidth] [nodeBudget] [threads] [lines.jsonl.gz]`
 *
 * Reconstructs each game spec (from `mtgdraft replay-export`) and writes one [GameResult] per line,
 * then prints a summary: how many games were reproduced end to end, what share of half-turns were
 * reproduced in order, and the most common reasons a game stopped. With a lines file, also writes
 * each game's accepted line ([GameLine], gzipped JSONL): the whole game when it was reproduced, the
 * half-turns before the failure otherwise.
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
    val linesFile = args.getOrNull(6)?.let(::File)

    val specs = input.readLines().filter { it.isNotBlank() }.take(maxGames)
        .map { specJson.decodeFromString<GameSpec>(it) }
    val (registry, snapshotter) = engineCards(specs.map { it.set }.distinct().single())
    println("replay: ${specs.size} games, beam $beamWidth, budget $nodeBudget nodes/half-turn, $threads threads")

    val local = ThreadLocal.withInitial { Reconstructor(registry, snapshotter, beamWidth, nodeBudget) }
    val writers = ThreadLocal.withInitial { LineWriter(registry) }
    val pool = Executors.newFixedThreadPool(threads)
    val futures = specs.map { spec ->
        pool.submit(Callable {
            val reconstructor = local.get()
            val result = try {
                reconstructor.run(spec)
            } catch (e: Throwable) {
                return@Callable GameResult(spec.gameId, "error", spec.halfTurns.size, 0, reason = e.toString().take(300)) to null
            }
            val line = reconstructor.acceptedLine?.takeIf { linesFile != null }?.toList()?.let { steps ->
                try {
                    val seats = Seats.of(steps.first().before)
                    lineJson.encodeToString(GameLine.serializer(),
                        writers.get().line(spec, result, seats, steps, reconstructor.acceptedThrough, reconstructor.gaps))
                } catch (e: Throwable) {
                    System.err.println("line ${spec.gameId}: $e")
                    null
                }
            }
            result to line
        })
    }
    val results = mutableListOf<GameResult>()
    output.parentFile?.mkdirs()
    val lines = linesFile?.let { f ->
        f.parentFile?.mkdirs()
        GZIPOutputStream(f.outputStream()).bufferedWriter()
    }
    var written = 0
    output.bufferedWriter().use { w ->
        futures.forEachIndexed { i, f ->
            val (r, line) = f.get()
            results += r
            w.write(specJson.encodeToString(r))
            w.newLine()
            if (line != null && lines != null) {
                lines.write(line)
                lines.newLine()
                written++
            }
            if ((i + 1) % 10 == 0 || i + 1 == futures.size) {
                println("  ${i + 1}/${futures.size} done")
            }
        }
    }
    lines?.close()
    if (linesFile != null) println("lines: $written games to $linesFile")
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
    val resynced = played.filter { it.matched != null }
    if (resynced.isNotEmpty()) {
        val after = resynced.sumOf { it.halfTurns - it.reproduced }
        val kept = resynced.sumOf { it.matched!! - it.reproduced }
        println("resync: ${resynced.size} games, ${resynced.sumOf { it.gaps.size }} gaps; " +
            "half-turns after the first break rebuilt $kept / $after (${"%.1f".format(100.0 * kept / maxOf(after, 1))}%); " +
            "all half-turns ${played.sumOf { it.matched ?: it.reproduced }} / $halfTurns")
        resynced.mapNotNull { it.stopReason }.groupingBy { it.take(40) }
            .eachCount().entries.sortedByDescending { it.value }.take(8)
            .forEach { (k, n) -> println("  stopped $n  $k") }
    }
    val medianMs = results.map { it.millis }.sorted().let { if (it.isEmpty()) 0 else it[it.size / 2] }
    println("median time per game: $medianMs ms")
    println("top reasons:")
    results.filter { it.reason != null }
        .groupingBy { it.reason!!.substringBefore(' ').take(40) }.eachCount()
        .entries.sortedByDescending { it.value }.take(12)
        .forEach { (k, n) -> println("  $n  $k") }
}
