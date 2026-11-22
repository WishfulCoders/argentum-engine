package com.wingedsheep.search

import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

internal actual fun compileQueryRegex(pattern: String, flags: String): (String) -> Boolean {
    var bits = 0
    if ('i' in flags) bits = bits or Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
    if ('m' in flags) bits = bits or Pattern.MULTILINE
    if ('s' in flags) bits = bits or Pattern.DOTALL
    val compiled = try {
        Pattern.compile(pattern, bits)
    } catch (e: PatternSyntaxException) {
        throw IllegalArgumentException(e.description, e)
    }
    return { input -> compiled.matcher(input).find() }
}

internal actual fun String.containsText(needle: String): Boolean = contains(needle)
