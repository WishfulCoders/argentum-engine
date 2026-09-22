package com.wingedsheep.gameserver.controller

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Files
import java.nio.file.Path

private val logger = LoggerFactory.getLogger(PlaytestController::class.java)

/**
 * Development-only: the curated matchup list the playtest page plays through.
 *
 * Research tooling, not part of the game. It exists because collecting gameplay labels means
 * playing *chosen* decks — a named draft deck against a named opponent, with a reason on record
 * for why that pairing is worth an hour — rather than a fresh sealed pool every game
 * (mtg-draft-ai `docs/44`).
 *
 * The file is written by that repo's `scripts/playtest/build_matchups.py` from the same
 * `decks.jsonl` an arena run takes. **Its rows are passed through untouched**: the curation script
 * owns the schema and the page renders it, so adding a field to a matchup — another bucket, a new
 * statistic — needs no change here. Deliberately not modelled in Kotlin, which would make this a
 * third place to keep in sync for no gain.
 *
 * **WARNING:** never enable in production. Enable with `game.dev-endpoints.enabled=true`.
 */
@RestController
@RequestMapping("/api/dev/playtest")
@ConditionalOnProperty(name = ["game.dev-endpoints.enabled"], havingValue = "true")
class PlaytestController(
    @Value("\${game.playtest.matchups-file:}") private val matchupsFile: String,
) {

    /**
     * The curated matchups, as a JSON array.
     *
     * An unconfigured or missing file is an empty list, not an error: the page then says there is
     * nothing curated yet, which is the honest state of a server whose owner has not run the
     * script. A malformed line is the one thing worth failing on — a half-parsed matchup would
     * seat a deck nobody chose.
     */
    @GetMapping("/matchups", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun matchups(): ResponseEntity<String> {
        if (matchupsFile.isBlank()) return ResponseEntity.ok("[]")
        val path = Path.of(matchupsFile)
        if (!Files.exists(path)) {
            logger.warn("Playtest matchups file {} does not exist; serving none", path)
            return ResponseEntity.ok("[]")
        }
        val rows = Files.readAllLines(path).map(String::trim).filter { it.isNotEmpty() }
        rows.forEachIndexed { i, row ->
            require(row.startsWith("{") && row.endsWith("}")) {
                "Line ${i + 1} of $path is not a JSON object: ${row.take(60)}"
            }
        }
        return ResponseEntity.ok(rows.joinToString(",", prefix = "[", postfix = "]"))
    }
}
