package com.wingedsheep.search

/**
 * Public facade for the search language.
 *
 * [compile] produces a [SearchQuery] — the predicate, its [ParseError]s, and what the query says
 * about itself. [parse] is the predicate-and-errors shorthand the REST controller uses.
 *
 * The grammar lives in [Parser]; the supported keys live in [Matchers]. This module is the single
 * source of truth: the web-client runs it compiled to JavaScript (`src/jsMain`), not a port.
 */
object SearchService {

    data class SearchResult(
        val predicate: CardPredicate,
        val errors: List<ParseError>,
    )

    fun compile(query: String): SearchQuery = SearchQuery.compile(query)

    fun parse(query: String): SearchResult {
        val compiled = compile(query)
        return SearchResult(compiled::matches, compiled.errors)
    }

    /** Convenience: filter a list of cards by [query] in one call. */
    fun <T : SearchCard> search(cards: List<T>, query: String): List<T> {
        val compiled = compile(query)
        return cards.filter(compiled::matches)
    }
}
