package com.wingedsheep.gameserver

import com.wingedsheep.gameserver.matchmaking.MatchmakingMode
import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.sdk.core.DeckFormat
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

class MatchmakingTest : GameServerTestBase() {

    init {
        test("two strangers in the same queue are paired, accept, and land in a locked lobby") {
            val alice = createClient().apply { connectAs("Alice Queue") }
            val bob = createClient().apply { connectAs("Bob Queue") }

            alice.send(ClientMessage.JoinMatchmaking(format = DeckFormat.PAUPER))
            eventually(5.seconds) {
                alice.messages.filterIsInstance<ServerMessage.MatchmakingStatus>().lastOrNull()?.searching shouldBe true
            }
            bob.send(ClientMessage.JoinMatchmaking(format = DeckFormat.PAUPER))

            val offers = listOf(alice, bob).map { client ->
                eventually(5.seconds) {
                    client.messages.filterIsInstance<ServerMessage.MatchFound>().lastOrNull().shouldNotBeNull()
                }
            }
            offers[0].matchId shouldBe offers[1].matchId
            offers[0].opponentName shouldBe "Bob Queue"
            offers[0].opponentRating shouldBe null

            alice.send(ClientMessage.RespondToMatch(offers[0].matchId, accept = true))
            bob.send(ClientMessage.RespondToMatch(offers[1].matchId, accept = true))

            for (client in listOf(alice, bob)) {
                eventually(5.seconds) {
                    val lobby = client.messages.filterIsInstance<ServerMessage.QuickGameLobbyState>().lastOrNull()
                        .shouldNotBeNull()
                    lobby.matchmade shouldBe true
                    lobby.format shouldBe DeckFormat.PAUPER
                    lobby.players.size shouldBe 2
                    client.messages.filterIsInstance<ServerMessage.MatchmakingStatus>().last().searching shouldBe false
                }
            }

            // The queue chose the format; nobody can change it afterwards.
            alice.send(ClientMessage.SetQuickGameLobbyFormat(DeckFormat.STANDARD))
            eventually(5.seconds) { alice.latestError().shouldNotBeNull() }

            // Either player leaving ends the pairing for both.
            bob.send(ClientMessage.LeaveQuickGameLobby)
            eventually(5.seconds) {
                alice.messages.filterIsInstance<ServerMessage.QuickGameLobbyClosed>().lastOrNull().shouldNotBeNull()
            }
        }

        test("a decline requeues the player who accepted") {
            val carol = createClient().apply { connectAs("Carol Queue") }
            val dave = createClient().apply { connectAs("Dave Queue") }

            carol.send(ClientMessage.JoinMatchmaking(format = DeckFormat.PREMODERN))
            dave.send(ClientMessage.JoinMatchmaking(format = DeckFormat.PREMODERN))
            val offer = eventually(5.seconds) {
                carol.messages.filterIsInstance<ServerMessage.MatchFound>().lastOrNull().shouldNotBeNull()
            }

            carol.send(ClientMessage.RespondToMatch(offer.matchId, accept = true))
            dave.send(ClientMessage.RespondToMatch(offer.matchId, accept = false))

            eventually(5.seconds) {
                val status = carol.messages.filterIsInstance<ServerMessage.MatchmakingStatus>().last()
                status.searching shouldBe true
                status.notice.shouldNotBeNull()
                dave.messages.filterIsInstance<ServerMessage.MatchmakingStatus>().last().searching shouldBe false
            }
            carol.send(ClientMessage.LeaveMatchmaking)
            eventually(5.seconds) {
                carol.messages.filterIsInstance<ServerMessage.MatchmakingStatus>().last().searching shouldBe false
            }
        }

        /** Queue two fresh players for [mode], accept the match for both, and return them. */
        suspend fun matchAndAccept(mode: MatchmakingMode, names: Pair<String, String>): List<TestWebSocketClient> {
            val players = listOf(names.first, names.second).map { name -> createClient().apply { connectAs(name) } }
            players.forEach { it.send(ClientMessage.JoinMatchmaking(mode = mode)) }
            val offers = players.map { client ->
                eventually(5.seconds) {
                    client.messages.filterIsInstance<ServerMessage.MatchFound>().lastOrNull().shouldNotBeNull()
                }
            }
            offers.forEach { it.mode shouldBe mode }
            players.zip(offers).forEach { (client, offer) -> client.send(ClientMessage.RespondToMatch(offer.matchId, accept = true)) }
            for (client in players) {
                eventually(5.seconds) {
                    client.messages.filterIsInstance<ServerMessage.MatchmakingStatus>().last().matched shouldBe true
                }
            }
            return players
        }

        test("Random deck and Momir Basic start the game as soon as both accept") {
            for ((mode, names) in listOf(
                MatchmakingMode.RANDOM_DECK to ("Erin Random" to "Frank Random"),
                MatchmakingMode.MOMIR_BASIC to ("Gina Momir" to "Hal Momir"),
            )) {
                val players = matchAndAccept(mode, names)
                val sessions = players.map { client ->
                    eventually(10.seconds) {
                        client.messages.filterIsInstance<ServerMessage.GameCreated>().lastOrNull().shouldNotBeNull()
                    }.sessionId
                }
                sessions.distinct().size shouldBe 1
                // Nothing to prepare, so no lobby is ever shown.
                players.forEach { it.messages.filterIsInstance<ServerMessage.QuickGameLobbyState>().shouldBeEmpty() }
            }
        }

        test("Jump In seats the pair on their pack choice") {
            val players = matchAndAccept(MatchmakingMode.JUMP_IN, "Ivy Jump" to "Jack Jump")
            for (client in players) {
                eventually(10.seconds) {
                    val update = client.messages.filterIsInstance<ServerMessage.LobbyUpdate>().lastOrNull().shouldNotBeNull()
                    update.state shouldBe "DECK_BUILDING"
                    update.players.size shouldBe 2
                    update.jumpstart.shouldNotBeNull().offers.shouldNotBeEmpty()
                }
            }
        }

        test("modes without a ranked queue refuse a ranked search") {
            val player = createClient().apply { connectAs("Kim Ranked") }
            player.send(ClientMessage.JoinMatchmaking(mode = MatchmakingMode.JUMP_IN, ranked = true))
            eventually(5.seconds) { player.latestError()?.message shouldBe "Jump In has no ranked queue" }
        }

        test("guests can't queue for ranked") {
            val guest = createClient().apply { connectAs("Guest Queue") }
            guest.send(ClientMessage.JoinMatchmaking(format = null, ranked = true))
            eventually(5.seconds) { guest.latestError()?.message shouldBe "Sign in to play ranked" }
        }
    }
}
