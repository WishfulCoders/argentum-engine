package com.wingedsheep.gameserver.controller

import com.wingedsheep.gameserver.GameServerTestBase
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * The starter decks carry a summary the landing page's launch panel shows without loading the card
 * catalog: colours, a cover card, the curve and the creature/spell/land split. Every starter must
 * have one that adds up — a list whose cards don't all resolve would silently under-count.
 */
class ExampleDeckSummaryTest : GameServerTestBase() {

    init {
        test("every starter deck has a summary whose split adds up to its card count") {
            val body = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI("http://localhost:$port/api/decks/examples")).GET().build(),
                HttpResponse.BodyHandlers.ofString(),
            ).body()
            val decks = json.parseToJsonElement(body).jsonArray
            decks.shouldNotBeEmpty()
            for (deck in decks) {
                val summary = deck.jsonObject["summary"].shouldNotBeNull().jsonObject
                val count = summary["cardCount"]!!.jsonPrimitive.int
                val creatures = summary["creatures"]!!.jsonPrimitive.int
                val spells = summary["spells"]!!.jsonPrimitive.int
                val lands = summary["lands"]!!.jsonPrimitive.int
                (creatures + spells + lands) shouldBe count
                summary["curve"]!!.jsonArray.sumOf { it.jsonPrimitive.int } shouldBe creatures + spells
                summary["colors"]!!.jsonArray.shouldNotBeEmpty()
                summary["coverCard"]!!.jsonPrimitive.content.length shouldBeGreaterThan 0
            }
        }

        // The tip is loaded as the deck's note, which the client caps at 120 characters.
        test("every starter deck has a play tip short enough to be a deck note") {
            val body = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI("http://localhost:$port/api/decks/examples")).GET().build(),
                HttpResponse.BodyHandlers.ofString(),
            ).body()
            for (deck in json.parseToJsonElement(body).jsonArray) {
                val note = deck.jsonObject["note"].shouldNotBeNull().jsonPrimitive.content
                note.isNotBlank() shouldBe true
                note.length shouldBeLessThanOrEqual 120
            }
        }
    }
}
