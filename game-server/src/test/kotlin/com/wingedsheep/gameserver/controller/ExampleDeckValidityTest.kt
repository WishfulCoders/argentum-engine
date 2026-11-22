package com.wingedsheep.gameserver.controller

import com.wingedsheep.gameserver.GameServerTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * A starter deck the picker offers must be playable in the format it claims: every card name
 * resolves to a registered card, and every card is legal there. A metagame list copied from a
 * tournament is the usual way this breaks — a mistyped name, or legality data that predates a
 * reprint that brought a card back into Standard.
 */
class ExampleDeckValidityTest : GameServerTestBase() {

    init {
        test("every starter deck with a format validates in that format") {
            val client = HttpClient.newHttpClient()
            val decks = json.parseToJsonElement(
                client.send(
                    HttpRequest.newBuilder(URI("http://localhost:$port/api/decks/examples")).GET().build(),
                    HttpResponse.BodyHandlers.ofString(),
                ).body(),
            ).jsonArray
            decks.shouldNotBeEmpty()

            for (deck in decks.map { it.jsonObject }) {
                val format = (deck["format"] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: continue
                val commander = (deck["commander"] as? JsonPrimitive)?.takeIf { it.isString }?.content
                // The commander rides in `cards`; the validator wants it separately.
                val deckList = deck["cards"]!!.jsonObject
                    .mapValues { (_, n) -> n.jsonPrimitive.int }
                    .let { cards -> if (commander == null) cards else cards - commander }
                val request = JsonObject(
                    buildMap {
                        put("deckList", JsonObject(deckList.mapValues { JsonPrimitive(it.value) }))
                        put("format", JsonPrimitive(format))
                        if (commander != null) put("commander", JsonPrimitive(commander))
                    },
                )
                val result = json.parseToJsonElement(
                    client.send(
                        HttpRequest.newBuilder(URI("http://localhost:$port/api/decks/validate"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(request.toString()))
                            .build(),
                        HttpResponse.BodyHandlers.ofString(),
                    ).body(),
                ).jsonObject
                val id = deck["id"]!!.jsonPrimitive.content
                withClue("$id in $format: ${result["errors"]}") {
                    result["valid"]!!.jsonPrimitive.boolean shouldBe true
                }
            }
        }
    }
}
