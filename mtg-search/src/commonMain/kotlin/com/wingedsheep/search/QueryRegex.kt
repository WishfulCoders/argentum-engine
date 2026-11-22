package com.wingedsheep.search

/**
 * Compile a regex the user typed (`name:/^bolt/`, `o:/draw .* card/`) with the platform's own
 * engine — `java.util.regex` on the server, the browser's `RegExp` in the web-client — and return
 * its "contains a match" test. Kotlin's common `Regex` can't carry the `s` (dot-all) flag, and on
 * JS it compiles in unicode mode, which rejects patterns a plain `RegExp` accepts.
 *
 * [flags] is the validated subset of `i` / `m` / `s` (see [Matchers]). Throws
 * [IllegalArgumentException] carrying the engine's message when [pattern] doesn't compile.
 */
internal expect fun compileQueryRegex(pattern: String, flags: String): (String) -> Boolean

/**
 * Plain substring test for the per-card matchers. On JS it is a direct `String.indexOf` from a call
 * site of its own: Kotlin's `contains` goes through a stdlib wrapper shared with every other string
 * search, whose native call V8 can't specialise, and that alone cost several times the TypeScript.
 */
internal expect fun String.containsText(needle: String): Boolean
