package com.wingedsheep.gameserver.controller

import com.wingedsheep.gameserver.GameServerTestBase
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * `POST /api/decks/summaries` gives a saved deck the same at-a-glance summary a starter deck
 * carries, honouring the owner's chosen cover while the card is still in the deck.
 */
class DeckSummariesTest : GameServerTestBase() {

    private val client = HttpClient.newHttpClient()

    private fun get(path: String): String = client.send(
        HttpRequest.newBuilder(URI("http://localhost:$port$path")).GET().build(),
        HttpResponse.BodyHandlers.ofString(),
    ).body()

    private fun post(path: String, body: JsonObject): String = client.send(
        HttpRequest.newBuilder(URI("http://localhost:$port$path"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build(),
        HttpResponse.BodyHandlers.ofString(),
    ).body()

    init {
        test("a saved deck's summary uses its chosen cover, and ignores one it no longer contains") {
            val starter = json.parseToJsonElement(get("/api/decks/examples")).jsonArray
                .first().jsonObject
            val cards = starter["cards"]!!.jsonObject
            val automatic = starter["summary"]!!.jsonObject["coverCard"]!!.jsonPrimitive.content
            val other = cards.keys.first { it != automatic }

            fun deck(cover: String) = buildJsonObject {
                put("cards", cards)
                put("coverCard", cover)
            }
            val response = json.parseToJsonElement(
                post(
                    "/api/decks/summaries",
                    buildJsonObject {
                        put("decks", buildJsonObject {
                            put("chosen", deck(other))
                            put("stale", deck("A Card Not In This Deck"))
                        })
                    },
                ),
            ).jsonObject

            val chosen = response["chosen"]!!.jsonObject
            chosen["coverCard"]!!.jsonPrimitive.content shouldBe other
            chosen["coverImageUri"]!!.jsonPrimitive.content shouldStartWith "http"
            chosen["cardCount"]!!.jsonPrimitive.int shouldBe cards.values.sumOf { it.jsonPrimitive.int }

            val stale = response["stale"]!!.jsonObject
            stale["coverCard"]!!.jsonPrimitive.content shouldBe automatic
            stale["coverCard"]!!.jsonPrimitive.content shouldNotBe other
        }
    }
}
