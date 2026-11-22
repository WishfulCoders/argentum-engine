package com.wingedsheep.gameserver.social

import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

class BlockServiceTest : FunSpec({

    val guest = Party(EntityId("guest"), userId = null)
    val alice = Party(EntityId("alice"), UUID.randomUUID())
    val bob = Party(EntityId("bob"), UUID.randomUUID())

    test("a guest's block lasts the session and works both ways for pairing") {
        val blocks = BlockService(NoAccountBlockStore())
        blocks.block(guest, alice)

        blocks.hasBlocked(guest, alice) shouldBe true
        blocks.hasBlocked(alice, guest) shouldBe false
        blocks.blockedEitherWay(alice, guest) shouldBe true

        blocks.unblock(guest, alice)
        blocks.blockedEitherWay(alice, guest) shouldBe false
    }

    test("a block between two accounts is stored, and the pairing cache sees it at once") {
        val store = mockk<AccountBlockStore>(relaxed = true)
        every { store.blockedEitherWay(alice.userId!!) } returns emptySet() andThen setOf(bob.userId!!)
        val blocks = BlockService(store)

        blocks.blockedEitherWay(alice, bob) shouldBe false
        blocks.block(alice, bob)
        verify { store.block(alice.userId!!, bob.userId!!) }
        blocks.blockedEitherWay(alice, bob) shouldBe true
    }
})
