package com.wingedsheep.search

/**
 * Compile a parsed AST into a [CardPredicate], collecting per-atom errors.
 *
 * Compile-time errors collapse the offending atom to `false` locally — the
 * rest of the query still evaluates. The caller surfaces errors through the
 * search bar so users see a precise diagnostic instead of an empty result list.
 */
object Evaluator {

    private val TRUE: CardPredicate = { true }
    private val FALSE: CardPredicate = { false }

    fun compile(ast: Node?): CompileResult {
        val errors = ArrayList<ParseError>()
        if (ast == null) return CompileResult(TRUE, errors)
        val predicate = walk(ast, errors)
        return CompileResult(predicate, errors)
    }

    data class CompileResult(val predicate: CardPredicate, val errors: List<ParseError>)

    private fun walk(node: Node, errors: MutableList<ParseError>): CardPredicate = when (node) {
        is AndNode -> andOf(node.children.map { walk(it, errors) })
        is OrNode -> orOf(node.children.map { walk(it, errors) })
        is NotNode -> notOf(walk(node.child, errors))
        is AtomNode -> compileAtom(node, errors)
    }

    // Arrays, not lists: these run per card per keystroke, and an Array loop allocates no iterator.
    private fun andOf(ps: List<CardPredicate>): CardPredicate {
        val all = ps.toTypedArray()
        return { c ->
            var ok = true
            for (p in all) if (!p(c)) { ok = false; break }
            ok
        }
    }

    private fun orOf(ps: List<CardPredicate>): CardPredicate {
        val any = ps.toTypedArray()
        return { c ->
            var ok = false
            for (p in any) if (p(c)) { ok = true; break }
            ok
        }
    }
    private fun notOf(p: CardPredicate): CardPredicate = { c -> !p(c) }

    private fun compileAtom(atom: AtomNode, errors: MutableList<ParseError>): CardPredicate {
        if (atom.key == null) {
            // Bareword + `!exact` form route through the name matcher.
            val matcher = Matchers.REGISTRY["name"]!!
            return runMatcher(matcher, atom, errors)
        }
        val matcher = Matchers.REGISTRY[atom.key]
        if (matcher == null) {
            val sugg = Matchers.suggestKey(atom.key)
            errors += ParseError(
                "Unknown filter \"${atom.key}\".",
                atom.span,
                if (sugg != null) "Did you mean \"$sugg:\"?" else null,
                if (sugg != null && atom.keySpan != null) Fix(atom.keySpan, sugg) else null,
            )
            return FALSE
        }
        if (atom.op !in matcher.ops) {
            errors += ParseError(
                "Operator \"${atom.op.text}\" is not supported on \"${atom.key}\". " +
                    "Try ${matcher.ops.joinToString(", ") { "\"${it.text}\"" }}.",
                atom.span,
            )
            return FALSE
        }
        return runMatcher(matcher, atom, errors)
    }

    private fun runMatcher(matcher: Matcher, atom: AtomNode, errors: MutableList<ParseError>): CardPredicate =
        when (val r = matcher.build(atom)) {
            is MatcherResult.Ok -> r.predicate
            is MatcherResult.Err -> {
                errors += ParseError(r.message, atom.span, r.suggestion)
                FALSE
            }
        }
}
