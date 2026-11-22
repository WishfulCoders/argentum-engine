package com.wingedsheep.gameserver

import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.protocol.Emote
import com.wingedsheep.gameserver.protocol.ServerMessage
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/**
 * Playing with strangers, end to end over the socket: the result screen's rematch and block, preset
 * emotes, and warming up against the AI while the matchmaking queue searches.
 */
class SocialPlayTest : GameServerTestBase() {

    /** Two guests in a quick lobby on random decks, through mulligans into a live game. */
    private suspend fun startQuickGame(hostName: String, guestName: String): Pair<TestWebSocketClient, TestWebSocketClient> {
        val host = createClient().apply { connectAs(hostName) }
        val guest = createClient().apply { connectAs(guestName) }
        host.send(ClientMessage.CreateQuickGameLobby())
        val lobbyId = eventually(5.seconds) {
            host.messages.filterIsInstance<ServerMessage.QuickGameLobbyState>().last().lobbyId
        }
        guest.send(ClientMessage.JoinQuickGameLobby(lobbyId))
        eventually(5.seconds) {
            host.messages.filterIsInstance<ServerMessage.QuickGameLobbyState>().last().players shouldHaveSize 2
        }
        for (client in listOf(host, guest)) {
            client.send(ClientMessage.SubmitQuickGameLobbyDeck(deckList = emptyMap()))
        }
        eventually(5.seconds) {
            host.messages.filterIsInstance<ServerMessage.QuickGameLobbyState>().last()
                .players.all { it.randomDeck } shouldBe true
        }
        for (client in listOf(host, guest)) client.send(ClientMessage.SetQuickGameLobbyReady(true))
        keepHands(host, guest, gamesCreated = 1)
        return host to guest
    }

    private suspend fun keepHands(vararg clients: TestWebSocketClient, gamesCreated: Int) {
        for (client in clients) {
            eventually(10.seconds) {
                client.messages.count { it is ServerMessage.GameCreated } shouldBe gamesCreated
                client.messages.count { it is ServerMessage.MulliganDecision } shouldBe gamesCreated
            }
            client.send(ClientMessage.KeepHand)
        }
    }

    private fun TestWebSocketClient.postGame(): ServerMessage.PostGame? =
        messages.filterIsInstance<ServerMessage.PostGame>().lastOrNull()

    private fun TestWebSocketClient.emotes(): List<ServerMessage.EmoteReceived> =
        messages.filterIsInstance<ServerMessage.EmoteReceived>()

    init {
        test("after a game both players can ask for a rematch, which starts at once with the same setup") {
            val (alice, bob) = startQuickGame("Alice Rematch", "Bob Rematch")
            alice.send(ClientMessage.Concede())

            val offer = eventually(5.seconds) { alice.postGame().shouldNotBeNull() }
            offer.opponentName shouldBe "Bob Rematch"
            offer.canRematch shouldBe true
            // Guests have no accounts to befriend.
            offer.friendship shouldBe ServerMessage.FriendshipState.UNAVAILABLE

            alice.send(ClientMessage.PostGameRematch(offer.gameId))
            eventually(5.seconds) { bob.postGame()?.rematch?.opponent shouldBe true }

            bob.send(ClientMessage.PostGameRematch(offer.gameId))
            keepHands(alice, bob, gamesCreated = 2)
            eventually(5.seconds) {
                (alice.latestState() != null && bob.latestState() != null) shouldBe true
            }
        }

        test("leaving the result screen takes the rematch off the table for the other player") {
            val (carol, dave) = startQuickGame("Carol Leave", "Dave Leave")
            dave.send(ClientMessage.Concede())
            val offer = eventually(5.seconds) { carol.postGame().shouldNotBeNull() }

            dave.send(ClientMessage.PostGameLeave(offer.gameId))
            eventually(5.seconds) { carol.postGame()?.opponentLeft shouldBe true }
        }

        test("emotes reach the whole table, are rate-limited, and stop at a block") {
            val (erin, frank) = startQuickGame("Erin Emote", "Frank Emote")
            eventually(5.seconds) { (erin.latestState() != null && frank.latestState() != null) shouldBe true }

            erin.send(ClientMessage.SendEmote(Emote.HELLO))
            eventually(5.seconds) {
                frank.emotes().map { it.emote } shouldBe listOf(Emote.HELLO)
                erin.emotes().map { it.emote } shouldBe listOf(Emote.HELLO)
            }

            // Inside the minimum gap: dropped.
            erin.send(ClientMessage.SendEmote(Emote.MY_BAD))
            delay(500)
            frank.emotes() shouldHaveSize 1

            // Frank blocks Erin after the game; their next game's emotes don't reach him.
            frank.send(ClientMessage.Concede())
            val offer = eventually(5.seconds) { frank.postGame().shouldNotBeNull() }
            frank.send(ClientMessage.PostGameBlock(offer.gameId))
            eventually(5.seconds) {
                frank.postGame()?.blocked shouldBe true
                // To Erin it reads as Frank leaving; nothing names the block.
                erin.postGame()?.opponentLeft shouldBe true
            }
        }

        test("a blocked pair is never matched") {
            val (gina, hal) = startQuickGame("Gina Block", "Hal Block")
            gina.send(ClientMessage.Concede())
            val offer = eventually(5.seconds) { gina.postGame().shouldNotBeNull() }
            gina.send(ClientMessage.PostGameBlock(offer.gameId))
            eventually(5.seconds) { gina.postGame()?.blocked shouldBe true }
            gina.send(ClientMessage.PostGameLeave(offer.gameId))
            hal.send(ClientMessage.PostGameLeave(offer.gameId))

            gina.send(ClientMessage.JoinMatchmaking(format = com.wingedsheep.sdk.core.DeckFormat.VINTAGE))
            hal.send(ClientMessage.JoinMatchmaking(format = com.wingedsheep.sdk.core.DeckFormat.VINTAGE))
            eventually(5.seconds) {
                hal.messages.filterIsInstance<ServerMessage.MatchmakingStatus>().lastOrNull()?.searching shouldBe true
            }
            delay(2_500)
            gina.messages.filterIsInstance<ServerMessage.MatchFound>() shouldHaveSize 0
            gina.send(ClientMessage.LeaveMatchmaking)
            hal.send(ClientMessage.LeaveMatchmaking)
        }

        test("a player warming up against the AI stays queued, and a match ends the warm-up for the lobby") {
            val ivy = createClient().apply { connectAs("Ivy Warmup") }
            val jon = createClient().apply { connectAs("Jon Warmup") }

            ivy.send(ClientMessage.JoinMatchmaking(format = com.wingedsheep.sdk.core.DeckFormat.LEGACY))
            ivy.send(ClientMessage.CreateQuickGameLobby(vsAi = true))
            eventually(5.seconds) {
                ivy.messages.filterIsInstance<ServerMessage.QuickGameLobbyState>().lastOrNull().shouldNotBeNull()
            }
            ivy.send(ClientMessage.SubmitQuickGameLobbyDeck(deckList = emptyMap()))
            ivy.send(ClientMessage.SetQuickGameLobbyReady(true))
            eventually(10.seconds) { ivy.messages.count { it is ServerMessage.GameCreated } shouldBe 1 }
            // Still searching through the AI game's first ticks.
            delay(1_500)
            ivy.messages.filterIsInstance<ServerMessage.MatchmakingStatus>().last().searching shouldBe true

            jon.send(ClientMessage.JoinMatchmaking(format = com.wingedsheep.sdk.core.DeckFormat.LEGACY))
            val offers = listOf(ivy, jon).map { client ->
                eventually(5.seconds) { client.messages.filterIsInstance<ServerMessage.MatchFound>().lastOrNull().shouldNotBeNull() }
            }
            ivy.send(ClientMessage.RespondToMatch(offers[0].matchId, accept = true))
            jon.send(ClientMessage.RespondToMatch(offers[1].matchId, accept = true))

            eventually(10.seconds) {
                val lobby = ivy.messages.filterIsInstance<ServerMessage.QuickGameLobbyState>().last()
                lobby.matchmade shouldBe true
                lobby.players.map { it.playerName }.toSet() shouldBe setOf("Ivy Warmup", "Jon Warmup")
            }
            // The warm-up ended without a game-over screen for Ivy.
            ivy.messages.filterIsInstance<ServerMessage.GameOver>() shouldHaveSize 0
        }
    }
}
