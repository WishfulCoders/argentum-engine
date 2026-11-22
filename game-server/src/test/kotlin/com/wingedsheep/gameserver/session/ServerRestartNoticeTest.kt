package com.wingedsheep.gameserver.session

import com.wingedsheep.gameserver.handler.MessageSender
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession

class ServerRestartNoticeTest : FunSpec({
    fun openSocket(id: String, sent: MutableList<String>): WebSocketSession = mockk(relaxed = true) {
        every { this@mockk.id } returns id
        every { isOpen } returns true
        val msg = slot<TextMessage>()
        every { sendMessage(capture(msg)) } answers { sent += msg.captured.payload }
    }

    test("shutdown tells every connected human to expect a restart, then closes with 1012") {
        val registry = SessionRegistry()
        val sent = mutableListOf<String>()
        val ws = openSocket("ws-1", sent)
        val human = PlayerIdentity(token = "human", playerId = EntityId.of("p1"), playerName = "Alice")
        registry.preRegisterIdentity(human)
        human.webSocketSession = ws
        registry.preRegisterIdentity(
            PlayerIdentity(token = "ai", playerId = EntityId.of("p2"), playerName = "Bot", isAi = true)
        )

        val notice = ServerRestartNotice(registry, MessageSender(registry), expectedDowntimeSeconds = 90)
        notice.isShuttingDown shouldBe false

        notice.onContextClosed()

        notice.isShuttingDown shouldBe true
        sent.size shouldBe 1
        sent[0] shouldContain "\"type\":\"serverRestarting\""
        sent[0] shouldContain "\"expectedDowntimeSeconds\":90"
        verify(exactly = 1) { ws.close(CloseStatus.SERVICE_RESTARTED) }
    }

    test("a second close event does not notify twice") {
        val registry = SessionRegistry()
        val sent = mutableListOf<String>()
        val human = PlayerIdentity(token = "human", playerId = EntityId.of("p1"), playerName = "Alice")
        registry.preRegisterIdentity(human)
        human.webSocketSession = openSocket("ws-1", sent)

        val notice = ServerRestartNotice(registry, MessageSender(registry), expectedDowntimeSeconds = 90)
        notice.onContextClosed()
        notice.onContextClosed()

        sent.size shouldBe 1
    }
})
