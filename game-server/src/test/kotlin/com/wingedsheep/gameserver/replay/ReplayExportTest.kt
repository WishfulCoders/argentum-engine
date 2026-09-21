package com.wingedsheep.gameserver.replay

import com.wingedsheep.sdk.core.AttackMode
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.mockk
import kotlin.io.path.createTempDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

/**
 * The export writes one file per game, named after the game.
 *
 * It shipped with the interpolation escaped — `"\${replay.gameId}.json"` inside an already-escaped
 * Kotlin string — so every finished game overwrote a single file literally called
 * `${'$'}{replay.gameId}.json`. A research session is a pile of games and the second one silently ate
 * the first, which is the whole point of the export failing quietly (mtg-draft-ai `docs/44`).
 */
class ReplayExportTest : FunSpec({

    fun replay(gameId: String) = CompactReplay(
        gameId = gameId,
        players = listOf(ReplayPlayerInfo("p1", "Alice"), ReplayPlayerInfo("p2", "Bob")),
        startedAt = "2026-09-19T00:00:00Z",
        endedAt = "2026-09-19T00:10:00Z",
        winnerName = "Alice",
        setup = ReplaySetup(
            seed = 1L,
            format = Format.Standard,
            attackMode = AttackMode.MULTIPLE,
            players = listOf(
                ReplayPlayerSetup(playerId = "p1", name = "Alice", deck = Deck(cards = listOf("Forest"))),
                ReplayPlayerSetup(playerId = "p2", name = "Bob", deck = Deck(cards = listOf("Forest"))),
            ),
            seatRoster = emptyList(),
        ),
        actions = emptyList(),
    )

    test("each finished game exports to its own <gameId>.json") {
        val dir = createTempDirectory("replay-export")
        val service = ReplayService(
            store = mockk(relaxed = true),
            reconstructor = mockk(relaxed = true),
            presentation = mockk(relaxed = true),
            exportDir = dir.toString(),
        )

        service.save(replay("game-aaa"), archive = false)
        service.save(replay("game-bbb"), archive = false)

        dir.listDirectoryEntries().map { it.name }.sorted() shouldContainExactly
            listOf("game-aaa.json", "game-bbb.json")
    }
})
