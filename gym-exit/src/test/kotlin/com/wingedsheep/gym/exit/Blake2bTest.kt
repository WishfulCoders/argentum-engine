package com.wingedsheep.gym.exit

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class Blake2bTest : FunSpec({
    test("BLAKE2b-512 of the empty string (RFC 7693 test vector)") {
        Blake2b.digest(ByteArray(0), 64).joinToString("") { "%02x".format(it) } shouldBe
            "786a02f742015903c6c6fd852552d272912f4740e15847618a86e217f71f5419" +
            "d25e1031afee585313896444934eb04b903a685b1448b755d56f701afe9be2ce"
    }

    test("world_seed matches search.py's for T1's seed parts") {
        // python3 -c "import hashlib; print(int.from_bytes(hashlib.blake2b(repr((64, 0, 5, 0)).encode(), digest_size=8).digest(), 'big') >> 2)"
        Blake2b.worldSeed(64, 0, 5, 0) shouldBe 477649256562725294L
    }
})
