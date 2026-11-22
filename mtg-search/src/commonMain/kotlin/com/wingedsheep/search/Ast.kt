package com.wingedsheep.search

/**
 * Shared AST + diagnostic types for the search query language.
 *
 * The lexer produces a flat [Token] stream; the parser folds those into a
 * [Node] tree; the evaluator compiles the tree into a `(SearchCard) -> Boolean`
 * predicate. Errors carry [Span] info so callers can underline the offending
 * region in the original query string.
 */

/** Inclusive [start], exclusive [end] offset into the original query string. */
data class Span(val start: Int, val end: Int)

enum class Op(val text: String) {
    EQ(":"),
    EXACT("="),
    NEQ("!="),
    LE("<="),
    GE(">="),
    LT("<"),
    GT(">"),
    ;

    companion object {
        /** Operator alternatives ordered by length, longest first, so `<=` doesn't lose to `<`. */
        val ORDERED: List<Op> = listOf(LE, GE, NEQ, EQ, EXACT, LT, GT)
        fun fromText(text: String): Op? = entries.firstOrNull { it.text == text }
    }
}

sealed interface TokenKind {
    object LParen : TokenKind
    object RParen : TokenKind
    object Or : TokenKind
    object And : TokenKind
    object Not : TokenKind
    /** A `key OP value` term, a bareword (key=null), or an exact-name shortcut (`!Foo`). */
    object Term : TokenKind
}

data class Token(
    val kind: TokenKind,
    val span: Span,
    val negate: Boolean = false,
    val key: String? = null,
    val op: Op = Op.EQ,
    val value: String = "",
    val regex: Boolean = false,
    val regexFlags: String = "",
    val exact: Boolean = false,
    /** Where the key sits in the query (`t` in `-t:goblin`), so a "did you mean" fix can replace just it. */
    val keySpan: Span? = null,
)

sealed interface Node {
    val span: Span
}
data class AndNode(val children: List<Node>, override val span: Span) : Node
data class OrNode(val children: List<Node>, override val span: Span) : Node
data class NotNode(val child: Node, override val span: Span) : Node
data class AtomNode(
    val key: String?,
    val op: Op,
    val value: String,
    val regex: Boolean,
    val regexFlags: String,
    val exact: Boolean,
    override val span: Span,
    val keySpan: Span? = null,
) : Node

data class ParseError(
    val message: String,
    val span: Span,
    val suggestion: String? = null,
    /** A one-click repair for [suggestion]: put [Fix.text] where [Fix.span] is. */
    val fix: Fix? = null,
)

/** Replace [span] of the original query with [text]. */
data class Fix(val span: Span, val text: String)
