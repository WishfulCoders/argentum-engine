package com.wingedsheep.gameserver.controller

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class CubeShareCodesTest : FunSpec({
    test("minted codes are well formed and do not repeat") {
        val codes = List(500) { CubeShareCodes.next() }
        codes.forEach { CubeShareCodes.isWellFormed(it) shouldBe true }
        codes.toSet() shouldHaveSize 500
    }

    test("malformed codes are rejected before any lookup") {
        CubeShareCodes.isWellFormed("") shouldBe false
        CubeShareCodes.isWellFormed("short") shouldBe false
        CubeShareCodes.isWellFormed("abc-def_ghij") shouldBe false
        CubeShareCodes.isWellFormed("abcdefghijklm") shouldBe false
    }
})
