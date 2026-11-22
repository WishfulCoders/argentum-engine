package com.wingedsheep.gameserver.lobby

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A Limited tournament played for ante settles into the players' pools: the card a player loses
 * leaves their pool and deck for the rest of the event, and the winner gains it in their sideboard.
 */
class AntePoolTransferTest : FunSpec({
    val host = EntityId("host")
    val guest = EntityId("guest")

    fun bear(n: Int) = CardDefinition.creature(
        name = "Bear $n",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAR),
        power = 2,
        toughness = 2,
    )

    fun lobby(format: TournamentFormat = TournamentFormat.SEALED, ante: Boolean = true): TournamentLobby {
        val lobby = TournamentLobby(
            setCodes = listOf("TST"),
            setNames = listOf("Test"),
            boosterGenerator = BoosterGenerator(emptyMap()),
            format = format,
            playingForAnte = ante,
        )
        lobby.addPlayer(PlayerIdentity(playerId = host, playerName = "Host"))
        lobby.addPlayer(PlayerIdentity(playerId = guest, playerName = "Guest"))
        // Host: 23 spells from a pool of 25, plus 17 Forests — exactly 40.
        val hostPool = (1..25).map(::bear)
        lobby.players[host] = lobby.players.getValue(host).copy(
            cardPool = hostPool,
            submittedDeck = (1..23).associate { "Bear $it" to 1 } + ("Forest" to 17),
        )
        lobby.players[guest] = lobby.players.getValue(guest).copy(
            cardPool = (26..50).map(::bear),
            submittedDeck = (26..48).associate { "Bear $it" to 1 } + ("Forest" to 17),
        )
        return lobby
    }

    test("the lost card moves from the loser's pool and deck to the winner's sideboard") {
        val lobby = lobby()
        lobby.anteSettlesIntoPools shouldBe true

        val change = lobby.transferAnteCard(host, guest, "Bear 7")

        change?.cardName shouldBe "Bear 7"
        lobby.players.getValue(host).cardPool.none { it.name == "Bear 7" } shouldBe true
        lobby.getSubmittedDeck(host)!!.containsKey("Bear 7") shouldBe false
        lobby.players.getValue(guest).cardPool.count { it.name == "Bear 7" } shouldBe 1
        lobby.getSubmittedSideboard(guest)["Bear 7"] shouldBe 1
    }

    test("a deck that drops below 40 is topped up with its most-played basic land") {
        val lobby = lobby()

        val change = lobby.transferAnteCard(host, guest, "Bear 7")

        change?.refilledWith shouldBe "Forest"
        lobby.getSubmittedDeck(host)!!.values.sum() shouldBe 40
        lobby.getSubmittedDeck(host)!!["Forest"] shouldBe 18
    }

    test("a card from the loser's sideboard leaves the pool without touching the deck") {
        val lobby = lobby()

        val change = lobby.transferAnteCard(host, guest, "Bear 25")

        change?.refilledWith shouldBe null
        lobby.getSubmittedDeck(host)!!.values.sum() shouldBe 40
        lobby.getSubmittedSideboard(host).containsKey("Bear 25") shouldBe false
    }

    test("basic lands are supplied, not owned, so winning one changes nobody's pool") {
        lobby().transferAnteCard(host, guest, "Forest") shouldBe null
    }

    test("constructed decks and lobbies not played for ante don't settle into pools") {
        lobby(format = TournamentFormat.PREMADE_DECKS).anteSettlesIntoPools shouldBe false
        lobby(ante = false).anteSettlesIntoPools shouldBe false
    }
})
