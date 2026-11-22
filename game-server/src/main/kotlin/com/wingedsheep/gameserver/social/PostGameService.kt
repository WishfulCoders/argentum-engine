package com.wingedsheep.gameserver.social

import com.wingedsheep.gameserver.ai.AiWebSocketSession
import com.wingedsheep.gameserver.friends.FriendsService
import com.wingedsheep.gameserver.handler.MessageSender
import com.wingedsheep.gameserver.handler.QuickGameLobbyHandler
import com.wingedsheep.gameserver.lobby.QuickGameLobbyRepository
import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.protocol.ErrorCode
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.protocol.ServerMessage.FriendshipState
import com.wingedsheep.gameserver.repository.GameRepository
import com.wingedsheep.gameserver.repository.LobbyRepository
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.gameserver.session.SessionRegistry
import com.wingedsheep.sdk.model.EntityId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * The result screen between two human opponents: rematch, add friend, block.
 *
 * When a 1v1 game outside any tournament ends, [onGameOver] opens a post-game for its two seats and
 * sends each a [ServerMessage.PostGame]; every action re-sends it to both, so the client only ever
 * renders what it was told. The post-game closes when the rematch starts, when both players have
 * left the result screen, or after [MAX_AGE_MS].
 *
 * - **Rematch** needs the [RematchRecipe] the quick lobby left behind, so it is offered for quick
 *   games (matchmade or invite) and not, say, for a dev scenario. Both asking starts the new game at
 *   once with the same decks — there is nothing left to choose.
 * - **Add friend** needs both seats signed in; it sends a request, or accepts theirs.
 * - **Block** is described on [BlockService]. To the blocked player it looks like the blocker left.
 */
@Component
class PostGameService(
    private val sessionRegistry: SessionRegistry,
    private val sender: MessageSender,
    private val recipes: RematchRecipes,
    private val quickGameLobbyHandler: QuickGameLobbyHandler,
    private val quickGameLobbies: QuickGameLobbyRepository,
    private val tournamentLobbies: LobbyRepository,
    private val gameRepository: GameRepository,
    private val blocks: BlockService,
    private val friendsProvider: ObjectProvider<FriendsService>,
) {
    private val logger = LoggerFactory.getLogger(PostGameService::class.java)

    private data class Seat(val playerId: EntityId, val name: String, val userId: UUID?) {
        val party: Party get() = Party(playerId, userId)
    }

    private class PostGame(val gameId: String, val seats: List<Seat>, val recipe: RematchRecipe?) {
        val openedAt: Long = System.currentTimeMillis()
        val wantsRematch: MutableSet<EntityId> = ConcurrentHashMap.newKeySet()
        val left: MutableSet<EntityId> = ConcurrentHashMap.newKeySet()
        @Volatile var starting: Boolean = false
        val notices = ConcurrentHashMap<EntityId, String>()

        fun seat(playerId: EntityId): Seat? = seats.firstOrNull { it.playerId == playerId }
        fun opponentOf(playerId: EntityId): Seat = seats.first { it.playerId != playerId }
    }

    private val games = ConcurrentHashMap<String, PostGame>()

    /** Called as a game ends, while its session still lists the seats. */
    fun onGameOver(gameSession: GameSession) {
        val recipe = recipes.take(gameSession.sessionId)
        val players = gameSession.getPlayers()
        if (players.size != 2 || players.any { it.webSocketSession is AiWebSocketSession }) return
        if (players.any { it.playerId in gameSession.departedForMatch }) return
        val seats = players.map { player ->
            val identity = sessionRegistry.getIdentityByWsId(player.webSocketSession.id)
                ?: sessionRegistry.getAllIdentities().firstOrNull { it.playerId == player.playerId }
            Seat(player.playerId, player.playerName, identity?.userId)
        }
        // One account seated twice (two tabs) has nobody to befriend or rematch.
        if (seats[0].userId != null && seats[0].userId == seats[1].userId) return
        val postGame = PostGame(gameSession.sessionId, seats, recipe)
        games[postGame.gameId] = postGame
        broadcast(postGame)
    }

    fun handle(session: WebSocketSession, message: ClientMessage) {
        val identity = sessionRegistry.getIdentityByWsId(session.id) ?: run {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected"); return
        }
        val gameId = when (message) {
            is ClientMessage.PostGameRematch -> message.gameId
            is ClientMessage.PostGameAddFriend -> message.gameId
            is ClientMessage.PostGameBlock -> message.gameId
            is ClientMessage.PostGameLeave -> message.gameId
            else -> return
        }
        val postGame = games[gameId] ?: return
        val me = postGame.seat(identity.playerId) ?: return
        val them = postGame.opponentOf(me.playerId)
        // Both seats can click Rematch in the same instant; only one of them may start the game.
        synchronized(postGame) {
            when (message) {
                is ClientMessage.PostGameRematch -> rematch(postGame, me, them, message.want)
                is ClientMessage.PostGameAddFriend -> addFriend(postGame, me, them)
                is ClientMessage.PostGameBlock -> block(postGame, me, them, message.block)
                is ClientMessage.PostGameLeave -> leave(postGame, me)
                else -> {}
            }
        }
    }

    private fun rematch(postGame: PostGame, me: Seat, them: Seat, want: Boolean) {
        if (postGame.recipe == null || postGame.starting) return
        if (want && (them.playerId in postGame.left || blocks.blockedEitherWay(me.party, them.party))) {
            sendTo(me.playerId, stateFor(postGame, me))
            return
        }
        if (want) postGame.wantsRematch += me.playerId else postGame.wantsRematch -= me.playerId
        if (postGame.wantsRematch.size < 2) {
            broadcast(postGame)
            return
        }
        postGame.starting = true
        broadcast(postGame)
        val busy = postGame.seats.firstOrNull { seat -> identityOf(seat.playerId)?.let(::isBusyElsewhere) ?: true }
        val started = busy == null && runCatching { quickGameLobbyHandler.startRematch(postGame.recipe) }
            .onFailure { logger.error("Rematch for game ${postGame.gameId} failed to start", it) }
            .getOrDefault(false)
        if (started) {
            logger.info("Rematch of game ${postGame.gameId} started")
            games.remove(postGame.gameId)
            return
        }
        // Couldn't seat them (one disconnected, or a start-time check closed the lobby): say so and
        // let them try again or move on.
        postGame.starting = false
        postGame.wantsRematch.clear()
        val reason = busy?.let { "${it.name} is no longer available" } ?: "The rematch couldn't start"
        postGame.seats.forEach { postGame.notices[it.playerId] = reason }
        broadcast(postGame)
    }

    private fun addFriend(postGame: PostGame, me: Seat, them: Seat) {
        val friends = friendsProvider.ifAvailable ?: return
        val a = me.userId ?: return
        val b = them.userId ?: return
        if (blocks.blockedEitherWay(me.party, them.party)) return
        friends.befriend(a, b)
        broadcast(postGame)
    }

    private fun block(postGame: PostGame, me: Seat, them: Seat, block: Boolean) {
        if (block) {
            blocks.block(me.party, them.party)
            // From the other side a block reads as the blocker leaving ([stateFor]); nothing names it.
            postGame.wantsRematch.clear()
        } else {
            blocks.unblock(me.party, them.party)
        }
        broadcast(postGame)
    }

    private fun leave(postGame: PostGame, me: Seat) {
        postGame.left += me.playerId
        postGame.wantsRematch -= me.playerId
        if (postGame.seats.all { it.playerId in postGame.left }) {
            games.remove(postGame.gameId)
            return
        }
        broadcast(postGame)
    }

    private fun stateFor(postGame: PostGame, me: Seat): ServerMessage.PostGame {
        val them = postGame.opponentOf(me.playerId)
        val blockedByMe = blocks.hasBlocked(me.party, them.party)
        val blockedEitherWay = blockedByMe || blocks.hasBlocked(them.party, me.party)
        return ServerMessage.PostGame(
            gameId = postGame.gameId,
            opponentName = them.name,
            canRematch = postGame.recipe != null,
            rematch = ServerMessage.RematchState(
                you = me.playerId in postGame.wantsRematch,
                opponent = them.playerId in postGame.wantsRematch,
                starting = postGame.starting,
            ),
            opponentLeft = them.playerId in postGame.left || (blockedEitherWay && !blockedByMe),
            friendship = if (blockedEitherWay) FriendshipState.UNAVAILABLE else friendshipOf(me, them),
            blocked = blockedByMe,
            notice = postGame.notices.remove(me.playerId),
        )
    }

    private fun friendshipOf(me: Seat, them: Seat): FriendshipState {
        val friends = friendsProvider.ifAvailable ?: return FriendshipState.UNAVAILABLE
        val a = me.userId ?: return FriendshipState.UNAVAILABLE
        val b = them.userId ?: return FriendshipState.UNAVAILABLE
        return when (friends.relationship(a, b)) {
            FriendsService.Relationship.NONE -> FriendshipState.NONE
            FriendsService.Relationship.REQUEST_SENT -> FriendshipState.REQUEST_SENT
            FriendsService.Relationship.REQUEST_RECEIVED -> FriendshipState.REQUEST_RECEIVED
            FriendsService.Relationship.FRIENDS -> FriendshipState.FRIENDS
        }
    }

    /** Everyone still on the result screen gets their own view of it. */
    private fun broadcast(postGame: PostGame) {
        postGame.seats
            .filterNot { it.playerId in postGame.left }
            .forEach { sendTo(it.playerId, stateFor(postGame, it)) }
    }

    private fun identityOf(playerId: EntityId): PlayerIdentity? =
        sessionRegistry.getAllIdentities().firstOrNull { it.playerId == playerId }

    /** Disconnected, or already in a lobby or an unfinished game. */
    private fun isBusyElsewhere(identity: PlayerIdentity): Boolean =
        !identity.isConnected ||
            identity.currentQuickGameLobbyId?.let { quickGameLobbies.findById(it) } != null ||
            identity.currentLobbyId?.let { tournamentLobbies.findLobbyById(it) } != null ||
            identity.currentGameSessionId?.let { gameRepository.findById(it) }?.isGameOver() == false

    private fun sendTo(playerId: EntityId, message: ServerMessage) {
        val ws = identityOf(playerId)?.webSocketSession
        if (ws != null && ws.isOpen) sender.send(ws, message)
    }

    @Scheduled(fixedDelay = SWEEP_MS, initialDelay = SWEEP_MS)
    fun sweep() {
        val cutoff = System.currentTimeMillis() - MAX_AGE_MS
        games.values.removeIf { it.openedAt < cutoff && !it.starting }
    }

    companion object {
        private const val SWEEP_MS = 60_000L
        const val MAX_AGE_MS = 15 * 60_000L
    }
}
