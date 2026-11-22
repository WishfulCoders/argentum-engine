package com.wingedsheep.gameserver.session

import com.wingedsheep.gameserver.handler.MessageSender
import com.wingedsheep.gameserver.protocol.ServerMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus

/**
 * Tells every connected player the server is going down for a restart (a deploy, in practice)
 * before their sockets drop, so the client can say "updating, back in a minute or two" instead
 * of looking broken.
 *
 * Runs on [ContextClosedEvent], which Spring publishes on SIGTERM *before* stopping the web
 * server, so the sockets are still open. Each live socket gets [ServerMessage.ServerRestarting]
 * and is then closed with 1012 (Service Restart). Games and lobbies are already persisted (Redis);
 * [SessionRecoveryService] restores the identities on the next boot, and the clients' reconnect
 * lands them back where they were.
 *
 * [isShuttingDown] lets the disconnect path skip its usual reactions — the opponent-disconnected
 * countdown and abandonment timers — which would otherwise fire for every socket this closes and
 * tell each player their opponent walked out.
 */
@Component
class ServerRestartNotice(
    private val sessionRegistry: SessionRegistry,
    private val sender: MessageSender,
    @param:Value("\${app.restart-expected-downtime-seconds:60}")
    private val expectedDowntimeSeconds: Int,
) {
    private val logger = LoggerFactory.getLogger(ServerRestartNotice::class.java)

    @Volatile
    private var shuttingDown = false

    val isShuttingDown: Boolean get() = shuttingDown

    @EventListener(ContextClosedEvent::class)
    fun onContextClosed() {
        if (shuttingDown) return
        shuttingDown = true

        val message = ServerMessage.ServerRestarting(expectedDowntimeSeconds)
        var notified = 0
        sessionRegistry.forEachIdentity { _, identity ->
            if (identity.isAi) return@forEachIdentity
            val ws = identity.webSocketSession ?: return@forEachIdentity
            if (!ws.isOpen) return@forEachIdentity
            sender.send(ws, message)
            try {
                ws.close(CloseStatus.SERVICE_RESTARTED)
            } catch (e: Exception) {
                logger.debug("Closing ${ws.id} on shutdown failed", e)
            }
            notified++
        }
        logger.info("Server shutting down — told $notified connected player(s) to expect a restart")
    }
}
