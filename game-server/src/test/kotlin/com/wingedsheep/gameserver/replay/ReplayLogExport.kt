package com.wingedsheep.gameserver.replay

import com.wingedsheep.gameserver.config.GameBeansConfig
import com.wingedsheep.gameserver.config.GameProperties
import io.kotest.core.spec.style.FunSpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * Re-simulates every `*.replay.json` in a directory (as `scripts/fetch-ai-games` downloads them from
 * the server) and writes `<gameId>.log` beside each — the [ReplayGameLog] rendering, for reading the
 * AI's games against real players. Off unless `-DreplayLog=true`.
 *
 * The AI seats come from the directory's `index.json` (`aiSeats` per game, written by the fetch
 * script); a replay missing from it falls back to the server's `[AI] ` name prefix. The registry is
 * the server's own ([GameBeansConfig]), so with the replay's pinned cards overlaid, reconstruction
 * runs on exactly what the server's replay viewer would.
 *
 * ```
 * just ai-replay-logs build/ai-games
 * scripts/gradle-locked :game-server:test --tests "*.ReplayLogExport" -Dbenchmark=true \
 *     -DreplayLog=true -DreplayLogDir=$PWD/build/ai-games
 * ```
 */
class ReplayLogExport : FunSpec({
    val enabled = System.getProperty("replayLog") == "true"

    test("render downloaded replays as AI-review logs").config(enabled = enabled) {
        val dir = File(requireNotNull(System.getProperty("replayLogDir")) { "set -DreplayLogDir" })
        require(dir.isDirectory) { "replayLogDir $dir is not a directory" }
        val files = dir.listFiles { f -> f.name.endsWith(".replay.json") }.orEmpty().sortedBy { it.name }
        require(files.isNotEmpty()) { "no *.replay.json in $dir — run scripts/fetch-ai-games first" }

        val index = File(dir, "index.json").takeIf { it.exists() }
            ?.let { Json.parseToJsonElement(it.readText()).jsonArray.map { e -> e.jsonObject } }
            .orEmpty()
            .associateBy { it.getValue("gameId").jsonPrimitive.content }

        val beans = GameBeansConfig(GameProperties())
        val cards = beans.cardRegistry()
        val renderer = ReplayGameLog(cards, beans.printingRegistry(cards), beans.tokenArtRegistry())

        val tally = sortedMapOf<String, Int>()
        for (file in files) {
            val line = try {
                val replay = ReplayFile.parse(file.readBytes())
                val entry = index[replay.gameId]
                val ai = entry?.get("players")?.jsonArray
                    ?.map { it.jsonObject }
                    ?.filter { it["ai"]?.jsonPrimitive?.boolean == true }
                    ?.map { it.getValue("name").jsonPrimitive.content }
                    ?.toSet()
                    ?: replay.setup.players.map { it.name }.filter { it.startsWith("[AI]") }.toSet()
                val result = renderer.render(replay, ai, entry?.get("winnerName")?.jsonPrimitive?.contentOrNull)
                File(dir, "${replay.gameId}.log").writeText(result.text)
                tally.merge(result.fidelity.name, 1, Int::plus)
                "${replay.gameId}: ${result.fidelity} ${result.actionsApplied}/${replay.actions.size} actions" +
                    " (engine ${replay.engineVersion.take(12)})" + (result.divergenceReason?.let { " — $it" } ?: "")
            } catch (e: Exception) {
                tally.merge("ERROR", 1, Int::plus)
                File(dir, "${file.name.removeSuffix(".replay.json")}.log")
                    .writeText("ERROR rendering ${file.name}: ${e::class.simpleName}: ${e.message}\n${e.stackTraceToString()}")
                "${file.name}: ERROR ${e::class.simpleName}: ${e.message}"
            }
            println(line)
        }
        println("Fidelity: ${tally.entries.joinToString { "${it.key}=${it.value}" }}. Logs in ${dir.absolutePath}")
    }
})
