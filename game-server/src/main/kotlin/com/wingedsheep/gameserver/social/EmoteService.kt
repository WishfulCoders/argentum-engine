package com.wingedsheep.gameserver.social

import com.wingedsheep.gameserver.ai.AiWebSocketSession
import com.wingedsheep.gameserver.handler.MessageSender
import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.protocol.Emote
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.repository.GameRepository
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.SessionRegistry
import com.wingedsheep.sdk.model.EntityId
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Relays a seated player's preset [Emote] to their table: every seat (the sender included, so their
 * own bubble comes from the same message as everyone else's) and the spectators. Not delivered to
 * anyone who has blocked the sender.
 *
 * Rate-limited per player — at most [BURST] in any [WINDOW_MS], and never two within [MIN_GAP_MS] —
 * because a preset that can be spammed is just a different kind of free text. Over the limit, an
 * emote is dropped silently; the client's own cooldown keeps an honest player from ever hitting it.
 *
 * The AI answers a greeting in kind, a beat later, so saying hello to it isn't met with silence.
 */
@Component
class EmoteService(
    private val sessionRegistry: SessionRegistry,
    private val gameRepository: GameRepository,
    private val sender: MessageSender,
    private val blocks: BlockService,
) {
    private val recent = ConcurrentHashMap<EntityId, ArrayDeque<Long>>()

    fun handle(session: WebSocketSession, message: ClientMessage.SendEmote) {
        val identity = sessionRegistry.getIdentityByWsId(session.id) ?: return
        val game = identity.currentGameSessionId?.let { gameRepository.findById(it) } ?: return
        if (game.isGameOver()) return
        if (game.getPlayers().none { it.playerId == identity.playerId }) return
        if (!allow(identity.playerId, System.currentTimeMillis())) return
        relay(game, identity.playerId, message.emote)

        val reply = AI_REPLIES[message.emote] ?: return
        game.getPlayers()
            .filter { it.webSocketSession is AiWebSocketSession && it.playerId != identity.playerId }
            .forEach { ai ->
                CompletableFuture.runAsync(
                    { if (!game.isGameOver()) relay(game, ai.playerId, reply) },
                    CompletableFuture.delayedExecutor(AI_REPLY_DELAY_MS, TimeUnit.MILLISECONDS),
                )
            }
    }

    internal fun allow(playerId: EntityId, now: Long): Boolean {
        val times = recent.computeIfAbsent(playerId) { ArrayDeque() }
        synchronized(times) {
            while (times.isNotEmpty() && now - times.first() >= WINDOW_MS) times.removeFirst()
            if (times.size >= BURST) return false
            if (times.isNotEmpty() && now - times.last() < MIN_GAP_MS) return false
            times.addLast(now)
            return true
        }
    }

    private fun relay(game: GameSession, from: EntityId, emote: Emote) {
        val message = ServerMessage.EmoteReceived(from, emote)
        val senderParty = partyOf(from)
        for (recipient in game.getPlayers() + game.getSpectators()) {
            val ws = recipient.webSocketSession
            if (ws is AiWebSocketSession || !ws.isOpen) continue
            if (recipient.playerId != from && blocks.hasBlocked(partyOf(recipient.playerId), senderParty)) continue
            sender.send(ws, message)
        }
    }

    private fun partyOf(playerId: EntityId): Party =
        Party(playerId, sessionRegistry.getAllIdentities().firstOrNull { it.playerId == playerId }?.userId)

    companion object {
        const val BURST = 4
        const val WINDOW_MS = 15_000L
        const val MIN_GAP_MS = 1_500L
        private const val AI_REPLY_DELAY_MS = 1_400L

        private val AI_REPLIES = mapOf(
            Emote.HELLO to Emote.HELLO,
            Emote.GOOD_LUCK to Emote.GOOD_LUCK,
            Emote.GOOD_GAME to Emote.GOOD_GAME,
            Emote.WELL_PLAYED to Emote.THANKS,
            Emote.NICE_DECK to Emote.THANKS,
            Emote.NICE_COMBO to Emote.THANKS,
            Emote.NICE_TOPDECK to Emote.THANKS,
        )
    }
}
