package com.wingedsheep.search

/**
 * Recursive-descent parser. Grammar (low to high precedence):
 *
 *   Or   ::= And ('or' And)*
 *   And  ::= Not (('and')? Not)*    -- AND is implicit by juxtaposition
 *   Not  ::= 'not'? Atom
 *   Atom ::= '(' Or ')' | TERM
 *
 * Errors are recoverable: structural problems push a [ParseError] but the
 * parser continues so the rest of the query still filters meaningfully.
 */
object Parser {

    data class ParseAst(val ast: Node?, val errors: List<ParseError>)

    fun parse(query: String): ParseAst {
        val tokens = Lexer.lex(query)
        if (tokens.isEmpty()) return ParseAst(null, emptyList())
        val state = State(tokens)
        val node = parseOr(state)
        if (state.pos < tokens.size) {
            val tok = tokens[state.pos]
            state.errors += ParseError(
                if (tok.kind == TokenKind.RParen) "Unmatched `)`." else "Unexpected token.",
                tok.span,
            )
        }
        return ParseAst(node, state.errors)
    }

    /** Detect any feature beyond the flat-AND model: `or`, `not` keyword, parens. */
    fun isAdvancedQuery(query: String): Boolean {
        for (tok in Lexer.lex(query)) {
            when (tok.kind) {
                TokenKind.LParen, TokenKind.RParen, TokenKind.Or, TokenKind.Not -> return true
                else -> {}
            }
        }
        return false
    }

    private class State(val tokens: List<Token>) {
        var pos = 0
        val errors = ArrayList<ParseError>()
    }

    private fun parseOr(state: State): Node? {
        var left = parseAnd(state)
        while (state.pos < state.tokens.size) {
            val tok = state.tokens[state.pos]
            if (tok.kind != TokenKind.Or) break
            state.pos++
            val right = parseAnd(state)
            if (right == null) {
                state.errors += ParseError("Expected expression after `or`.", tok.span)
                break
            }
            left = if (left == null) right else combine(NodeKind.OR, left, right)
        }
        return left
    }

    private fun parseAnd(state: State): Node? {
        var left = parseNot(state)
        while (state.pos < state.tokens.size) {
            val tok = state.tokens[state.pos]
            if (tok.kind == TokenKind.Or || tok.kind == TokenKind.RParen) break
            if (tok.kind == TokenKind.And) state.pos++
            val right = parseNot(state)
            if (right == null) {
                if (tok.kind == TokenKind.And) {
                    state.errors += ParseError("Expected expression after `and`.", tok.span)
                }
                break
            }
            left = if (left == null) right else combine(NodeKind.AND, left, right)
        }
        return left
    }

    private fun parseNot(state: State): Node? {
        if (state.pos >= state.tokens.size) return null
        val tok = state.tokens[state.pos]
        if (tok.kind == TokenKind.Not) {
            state.pos++
            val child = parseAtom(state)
            if (child == null) {
                state.errors += ParseError("Expected expression after `not`.", tok.span)
                return null
            }
            return NotNode(child, Span(tok.span.start, child.span.end))
        }
        return parseAtom(state)
    }

    private fun parseAtom(state: State): Node? {
        if (state.pos >= state.tokens.size) return null
        val tok = state.tokens[state.pos]
        if (tok.kind == TokenKind.LParen) {
            state.pos++
            val inner = parseOr(state)
            if (state.pos < state.tokens.size && state.tokens[state.pos].kind == TokenKind.RParen) {
                val rparen = state.tokens[state.pos]
                state.pos++
                if (inner == null) {
                    state.errors += ParseError("Empty group `()`.", Span(tok.span.start, rparen.span.end))
                    return null
                }
                return inner.withSpan(Span(tok.span.start, rparen.span.end))
            }
            state.errors += ParseError("Unmatched `(`.", tok.span)
            return inner
        }
        if (tok.kind == TokenKind.RParen) return null
        if (tok.kind == TokenKind.Or || tok.kind == TokenKind.And || tok.kind == TokenKind.Not) return null
        // term
        state.pos++
        val atom = AtomNode(
            key = tok.key,
            op = tok.op,
            value = tok.value,
            regex = tok.regex,
            regexFlags = tok.regexFlags,
            exact = tok.exact,
            span = tok.span,
            keySpan = tok.keySpan,
        )
        return if (tok.negate) NotNode(atom, tok.span) else atom
    }

    private enum class NodeKind { AND, OR }

    private fun combine(kind: NodeKind, left: Node, right: Node): Node {
        val span = Span(left.span.start, right.span.end)
        val children = ArrayList<Node>()
        when {
            kind == NodeKind.AND && left is AndNode -> children += left.children
            kind == NodeKind.OR && left is OrNode -> children += left.children
            else -> children += left
        }
        when {
            kind == NodeKind.AND && right is AndNode -> children += right.children
            kind == NodeKind.OR && right is OrNode -> children += right.children
            else -> children += right
        }
        return when (kind) {
            NodeKind.AND -> AndNode(children, span)
            NodeKind.OR -> OrNode(children, span)
        }
    }

    private fun Node.withSpan(span: Span): Node = when (this) {
        is AndNode -> copy(span = span)
        is OrNode -> copy(span = span)
        is NotNode -> copy(span = span)
        is AtomNode -> copy(span = span)
    }
}
