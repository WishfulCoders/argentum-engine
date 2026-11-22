package com.wingedsheep.search

/**
 * Lexer for the Scryfall-flavoured search language.
 *
 * The only lexer: the web-client runs this same code, compiled to JavaScript.
 *
 * Decisions:
 *   - `(` / `)` always split a token, even without surrounding whitespace.
 *     Inside double quotes parens are literal.
 *   - `and` / `or` / `not` are keywords only when they appear as a standalone
 *     bareword (no leading `-` / `!`, no key/op).
 *   - Regex literals (`/.../[a-z]*`) are only recognised as the *value* of a
 *     `key OP` term. A bare `/foo/` outside a `key:` context is treated as a
 *     regular bareword (slashes appear in real card names).
 */
object Lexer {

    private val KEY_RE = Regex("^[a-zA-Z]+$")

    fun lex(query: String): List<Token> {
        val tokens = ArrayList<Token>()
        var i = 0
        val n = query.length

        while (i < n) {
            val ch = query[i]
            if (ch.isWhitespace()) { i++; continue }
            if (ch == '(') { tokens.add(Token(TokenKind.LParen, Span(i, i + 1))); i++; continue }
            if (ch == ')') { tokens.add(Token(TokenKind.RParen, Span(i, i + 1))); i++; continue }

            val start = i
            var inQuote = false
            var quoteChar: Char? = null
            while (i < n) {
                val c = query[i]
                if (inQuote) {
                    if (c == quoteChar) { inQuote = false; quoteChar = null }
                    i++
                    continue
                }
                if (c == '"' || c == '\'') { inQuote = true; quoteChar = c; i++; continue }
                if (c.isWhitespace()) break
                if (c == '(' || c == ')') break
                i++
            }
            val span = Span(start, i)
            if (i == start) continue
            // A slice of the query, not a StringBuilder: on JS, char-by-char appends leave a rope
            // that V8 searches several times slower, and these values are searched once per card.
            tokens.add(buildTerm(query.substring(start, i), span))
        }
        return tokens
    }

    private fun buildTerm(raw: String, span: Span): Token {
        val lc = raw.lowercase()
        if (raw.isNotEmpty() && raw[0] != '-' && raw[0] != '!') {
            when (lc) {
                "and" -> return Token(TokenKind.And, span)
                "or"  -> return Token(TokenKind.Or, span)
                "not" -> return Token(TokenKind.Not, span)
            }
        }

        var term = raw
        var negate = false
        var exact = false
        if (term.startsWith("-") && term.length > 1) { negate = true; term = term.substring(1) }
        if (term.startsWith("!") && term.length > 1) { exact = true; term = term.substring(1) }

        val match = findKeyOp(term)
        if (match != null) {
            val (key, op, valueStart) = match
            val keyStart = span.start + (if (negate) 1 else 0) + (if (exact) 1 else 0)
            val parsed = parseValue(term.substring(valueStart), allowRegex = true)
            return Token(
                kind = TokenKind.Term,
                span = span,
                negate = negate,
                key = key.lowercase(),
                op = op,
                value = parsed.value,
                regex = parsed.regex,
                regexFlags = parsed.regexFlags,
                exact = exact,
                keySpan = Span(keyStart, keyStart + key.length),
            )
        }

        val parsed = parseValue(term, allowRegex = false)
        return Token(
            kind = TokenKind.Term,
            span = span,
            negate = negate,
            key = null,
            op = Op.EQ,
            value = parsed.value,
            regex = parsed.regex,
            regexFlags = parsed.regexFlags,
            exact = exact,
        )
    }

    private data class KeyOpMatch(val key: String, val op: Op, val valueStart: Int)

    private fun findKeyOp(term: String): KeyOpMatch? {
        for (i in term.indices) {
            for (op in Op.ORDERED) {
                if (i + op.text.length >= term.length) continue
                if (!term.regionMatches(i, op.text, 0, op.text.length)) continue
                val key = term.substring(0, i)
                if (KEY_RE.matches(key)) {
                    return KeyOpMatch(key, op, i + op.text.length)
                }
            }
        }
        return null
    }

    private data class ParsedValue(val value: String, val regex: Boolean, val regexFlags: String)

    private val REGEX_RE = Regex("^/(.+)/([a-zA-Z]*)$")

    private fun parseValue(raw: String, allowRegex: Boolean): ParsedValue {
        if (raw.length >= 2) {
            val first = raw[0]
            if ((first == '"' || first == '\'') && raw.last() == first) {
                return ParsedValue(raw.substring(1, raw.length - 1), regex = false, regexFlags = "")
            }
            if (allowRegex && first == '/') {
                val m = REGEX_RE.matchEntire(raw)
                if (m != null) {
                    return ParsedValue(m.groupValues[1], regex = true, regexFlags = m.groupValues[2])
                }
            }
        }
        return ParsedValue(raw, regex = false, regexFlags = "")
    }
}
