package com.wingedsheep.replay

import com.wingedsheep.arena.arenaProfile
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
 * `replay <specs.jsonl> <results.jsonl> [maxGames] [beamWidth] [nodeBudget] [threads] [lines.jsonl.gz] [prefs.jsonl.gz]`
 *
 * Reconstructs each game spec (from `mtgdraft replay-export`) and writes one [GameResult] per line,
 * then prints a summary: how many games were reproduced end to end, what share of half-turns were
 * reproduced in order, and the most common reasons a game stopped. With a lines file, also writes
 * each game's accepted line ([GameLine], gzipped JSONL): the whole game when it was reproduced, the
 * half-turns before the failure otherwise. With a prefs file, also writes the user's priority choices on
 * that line with every alternative simulated ([PreferenceWriter]; pass `-` for no lines file), scored by
 * `-Dreplay.prefsProfile` (`current`, or `raceclock`). With `-Dreplay.playOn=PILOT[,PILOT...]` (arena profile names, e.g.
 * `current,raceclock+timing+correction-actions`) and `-Dreplay.playOnOut=FILE`, also plays every failed game on from the
 * start of its failed half-turn with each pilot in both seats ([PlayOn], C1), one [PlayOnRecord] per line.
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
    val linesFile = args.getOrNull(6)?.takeIf { it != "-" }?.let(::File)
    val prefsFile = args.getOrNull(7)?.let(::File)

    val specs = input.readLines().filter { it.isNotBlank() }.take(maxGames)
        .map { specJson.decodeFromString<GameSpec>(it) }
    val (registry, snapshotter) = engineCards(specs.map { it.set }.distinct().single())
    println("replay: ${specs.size} games, beam $beamWidth, budget $nodeBudget nodes/half-turn, $threads threads")

    val local = ThreadLocal.withInitial { Reconstructor(registry, snapshotter, beamWidth, nodeBudget) }
    val writers = ThreadLocal.withInitial { LineWriter(registry) }
    val prefsBase = PreferenceWriter.baseProfile(System.getProperty("replay.prefsProfile"))
    val prefWriters = ThreadLocal.withInitial { PreferenceWriter(registry, prefsBase) }
    val playOnFile = System.getProperty("replay.playOnOut")?.let(::File)
    val playOnPilots = System.getProperty("replay.playOn")?.split(',')?.map { it to arenaProfile(it) }
    require((playOnFile == null) == (playOnPilots == null)) { "-Dreplay.playOn and -Dreplay.playOnOut go together" }
    val playOns = playOnPilots?.let { pilots -> ThreadLocal.withInitial { PlayOn(registry, pilots) } }
    val pool = Executors.newFixedThreadPool(threads)
    val futures = specs.map { spec ->
        pool.submit(Callable {
            val reconstructor = local.get()
            val result = try {
                reconstructor.run(spec)
            } catch (e: Throwable) {
                return@Callable GameOut(GameResult(spec.gameId, "error", spec.halfTurns.size, 0, reason = e.toString().take(300)))
            }
            val accepted = reconstructor.acceptedLine?.takeIf { linesFile != null || prefsFile != null }?.toList()
            val line = accepted?.takeIf { linesFile != null }?.let { steps ->
                try {
                    val seats = Seats.of(steps.first().before)
                    lineJson.encodeToString(GameLine.serializer(),
                        writers.get().line(spec, result, seats, steps, reconstructor.acceptedThrough))
                } catch (e: Throwable) {
                    System.err.println("line ${spec.gameId}: $e")
                    null
                }
            }
            val prefs = accepted?.takeIf { prefsFile != null }?.let { steps ->
                try {
                    prefWriters.get().roots(spec, Seats.of(steps.first().before), steps)
                        .map { lineJson.encodeToString(PrefRoot.serializer(), it) }
                } catch (e: Throwable) {
                    System.err.println("prefs ${spec.gameId}: $e")
                    null
                }
            }
            val start = reconstructor.acceptedState
            val playOn = if (playOns != null && result.status == "failed" && start != null) {
                try {
                    playOns.get().play(spec, result, start).map { lineJson.encodeToString(PlayOnRecord.serializer(), it) }
                } catch (e: Throwable) {
                    System.err.println("playOn ${spec.gameId}: $e")
                    null
                }
            } else null
            GameOut(result, line, prefs, playOn)
        })
    }
    val results = mutableListOf<GameResult>()
    output.parentFile?.mkdirs()
    val lines = linesFile?.let { f ->
        f.parentFile?.mkdirs()
        GZIPOutputStream(f.outputStream()).bufferedWriter()
    }
    val prefs = prefsFile?.let { f ->
        f.parentFile?.mkdirs()
        GZIPOutputStream(f.outputStream()).bufferedWriter().also {
            it.write(lineJson.encodeToString(PrefHeader.serializer(), PrefHeader(PreferenceWriter.FEATURES, prefsBase.id)))
            it.newLine()
        }
    }
    val playOnOut = playOnFile?.let { f -> f.parentFile?.mkdirs(); f.bufferedWriter() }
    var written = 0
    var roots = 0
    var playedOn = 0
    output.bufferedWriter().use { w ->
        futures.forEachIndexed { i, f ->
            val (r, line, rootLines, playOnLines) = f.get()
            results += r
            w.write(specJson.encodeToString(r))
            w.newLine()
            if (line != null && lines != null) {
                lines.write(line)
                lines.newLine()
                written++
            }
            if (rootLines != null && prefs != null) {
                for (root in rootLines) { prefs.write(root); prefs.newLine() }
                roots += rootLines.size
            }
            if (playOnLines != null && playOnOut != null) {
                for (p in playOnLines) { playOnOut.write(p); playOnOut.newLine() }
                playedOn++
            }
            if ((i + 1) % 10 == 0 || i + 1 == futures.size) {
                println("  ${i + 1}/${futures.size} done")
            }
        }
    }
    lines?.close()
    if (linesFile != null) println("lines: $written games to $linesFile")
    prefs?.close()
    if (prefsFile != null) println("prefs: $roots choices to $prefsFile")
    playOnOut?.close()
    if (playOnFile != null) println("play-on: $playedOn broken games x ${playOnPilots!!.size} pilots to $playOnFile")
    pool.shutdown()
    summarize(results)
}

/** One game's outputs, gathered on the worker thread. */
private data class GameOut(
    val result: GameResult,
    val line: String? = null,
    val prefs: List<String>? = null,
    val playOn: List<String>? = null,
)

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
