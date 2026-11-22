package com.wingedsheep.gameserver.session

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.springframework.web.socket.WebSocketSession

/**
 * A browser seat can't follow a card by its id once it has lost track of the card.
 *
 * An engine id stays with its card all game. Without per-seat names, an opponent who saw two cards
 * revealed in a hand could tell which one was later cast face down, and a card seen before a
 * shuffle could be recognised when it came back as a manifest.
 */
class SeatIdentityTest : ScenarioTestBase() {

    private val json = Json { encodeDefaults = true; classDiscriminator = "type"; serializersModule = engineSerializersModule }

    private fun players(game: TestGame) = listOf(game.player1Id, game.player2Id).associateWith { player ->
        val socket = mockk<WebSocketSession>(relaxed = true) { every { id } returns player.value }
        PlayerSession(socket, player, player.value)
    }

    /** A full update for [seat], as a browser gets it on (re)connecting. */
    private fun GameSession.fullUpdate(seat: EntityId, state: GameState, game: TestGame): ServerMessage.StateUpdate {
        injectStateForTesting(state, players(game))
        clearLastSentState(seat)
        return createStateUpdate(seat, emptyList()) as ServerMessage.StateUpdate
    }

    /** A full update for [seat] carrying [events], as a browser gets it on (re)connecting. */
    private fun GameSession.fullUpdate(
        seat: EntityId,
        state: GameState,
        game: TestGame,
        events: List<GameEvent>,
    ): ServerMessage.StateUpdate {
        injectStateForTesting(state, players(game))
        clearLastSentState(seat)
        return createStateUpdate(seat, events) as ServerMessage.StateUpdate
    }

    /**
     * Two worlds that differ only in a card P1 never sees, played to the point where P2 casts
     * [cast] (picked from the bounced card and the drawn one). Returns every message P1 got.
     *
     * On P2's turn P1 Unsummons P2's [bounced], so the whole table knows it in P2's hand; then P2
     * draws [drawn] unseen.
     */
    private fun messagesToViewer(
        bounced: String,
        drawn: String,
        castFaceDown: Boolean,
        cast: (bounced: EntityId, drawn: EntityId) -> EntityId,
    ): List<ServerMessage.StateUpdate> {
        val game = scenario()
            .withPlayers()
            .withCardInHand(1, "Unsummon")
            .withLandsOnBattlefield(1, "Island", 1)
            .withCardOnBattlefield(2, bounced)
            .withLandsOnBattlefield(2, "Forest", 3)
            .withCardInLibrary(2, drawn)
            .withActivePlayer(2)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        val viewer = game.player1Id
        val owner = game.player2Id
        val bouncedId = game.findPermanent(bounced)!!
        val drawnId = game.state.getLibrary(owner).single()
        val session = GameSession(cardRegistry = cardRegistry)
        val messages = mutableListOf(session.fullUpdate(viewer, game.state, game))

        val unsummon = game.castSpell(1, "Unsummon", bouncedId)
        unsummon.error shouldBe null
        messages += session.fullUpdate(viewer, game.state, game, unsummon.events)
        val resolved = game.resolveStack()
        messages += session.fullUpdate(viewer, game.state, game, resolved.flatMap { it.events })
        game.state.getHand(owner) shouldBe listOf(bouncedId)

        game.state = game.state.moveToZone(drawnId, ZoneKey(owner, Zone.LIBRARY), ZoneKey(owner, Zone.HAND))
        messages += session.fullUpdate(viewer, game.state, game)

        val spell = game.execute(CastSpell(owner, cast(bouncedId, drawnId), castFaceDown = castFaceDown))
        spell.error shouldBe null
        game.state.stack shouldHaveSize 1
        messages += session.fullUpdate(viewer, game.state, game, spell.events)
        return messages
    }

    /**
     * [messages] as JSON, each card id replaced by its order of first appearance; whether an id is
     * an engine id or a seat name is kept, so a difference in form shows too. Each session's
     * interaction epoch is its own, so it's left out.
     */
    private fun relabelled(messages: List<ServerMessage.StateUpdate>): List<String> {
        val labels = HashMap<String, String>()
        val id = Regex("\"(card-\\d+|h\\d+)\"")
        return messages.map { message ->
            id.replace(json.encodeToString(ServerMessage.serializer(), message.copy(interactionEpoch = ""))) { match ->
                val raw = match.groupValues[1]
                "\"" + labels.getOrPut(raw) { "${raw.takeWhile { !it.isDigit() }}#${labels.size}" } + "\""
            }
        }
    }

    private fun ServerMessage.StateUpdate.battlefieldOf(player: EntityId): List<EntityId> =
        state.zones.single { it.zoneId == ZoneKey(player, Zone.BATTLEFIELD) }.cardIds

    init {
        test("an opponent shown two cards can't tell which one was cast face down") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Hill Giant")
                .build()
            val viewer = game.player1Id
            val owner = game.player2Id
            val (bears, giant) = game.state.getHand(owner)
            val revealed = listOf(bears, giant).fold(game.state) { state, card ->
                state.updateEntity(card) { it.with(RevealedToComponent.to(viewer)) }
            }
            val session = GameSession(cardRegistry = cardRegistry)
            session.fullUpdate(viewer, revealed, game).state.cards.keys shouldContainAll listOf(bears, giant)

            // One of them is cast face down, and the reveal of the other lapses.
            val faceDown = revealed
                .moveToZone(bears, ZoneKey(owner, Zone.HAND), ZoneKey(owner, Zone.BATTLEFIELD))
                .updateEntity(bears) { it.without<RevealedToComponent>().with(FaceDownComponent) }
                .updateEntity(giant) { it.without<RevealedToComponent>() }
            val update = session.fullUpdate(viewer, faceDown, game)
            val name = update.battlefieldOf(owner).single()

            name shouldNotBe bears
            name shouldNotBe giant
            json.encodeToString(ServerMessage.serializer(), update) shouldNotContain "\"${bears.value}\""
            // Watching the face-down permanent, the seat keeps its name.
            session.fullUpdate(viewer, faceDown, game).battlefieldOf(owner).single() shouldBe name
            // The owner saw its own card leave its hand, so it keeps following it.
            session.fullUpdate(owner, faceDown, game).battlefieldOf(owner).single() shouldBe bears
            // The seat's name reaches the card; the old one can't be used to probe which card it is.
            session.fromSeat(viewer, listOf(name), ListSerializer(EntityId.serializer())) shouldBe listOf(bears)
            session.fromSeat(viewer, listOf(bears), ListSerializer(EntityId.serializer())) shouldBe null
        }

        test("a card seen before it was shuffled away comes back under a new name") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()
            val viewer = game.player1Id
            val owner = game.player2Id
            val bears = game.findPermanent("Grizzly Bears")!!
            val session = GameSession(cardRegistry = cardRegistry)
            session.fullUpdate(viewer, game.state, game).battlefieldOf(owner) shouldBe listOf(bears)

            val tucked = game.state.moveToZone(bears, ZoneKey(owner, Zone.BATTLEFIELD), ZoneKey(owner, Zone.LIBRARY))
            val whileHidden = session.fullUpdate(viewer, tucked, game)
            // While the renamed card is out of sight, nothing in the message needs renaming.
            session.clearLastSentState(viewer)
            (session.createStateUpdate(viewer, emptyList(), useEngineDecisionIds = true) as ServerMessage.StateUpdate)
                .state shouldBe whileHidden.state
            val manifested = tucked
                .moveToZone(bears, ZoneKey(owner, Zone.LIBRARY), ZoneKey(owner, Zone.BATTLEFIELD))
                .updateEntity(bears) { it.with(FaceDownComponent) }

            val renamed = session.fullUpdate(viewer, manifested, game).battlefieldOf(owner).single()
            renamed shouldNotBe bears
            // A restart keeps the seat's names, so the card doesn't come back under its engine id.
            val restored = GameSession(cardRegistry = cardRegistry)
            restored.restoreFromPersistence(
                manifested, emptyMap(), mutableMapOf(), emptyMap(),
                seatNames = session.getSeatNamesForPersistence(),
            )
            restored.fullUpdate(viewer, manifested, game).battlefieldOf(owner).single() shouldBe renamed
            // In-process AI seats read raw engine state and keep engine ids.
            session.injectStateForTesting(manifested, players(game))
            session.clearLastSentState(viewer)
            (session.createStateUpdate(viewer, emptyList(), useEngineDecisionIds = true) as ServerMessage.StateUpdate)
                .battlefieldOf(owner).single() shouldBe bears
        }

        test("a seat can't tell the known copy of a card from another copy cast from the same hand") {
            // The bounced Bears is cast in one world; a second Bears, drawn unseen, in the other.
            val knownCast = messagesToViewer("Grizzly Bears", "Hill Giant", castFaceDown = false) { bounced, _ -> bounced }
            val otherCast = messagesToViewer("Grizzly Bears", "Grizzly Bears", castFaceDown = false) { _, drawn -> drawn }

            relabelled(knownCast) shouldBe relabelled(otherCast)
            val spell = knownCast.last().state.zones.single { it.zoneId.zoneType == Zone.STACK }.cardIds.single()
            // The known Bears reaches the stack under a name the seat hasn't been sent before.
            knownCast.dropLast(1).forEach {
                json.encodeToString(ServerMessage.serializer(), it) shouldNotContain "\"${spell.value}\""
            }
        }

        test("a seat can't tell a face-down spell is the morph it saw in hand") {
            val knownCast = messagesToViewer("Exalted Angel", "Battering Craghorn", castFaceDown = true) { bounced, _ -> bounced }
            val otherCast = messagesToViewer("Exalted Angel", "Battering Craghorn", castFaceDown = true) { _, drawn -> drawn }

            relabelled(knownCast) shouldBe relabelled(otherCast)
        }
    }
}
