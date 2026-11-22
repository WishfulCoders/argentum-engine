package com.wingedsheep.gameserver.controller

import java.security.SecureRandom

/**
 * Public cube link codes: 12 characters of base62 (~71 bits), so a link can be pasted in chat and
 * still not be enumerated. The database's unique index is the backstop against the negligible
 * chance of a collision.
 */
internal object CubeShareCodes {
    private const val LENGTH = 12
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    private val random = SecureRandom()

    fun next(): String = buildString(LENGTH) {
        repeat(LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
    }

    /** Cheap shape check so a malformed path never reaches the database. */
    fun isWellFormed(code: String): Boolean = code.length == LENGTH && code.all { it in ALPHABET }
}
