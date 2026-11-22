package com.wingedsheep.search

internal actual fun compileQueryRegex(pattern: String, flags: String): (String) -> Boolean {
    val compiled: dynamic = try {
        js("new RegExp(pattern, flags)")
    } catch (e: Throwable) {
        throw IllegalArgumentException(e.message ?: "invalid pattern", e)
    }
    return { input -> compiled.test(input).unsafeCast<Boolean>() }
}

internal actual fun String.containsText(needle: String): Boolean =
    asDynamic().indexOf(needle).unsafeCast<Int>() >= 0
