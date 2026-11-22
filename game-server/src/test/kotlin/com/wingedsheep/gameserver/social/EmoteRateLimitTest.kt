package com.wingedsheep.gameserver.social

import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

class EmoteRateLimitTest : FunSpec({

    fun service() = EmoteService(mockk(), mockk(), mockk(), BlockService(NoAccountBlockStore()))
    val alice = EntityId("alice")

    test("two emotes inside the minimum gap: the second is dropped") {
        val s = service()
        s.allow(alice, 0) shouldBe true
        s.allow(alice, EmoteService.MIN_GAP_MS - 1) shouldBe false
        s.allow(alice, EmoteService.MIN_GAP_MS) shouldBe true
    }

    test("a burst is capped per window, then the window frees up") {
        val s = service()
        val gap = EmoteService.MIN_GAP_MS
        (0 until EmoteService.BURST).forEach { s.allow(alice, it * gap) shouldBe true }
        s.allow(alice, EmoteService.BURST * gap) shouldBe false
        s.allow(alice, EmoteService.WINDOW_MS + gap) shouldBe true
    }

    test("each player has their own allowance") {
        val s = service()
        s.allow(alice, 0) shouldBe true
        s.allow(EntityId("bob"), 0) shouldBe true
    }
})
