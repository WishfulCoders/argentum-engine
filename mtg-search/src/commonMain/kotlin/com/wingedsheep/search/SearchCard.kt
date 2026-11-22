package com.wingedsheep.search

/**
 * Card projection consumed by the search engine.
 *
 * The search module never touches engine state — it operates on a flat
 * snapshot of a card. `game-server`'s `CardSummaryDTO` implements it directly;
 * in the browser, `JsSearchCard` reads the same fields off the deckbuilder's
 * `CardSummary` JSON (the very DTO the server sent).
 *
 *   - `colors` are the printed mana-cost colors (`{R}` cost ⇒ `RED`).
 *   - `colorIdentity` is CR 903.4 — used by the `c:` / `id:` filters.
 *   - `cardTypes` / `supertypes` / `subtypes` are uppercase enum names.
 *   - `legalFormats` carries uppercase format names (STANDARD, MODERN, …).
 */
interface SearchCard {
    val name: String
    val manaCost: String
    val cmc: Int
    val colors: List<String>
    val colorIdentity: List<String>
    val cardTypes: List<String>
    val supertypes: List<String>
    val subtypes: List<String>
    val basicLand: Boolean
    val rarity: String
    val setCode: String?
    val collectorNumber: String?
    val oracleText: String?
    /** Printed power as text — `"2"`, `"*"`, `"*+1"`. `pow` compares its leading integer. */
    val power: String?
    val toughness: String?
    val keywords: List<String>
    val legalFormats: List<String>
    val isDoubleFaced: Boolean
    /**
     * Every set this card has a printing in — the canonical [setCode] plus every reprint
     * registered in `PrintingRegistry`. Used by the `s:`/`set:` matcher so a query like
     * `s:EOE Banishing Light` matches the card via its EOE reprint even though its
     * canonical [setCode] is BLB.
     *
     * Defaults to empty so existing implementers aren't forced to populate it; the
     * matcher always falls back to [setCode] for backwards compatibility.
     */
    val printingSetCodes: List<String> get() = emptyList()

    /**
     * Names printed on this card's printings when they differ from the oracle [name] — e.g.
     * Through the Omenpaths' "Ademi of the Silkchutes" for Spectacular Spider-Man. The name
     * matcher (bareword, `name:`, `!exact`) checks these alongside [name], as Scryfall does.
     */
    val printedNames: List<String> get() = emptyList()

    /** Printed starting loyalty of a planeswalker (`"3"`, `"X"`), null otherwise. Drives `loy`. */
    val loyalty: String? get() = null

    /**
     * This card's fields in the shape the matchers read them per card — see [SearchView]. The
     * default builds a fresh view on each read; an implementation that lives across many queries
     * (the web-client's, filtering the catalog on every keystroke) overrides it with one kept.
     */
    val view: SearchView get() = SearchView(this)
}

/**
 * A card's searchable fields as the matchers want them per card: text lowercased once, lists as
 * plain arrays. Each is built on first use and kept.
 *
 * Arrays and backing fields are deliberate. On Kotlin/JS a `List.get` dispatches through whichever
 * list class it was handed, which V8 can't inline once a call site has seen several, and a `by lazy`
 * property builds a reflection object on every read. Either alone made catalog filtering several
 * times slower than the TypeScript this module replaced.
 */
class SearchView(private val card: SearchCard) {
    private var namesCache: Array<String>? = null
    private var oracleCache: String? = null
    private var typeLineCache: Array<String>? = null
    private var keywordsCache: Array<String>? = null
    private var identityCache: Array<String>? = null
    private var colorsCache: Array<String>? = null
    private var manaCache: Array<String>? = null
    private var costColorsCache: ManaCostMatch.CostColors? = null
    private var identityPipsCache: String? = null

    /** The oracle name and every printed name, lowercased. */
    val names: Array<String>
        get() = namesCache ?: (listOf(card.name) + card.printedNames).map { it.lowercase() }.toTypedArray().also { namesCache = it }

    /** Rules text, lowercased; empty when there is none. */
    val oracleText: String
        get() = oracleCache ?: (card.oracleText?.lowercase() ?: "").also { oracleCache = it }

    /** Card types, supertypes and subtypes — the type line's words — lowercased. */
    val typeLine: Array<String>
        get() = typeLineCache
            ?: (card.cardTypes + card.supertypes + card.subtypes).map { it.lowercase() }.toTypedArray().also { typeLineCache = it }

    /** Keyword abilities, lowercased. */
    val keywords: Array<String>
        get() = keywordsCache ?: card.keywords.map { it.lowercase() }.toTypedArray().also { keywordsCache = it }

    val colorIdentity: Array<String>
        get() = identityCache ?: card.colorIdentity.toTypedArray().also { identityCache = it }

    val colors: Array<String>
        get() = colorsCache ?: card.colors.toTypedArray().also { colorsCache = it }

    /** The printed cost's symbols (`2`, `U`, `R/W`), uppercased and sorted — a multiset `mana:` compares. */
    val manaSymbols: Array<String>
        get() = manaCache ?: ManaCostMatch.parseCardManaSymbols(card.manaCost).sorted().toTypedArray().also { manaCache = it }

    /** What the printed cost asks of a deck's colours — drives `c<=`. */
    val costColors: ManaCostMatch.CostColors
        get() = costColorsCache ?: ManaCostMatch.costColors(manaSymbols).also { costColorsCache = it }

    /** The colour identity as single-letter pips (`"WR"`). */
    val identityPips: String
        get() = identityPipsCache ?: ManaCostMatch.pipsOf(card.colorIdentity).also { identityPipsCache = it }
}
