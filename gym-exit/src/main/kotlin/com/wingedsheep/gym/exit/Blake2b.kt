package com.wingedsheep.gym.exit

/**
 * BLAKE2b (RFC 7693), unkeyed, for `search.py`'s `world_seed`:
 * `int.from_bytes(blake2b(repr(parts).encode(), digest_size=8).digest(), "big") >> 2`. With it the JVM determinizes
 * exactly the worlds T1 did, so E1 and T1 can be compared game by game, not only in distribution.
 */
object Blake2b {
    private val IV = longArrayOf(
        0x6a09e667f3bcc908L, -0x4498517a7b3558c5L, 0x3c6ef372fe94f82bL, -0x5ab00ac5a0e2c90fL,
        0x510e527fade682d1L, -0x64fa9773d4c193e1L, 0x1f83d9abfb41bd6bL, 0x5be0cd19137e2179L,
    )
    private val SIGMA = arrayOf(
        intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15),
        intArrayOf(14, 10, 4, 8, 9, 15, 13, 6, 1, 12, 0, 2, 11, 7, 5, 3),
        intArrayOf(11, 8, 12, 0, 5, 2, 15, 13, 10, 14, 3, 6, 7, 1, 9, 4),
        intArrayOf(7, 9, 3, 1, 13, 12, 11, 14, 2, 6, 5, 10, 4, 0, 15, 8),
        intArrayOf(9, 0, 5, 7, 2, 4, 10, 15, 14, 1, 11, 12, 6, 8, 3, 13),
        intArrayOf(2, 12, 6, 10, 0, 11, 8, 3, 4, 13, 7, 5, 15, 14, 1, 9),
        intArrayOf(12, 5, 1, 15, 14, 13, 4, 10, 0, 7, 6, 3, 9, 2, 8, 11),
        intArrayOf(13, 11, 7, 14, 12, 1, 3, 9, 5, 0, 15, 4, 8, 6, 2, 10),
        intArrayOf(6, 15, 14, 9, 11, 3, 0, 8, 12, 2, 13, 7, 1, 4, 10, 5),
        intArrayOf(10, 2, 8, 4, 7, 6, 1, 5, 15, 11, 9, 14, 3, 12, 13, 0),
    )

    fun digest(input: ByteArray, outLen: Int): ByteArray {
        require(outLen in 1..64)
        val h = IV.copyOf()
        h[0] = h[0] xor (0x01010000L or outLen.toLong())
        val blocks = maxOf(1, (input.size + 127) / 128)
        for (b in 0 until blocks) {
            val block = ByteArray(128)
            val start = b * 128
            val len = minOf(128, input.size - start).coerceAtLeast(0)
            System.arraycopy(input, start, block, 0, len)
            val last = b == blocks - 1
            compress(h, block, (start + len).toLong(), last)
        }
        return ByteArray(outLen) { (h[it / 8] ushr (8 * (it % 8))).toByte() }
    }

    private fun compress(h: LongArray, block: ByteArray, counter: Long, last: Boolean) {
        val m = LongArray(16) { i ->
            var x = 0L
            for (j in 7 downTo 0) x = (x shl 8) or (block[i * 8 + j].toLong() and 0xff)
            x
        }
        val v = LongArray(16)
        for (i in 0 until 8) { v[i] = h[i]; v[i + 8] = IV[i] }
        v[12] = v[12] xor counter
        if (last) v[14] = v[14].inv()
        fun g(a: Int, b: Int, c: Int, d: Int, x: Long, y: Long) {
            v[a] = v[a] + v[b] + x; v[d] = (v[d] xor v[a]).rotateRight(32)
            v[c] = v[c] + v[d]; v[b] = (v[b] xor v[c]).rotateRight(24)
            v[a] = v[a] + v[b] + y; v[d] = (v[d] xor v[a]).rotateRight(16)
            v[c] = v[c] + v[d]; v[b] = (v[b] xor v[c]).rotateRight(63)
        }
        for (r in 0 until 12) {
            val s = SIGMA[r % 10]
            g(0, 4, 8, 12, m[s[0]], m[s[1]]); g(1, 5, 9, 13, m[s[2]], m[s[3]])
            g(2, 6, 10, 14, m[s[4]], m[s[5]]); g(3, 7, 11, 15, m[s[6]], m[s[7]])
            g(0, 5, 10, 15, m[s[8]], m[s[9]]); g(1, 6, 11, 12, m[s[10]], m[s[11]])
            g(2, 7, 8, 13, m[s[12]], m[s[13]]); g(3, 4, 9, 14, m[s[14]], m[s[15]])
        }
        for (i in 0 until 8) h[i] = h[i] xor v[i] xor v[i + 8]
    }

    /** `world_seed(*parts)` for integer parts: blake2b-64 of Python's `repr` of the tuple, big-endian, >> 2. */
    fun worldSeed(vararg parts: Long): Long {
        val repr = if (parts.size == 1) "(${parts[0]},)" else parts.joinToString(", ", "(", ")")
        val d = digest(repr.toByteArray(Charsets.UTF_8), 8)
        var x = 0L
        for (b in d) x = (x shl 8) or (b.toLong() and 0xff)
        return x ushr 2
    }
}
