package com.wingedsheep.ai.draftsim

import com.wingedsheep.ai.engine.LimitedPickScorer
import com.wingedsheep.ai.llm.CardSummary
import io.kotest.core.spec.style.FunSpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File

/**
 * Golden dump for mtg-draft-ai's Python ports of the two draft pickers (`mtgdraft.sim.heuristic`).
 *
 * Reads the JSONL written by `mtgdraft heuristic-golden` — one draft state per line, each card in
 * [CardSummary] shape — and writes one line per state with [LimitedPickScorer]'s score (the AI
 * seats' picker) and [DraftsimScorer]'s total and raw rating per card, plus each picker's choice.
 * The Python side then compares the two score for score.
 *
 * Inert unless `DRAFT_GOLDEN_IN` and `DRAFT_GOLDEN_OUT` are set, so it never runs in normal CI:
 *   DRAFT_GOLDEN_IN=states.jsonl DRAFT_GOLDEN_OUT=kotlin.jsonl \
 *     ./gradlew :ai:test --tests '*DraftBaselineGoldenTest*' --rerun
 */
class DraftBaselineGoldenTest : FunSpec({

    fun card(o: JsonObject) = CardSummary(
        name = o["name"]!!.jsonPrimitive.content,
        manaCost = o["manaCost"]?.jsonPrimitive?.contentOrNull,
        typeLine = o["typeLine"]?.jsonPrimitive?.contentOrNull,
        rarity = o["rarity"]?.jsonPrimitive?.contentOrNull,
        power = o["power"]?.jsonPrimitive?.intOrNull,
        toughness = o["toughness"]?.jsonPrimitive?.intOrNull,
        oracleText = o["oracleText"]?.jsonPrimitive?.contentOrNull,
    )

    test("dump both pickers' scores for the Python port's states") {
        val inPath = System.getenv("DRAFT_GOLDEN_IN") ?: return@test
        val outPath = System.getenv("DRAFT_GOLDEN_OUT") ?: return@test
        val scorers = mutableMapOf<String, DraftsimScorer>()
        var archetypePath = false
        File(outPath).bufferedWriter().use { out ->
            File(inPath).forEachLine { line ->
                val state = Json.parseToJsonElement(line).jsonObject
                val setCode = state["setCode"]!!.jsonPrimitive.content
                val pack = state["pack"]!!.jsonArray.map { card(it.jsonObject) }
                val picked = state["picked"]!!.jsonArray.map { card(it.jsonObject) }

                // The AI seat: EngineAiPlayerController.chooseDraftPick
                val colors = LimitedPickScorer.inferColors(picked)
                val argentum = pack.map { LimitedPickScorer.score(it, colors, picked) }
                val argentumPick = pack.sortedByDescending { LimitedPickScorer.score(it, colors, picked) }.first().name

                // The Draftsim advisor: DraftsimDraftAdvisor.suggestPick
                val tables = DraftsimData.tablesFor(listOf(setCode))
                archetypePath = archetypePath || tables.archetypes.isNotEmpty()
                val scorer = scorers.getOrPut(setCode) { DraftsimScorer(tables) }
                val packCards = pack.map { it.toScorerCard() }
                val scores = scorer.scoreBoosterAuto(packCards, picked.map { it.toScorerCard() })
                val draftsimPick = scorer.argmax(packCards, scores)!!.name

                out.write(buildJsonObject {
                    put("argentum", JsonArray(argentum.map { JsonPrimitive(it) }))
                    put("argentumPick", argentumPick)
                    put("draftsimTotal", JsonArray(pack.map { JsonPrimitive(scores.getValue(it.name).total) }))
                    put("draftsimRaw", JsonArray(pack.map { JsonPrimitive(scores.getValue(it.name).rawRating) }))
                    put("draftsimPick", draftsimPick)
                    put("archetypePath", tables.archetypes.isNotEmpty())
                }.toString())
                out.newLine()
            }
        }
        println("DraftBaselineGolden: wrote $outPath (archetype path used: $archetypePath)")
    }
})
