@file:OptIn(ExperimentalJsExport::class)

package com.wingedsheep.search

/*
 * The web-client's entry points. `just search-js` compiles this module to an ES module in
 * `web-client/src/generated/mtg-search/`; `web-client/src/components/deckbuilder/query/index.ts`
 * wraps it in the deckbuilder's types. Keep this surface small and flat — everything here is
 * public JavaScript API with generated TypeScript declarations.
 */

/** Compile [query] once; then test cards and read what the query says about itself. */
@JsExport
fun compileQuery(query: String): CompiledQuery = CompiledQuery(SearchQuery.compile(query))

/** Whether [query] uses `or`, the `not` keyword or parentheses — see [SearchQuery.isAdvanced]. */
@JsExport
fun isAdvancedQuery(query: String): Boolean = Parser.isAdvancedQuery(query)

/** Every filter key, for the syntax reference: its aliases, operators and one-line description. */
@JsExport
fun searchKeys(): Array<SearchKey> =
    Matchers.REGISTRY.values.distinct().map { m ->
        SearchKey(m.aliases.toTypedArray(), m.ops.map { it.text }.toTypedArray(), m.description)
    }.toTypedArray()

@JsExport
class SearchKey(val aliases: Array<String>, val ops: Array<String>, val description: String)

@JsExport
class CompiledQuery internal constructor(private val query: SearchQuery) {
    val errors: Array<QueryError> = query.errors.map { e ->
        QueryError(e.message, e.span.start, e.span.end, e.suggestion, e.fix?.let { QueryFix(it.span.start, it.span.end, it.text) })
    }.toTypedArray()

    val isEmpty: Boolean get() = query.isEmpty
    val isAdvanced: Boolean get() = query.isAdvanced
    val setFilter: String? get() = query.setFilter
    val explanation: String? get() = query.explanation
    val constrainsName: Boolean get() = query.constrainsName
    val isConjunction: Boolean get() = query.isConjunction

    /** [SearchQuery.topLevelTerms] flattened to `[start0, end0, start1, end1, …]`. */
    val topLevelTermSpans: IntArray get() = query.topLevelTerms.flatMap { listOf(it.start, it.end) }.toIntArray()

    /** [card] is a deckbuilder `CardSummary` — the server's `CardSummaryDTO` JSON. */
    fun matches(card: dynamic): Boolean = query.matches(adapt(card))

    fun matchedPrintedName(card: dynamic): String? = query.matchedPrintedName(adapt(card))
}

/** A diagnostic over the query text: [start] inclusive, [end] exclusive. */
@JsExport
class QueryError(val message: String, val start: Int, val end: Int, val suggestion: String?, val fix: QueryFix?)

/** Replace `query[start, end)` with [text] to apply the suggestion. */
@JsExport
class QueryFix(val start: Int, val end: Int, val text: String)

/**
 * One [JsSearchCard] per `CardSummary` object, kept for as long as the object lives. The catalog
 * is filtered on every keystroke; reading each card's arrays into Kotlin lists once, rather than
 * per query, is most of what keeps that as fast as the hand-written TypeScript it replaced.
 * `CardSummary` objects are immutable React state — a changed card is a new object.
 */
private val adapters: dynamic = js("new WeakMap()")

private fun adapt(card: dynamic): SearchCard {
    val cached = adapters.get(card)
    if (cached != null) return cached.unsafeCast<SearchCard>()
    val adapted = JsSearchCard(card)
    adapters.set(card, adapted)
    return adapted
}

/**
 * A `CardSummary` object read as a [SearchCard], snapshotted on construction. Fields absent from
 * older fixtures (`keywords`, `legalFormats`, …) read as empty. Reads are unchecked casts: the JSON
 * comes from the server's own `CardSummaryDTO`, which implements [SearchCard] on the JVM side.
 */
private class JsSearchCard(o: dynamic) : SearchCard {
    override val name: String = o.name.unsafeCast<String>()
    override val manaCost: String = o.manaCost.unsafeCast<String?>() ?: ""
    override val cmc: Int = o.cmc.unsafeCast<Int?>() ?: 0
    override val colors: List<String> = strings(o.colors)
    override val colorIdentity: List<String> = strings(o.colorIdentity)
    override val cardTypes: List<String> = strings(o.cardTypes)
    override val supertypes: List<String> = strings(o.supertypes)
    override val subtypes: List<String> = strings(o.subtypes)
    override val basicLand: Boolean = o.basicLand == true
    override val rarity: String = o.rarity.unsafeCast<String?>() ?: ""
    override val setCode: String? = o.setCode.unsafeCast<String?>()
    override val collectorNumber: String? = o.collectorNumber.unsafeCast<String?>()
    override val oracleText: String? = o.oracleText.unsafeCast<String?>()
    override val power: String? = o.power.unsafeCast<String?>()
    override val toughness: String? = o.toughness.unsafeCast<String?>()
    override val loyalty: String? = o.loyalty.unsafeCast<String?>()
    override val keywords: List<String> = strings(o.keywords)
    override val legalFormats: List<String> = strings(o.legalFormats)
    override val isDoubleFaced: Boolean = o.isDoubleFaced == true
    override val printingSetCodes: List<String> = strings(o.printingSetCodes)
    override val printedNames: List<String> =
        o.printedNamePrintings.unsafeCast<Array<dynamic>?>()?.map { it.name.unsafeCast<String>() } ?: emptyList()
    override val view: SearchView = SearchView(this)
}

private fun strings(value: dynamic): List<String> =
    value.unsafeCast<Array<String>?>()?.asList() ?: emptyList()
