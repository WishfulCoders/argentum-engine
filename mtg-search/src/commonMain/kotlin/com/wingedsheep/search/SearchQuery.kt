package com.wingedsheep.search

/**
 * A compiled query: everything a search box needs from one string, computed once per keystroke.
 *
 * Beyond [matches] it answers the questions the deckbuilder asks about the query itself — the
 * plain-words reading shown under the search bar ([explanation]), whether the chip panel can still
 * round-trip it ([isAdvanced]), which set's art to show ([setFilter]), and which printing a
 * printed-name match should display ([matchedPrintedName]).
 */
class SearchQuery private constructor(
    val text: String,
    val ast: Node?,
    val errors: List<ParseError>,
    private val predicate: CardPredicate,
) {

    fun matches(card: SearchCard): Boolean = predicate(card)

    /** `true` for a blank query, which matches every card. */
    val isEmpty: Boolean get() = ast == null

    /**
     * `true` when the query uses anything beyond the flat AND the filter chips round-trip:
     * `or`, the `not` keyword, or parentheses. A `-` negated term is not advanced.
     */
    val isAdvanced: Boolean by lazy { Parser.isAdvancedQuery(text) }

    /**
     * The set code a positive `s:CODE` pins every result to, uppercased, or null. Only an
     * AND-only path counts: under `or` / `not` the filter doesn't hold for every match, so
     * swapping in that set's reprint art would be misleading.
     */
    val setFilter: String? by lazy { ast?.let(::findSetFilter) }

    /**
     * The query in plain words — `name contains "bolt" and power ≥ 4` — or null when it is
     * blank or has errors (the errors say what's wrong instead).
     */
    val explanation: String? by lazy {
        if (ast == null || errors.isNotEmpty()) null else explain(ast, parent = null)
    }

    /**
     * The spans of the query's top-level AND terms — the pieces a filter panel can toggle one by
     * one. `t:goblin (c:r or c:g) -kw:haste` has three: the atom, the bracketed group, the negation.
     * A query whose root isn't a plain AND (an `or` at the top) has none: see [isConjunction].
     */
    val topLevelTerms: List<Span>
        get() = when (val root = ast) {
            null -> emptyList()
            is AndNode -> root.children.map { it.span }
            is OrNode -> emptyList()
            else -> listOf(root.span)
        }

    /**
     * Whether appending a term ANDs it with the whole query. False when `or` sits at the top
     * (`t:elf or t:goblin`), where a new term would bind only to the last branch, so a caller must
     * bracket the query first.
     */
    val isConjunction: Boolean get() = ast !is OrNode

    private val nameTest: ((String) -> Boolean)? by lazy { ast?.let(::nameConstraint) }

    /** Whether any card could have a [matchedPrintedName] — false lets callers skip the scan. */
    val constrainsName: Boolean get() = nameTest != null

    /**
     * The printed name (one of [SearchCard.printedNames]) this query's name terms matched, when
     * the oracle name itself doesn't satisfy them — `ademi` finds Spectacular Spider-Man by its
     * Through the Omenpaths name, and the deckbuilder shows that printing. Null otherwise.
     */
    fun matchedPrintedName(card: SearchCard): String? {
        val test = nameTest ?: return null
        if (card.printedNames.isEmpty() || test(card.name)) return null
        return card.printedNames.firstOrNull(test)
    }

    companion object {
        private val EMPTY = SearchQuery("", null, emptyList()) { true }

        fun compile(query: String): SearchQuery {
            if (query.isBlank()) return EMPTY
            val parsed = Parser.parse(query)
            val compiled = Evaluator.compile(parsed.ast)
            return SearchQuery(query, parsed.ast, parsed.errors + compiled.errors, compiled.predicate)
        }

        private fun findSetFilter(root: Node): String? {
            val stack = ArrayDeque<Node>().apply { add(root) }
            while (stack.isNotEmpty()) {
                when (val node = stack.removeLast()) {
                    is AtomNode -> if (!node.regex && (node.key == "s" || node.key == "set") &&
                        (node.op == Op.EQ || node.op == Op.EXACT)
                    ) {
                        node.value.trim().uppercase().takeIf { it.isNotEmpty() }?.let { return it }
                    }
                    is AndNode -> stack.addAll(node.children)
                    is OrNode, is NotNode -> {}
                }
            }
            return null
        }

        /**
         * The name test a node imposes, or null when it leaves the name unconstrained. `and`
         * needs every constraint, `or` any — so a pasted decklist (`!"A" or !"B"`) picks art per
         * card — but an `or` with a branch that names nothing, and anything under `not`, don't.
         */
        private fun nameConstraint(node: Node): ((String) -> Boolean)? = when (node) {
            is AtomNode -> if (isNameAtom(node)) Matchers.textTest(node).first else null
            is NotNode -> null
            is AndNode -> {
                val tests = node.children.mapNotNull(::nameConstraint)
                if (tests.isEmpty()) null else { s -> tests.all { it(s) } }
            }
            is OrNode -> {
                val tests = node.children.map(::nameConstraint)
                if (tests.any { it == null }) null else { s -> tests.any { it!!(s) } }
            }
        }

        private fun isNameAtom(atom: AtomNode): Boolean =
            atom.key == null || ((atom.key == "name" || atom.key == "n") && (atom.op == Op.EQ || atom.op == Op.EXACT))

        private fun explain(node: Node, parent: Node?): String = when (node) {
            is AtomNode -> Matchers.REGISTRY.getValue(node.key ?: "name").explain(node)
            is NotNode -> "not (${explain(node.child, node)})"
            is AndNode -> node.children.joinToString(" and ") { explain(it, node) }
                .let { if (parent is AndNode || parent is OrNode) "($it)" else it }
            is OrNode -> node.children.joinToString(" or ") { explain(it, node) }
                .let { if (parent is AndNode || parent is OrNode) "($it)" else it }
        }
    }
}
