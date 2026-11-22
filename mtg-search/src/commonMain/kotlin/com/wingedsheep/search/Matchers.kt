package com.wingedsheep.search

/**
 * Matcher registry — the swappable part of the search language.
 *
 * Every supported key is one entry in [Matchers.REGISTRY]. A matcher receives
 * the parsed [AtomNode] and returns either a predicate or a typed error, and
 * can say in plain words what an atom asks for ([Matcher.explain]) — the line
 * the search bar shows under the query.
 *
 * This is the only implementation: the server runs it on the JVM and the
 * web-client runs it compiled to JavaScript. A new key is one entry here.
 */

typealias CardPredicate = (SearchCard) -> Boolean

sealed interface MatcherResult {
    data class Ok(val predicate: CardPredicate) : MatcherResult
    data class Err(val message: String, val suggestion: String? = null) : MatcherResult
}

interface Matcher {
    val aliases: List<String>
    val ops: List<Op>
    val description: String
    fun build(atom: AtomNode): MatcherResult
    /** What [atom] asks for, as a clause that reads after "cards whose / that …". */
    fun explain(atom: AtomNode): String
}

object Matchers {

    private val ALWAYS_TRUE: CardPredicate = { true }
    private val ALWAYS_FALSE: CardPredicate = { false }

    private val NUMERIC_OPS = listOf(Op.EQ, Op.EXACT, Op.NEQ, Op.LE, Op.GE, Op.LT, Op.GT)
    private val COLOR_OPS = listOf(Op.EQ, Op.EXACT, Op.LE, Op.GE, Op.LT, Op.GT)
    private val STRING_OPS = listOf(Op.EQ)

    private fun ok(p: CardPredicate): MatcherResult = MatcherResult.Ok(p)
    private fun err(message: String, suggestion: String? = null): MatcherResult =
        MatcherResult.Err(message, suggestion)

    // ---- numeric helpers --------------------------------------------------

    private class NumericField(val label: String, val get: (SearchCard) -> Int?)

    private val FIELDS: Map<String, NumericField> = mapOf(
        "cmc" to NumericField("mana value") { it.cmc },
        "mv" to NumericField("mana value") { it.cmc },
        "manavalue" to NumericField("mana value") { it.cmc },
        "pow" to NumericField("power") { parseLeadingInt(it.power) },
        "power" to NumericField("power") { parseLeadingInt(it.power) },
        "tou" to NumericField("toughness") { parseLeadingInt(it.toughness) },
        "toughness" to NumericField("toughness") { parseLeadingInt(it.toughness) },
        "loy" to NumericField("loyalty") { parseLeadingInt(it.loyalty) },
        "loyalty" to NumericField("loyalty") { parseLeadingInt(it.loyalty) },
    )

    /** The integer a printed value starts with (`"2"` → 2, `"1+*"` → 1, `"-1"` → -1), or null (`"*"`). */
    internal fun parseLeadingInt(s: String?): Int? {
        if (s.isNullOrEmpty()) return null
        var i = if (s[0] == '-') 1 else 0
        var n = 0
        val start = i
        while (i < s.length && s[i] in '0'..'9') {
            n = n * 10 + (s[i] - '0')
            i++
        }
        if (i == start) return null
        return if (s[0] == '-') -n else n
    }

    private fun numericCompare(op: Op, a: Int, b: Int): Boolean = when (op) {
        Op.EQ, Op.EXACT -> a == b
        Op.NEQ -> a != b
        Op.LT -> a < b
        Op.LE -> a <= b
        Op.GT -> a > b
        Op.GE -> a >= b
    }

    private fun symbol(op: Op): String = when (op) {
        Op.EQ, Op.EXACT -> "="
        Op.NEQ -> "≠"
        Op.LT -> "<"
        Op.LE -> "≤"
        Op.GT -> ">"
        Op.GE -> "≥"
    }

    /** A numeric key: compares [field] against an integer or another field (`pow>tou`). */
    private fun numeric(aliases: List<String>, description: String): Matcher {
        val field = FIELDS.getValue(aliases.first())
        return entry(
            aliases, NUMERIC_OPS, description,
            build = { atom ->
                val v = atom.value.lowercase().trim()
                val ref = FIELDS[v]
                val n = v.toIntOrNull()
                when {
                    ref != null -> ok { c ->
                        val left = field.get(c); val right = ref.get(c)
                        left != null && right != null && numericCompare(atom.op, left, right)
                    }
                    n == null -> err("Expected a number or field reference (cmc/pow/tou/loy), got \"${atom.value}\".")
                    else -> ok { c -> field.get(c)?.let { numericCompare(atom.op, it, n) } ?: false }
                }
            },
            explain = { atom ->
                val ref = FIELDS[atom.value.lowercase().trim()]
                "${field.label} ${symbol(atom.op)} ${ref?.label ?: atom.value.trim()}"
            },
        )
    }

    // ---- name / oracle / type --------------------------------------------

    private const val REGEX_FLAGS = "ims"

    /**
     * The string test a name or oracle atom applies — regex, `!exact`, or substring — or the
     * error that keeps it from compiling. Shared by the name matcher and [SearchQuery]'s
     * printed-name art selection, so both read an atom identically.
     */
    internal fun textTest(atom: AtomNode): Pair<((String) -> Boolean)?, String?> {
        if (atom.regex) {
            val bad = atom.regexFlags.filter { it !in REGEX_FLAGS }
            if (bad.isNotEmpty()) return null to "Unknown regex flag \"${bad.first()}\" — use i, m or s."
            // Scryfall searches text case-insensitively, so `/…/` is too unless flags are given.
            val flags = atom.regexFlags.ifEmpty { "i" }
            return try {
                compileQueryRegex(atom.value, flags) to null
            } catch (e: IllegalArgumentException) {
                null to "Invalid regex: ${e.message}."
            }
        }
        val needle = atom.value.lowercase()
        if (atom.exact) return ({ s: String -> s.lowercase() == needle }) to null
        return ({ s: String -> s.lowercase().containsText(needle) }) to null
    }

    /**
     * [textTest] for text that is already lowercase ([SearchView]) — the per-card path,
     * which then skips lowercasing every name on every keystroke. Regexes keep their own flags, so
     * they still test the original text.
     */
    private fun lowercaseTest(atom: AtomNode): ((String) -> Boolean)? {
        if (atom.regex) return null
        val needle = atom.value.lowercase()
        return if (atom.exact) { s -> s == needle } else { s -> s.containsText(needle) }
    }

    /** The oracle name or any printed name passes [test]. */
    private fun anyName(c: SearchCard, test: (String) -> Boolean): Boolean {
        if (test(c.name)) return true
        val printed = c.printedNames
        for (i in printed.indices) if (test(printed[i])) return true
        return false
    }

    private fun anyOf(values: Array<String>, test: (String) -> Boolean): Boolean {
        for (v in values) if (test(v)) return true
        return false
    }

    /** Every one of [wanted] is in [have]. */
    private fun containsAll(have: Array<String>, wanted: Array<String>): Boolean {
        for (w in wanted) if (w !in have) return false
        return true
    }

    /** Every one of [have] is in [allowed]. */
    private fun allIn(have: Array<String>, allowed: Array<String>): Boolean {
        for (h in have) if (h !in allowed) return false
        return true
    }

    private val nameMatcher = entry(
        listOf("name", "n"), STRING_OPS, "Card name",
        build = { atom ->
            val (test, error) = textTest(atom)
            when {
                test == null -> err(error!!)
                atom.value.isEmpty() -> ok(ALWAYS_TRUE)
                else -> {
                    val lower = lowercaseTest(atom)
                    if (lower != null) ok { c -> anyOf(c.view.names, lower) } else ok { c -> anyName(c, test) }
                }
            }
        },
        explain = { atom ->
            when {
                atom.regex -> "name matches /${atom.value}/"
                atom.exact -> "name is \"${atom.value}\""
                else -> "name contains \"${atom.value}\""
            }
        },
    )

    private val oracleMatcher = entry(
        listOf("oracle", "o"), STRING_OPS, "Rules text",
        build = { atom ->
            // `!` only means "exact" on a name; on oracle text it is ignored.
            val (test, error) = textTest(atom.copy(exact = false))
            when {
                test == null -> err(error!!)
                atom.value.isEmpty() -> ok(ALWAYS_TRUE)
                else -> {
                    val lower = lowercaseTest(atom.copy(exact = false))
                    if (lower != null) ok { c -> lower(c.view.oracleText) } else ok { c -> test(c.oracleText ?: "") }
                }
            }
        },
        explain = { atom ->
            if (atom.regex) "rules text matches /${atom.value}/" else "rules text contains \"${atom.value}\""
        },
    )

    /** Whether any of [values] contains [needle]. Runs per card: no allocation. */
    private fun anyContains(values: Array<String>, needle: String): Boolean {
        for (v in values) if (v.containsText(needle)) return true
        return false
    }

    private fun typeWords(atom: AtomNode) = atom.value.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }

    private val typeMatcher = entry(
        listOf("type", "t"), STRING_OPS, "Type line — every word must appear",
        build = { atom ->
            val words = typeWords(atom)
            if (words.isEmpty()) ok(ALWAYS_TRUE)
            else {
                val all = words.toTypedArray()
                ok { c ->
                    var ok = true
                    val typeLine = c.view.typeLine
                    for (w in all) {
                        if (!anyContains(typeLine, w)) {
                            ok = false
                            break
                        }
                    }
                    ok
                }
            }
        },
        explain = { atom -> "type line has ${typeWords(atom).joinToString(" and ") { "\"$it\"" }}" },
    )

    // ---- color / cost -----------------------------------------------------

    private val COLOR_ORDER = listOf(W, U, B, R, G)

    private fun colorList(set: Set<String>): String {
        val words = COLOR_ORDER.filter { it in set }.map { it.lowercase() }
        return when (words.size) {
            0 -> "no colours"
            1 -> words[0]
            else -> words.dropLast(1).joinToString(", ") + " and " + words.last()
        }
    }

    /**
     * @param label what the colours are read from, as the explanation says it.
     * @param atMostByCastability when true, `<=` means "playable in a deck limited to these
     *   colours" ([ManaCostMatch.playableWithin]) rather than a plain subset, so a hybrid
     *   `{R/W}` card survives `c<=w`. Only colour identity opts in; `cost:` stays literal.
     */
    private fun color(
        aliases: List<String>,
        description: String,
        label: String,
        atMostByCastability: Boolean,
        get: (SearchView) -> Array<String>,
    ): Matcher = entry(
        aliases, COLOR_OPS, description,
        build = { atom ->
            when (val parsed = ColorSugar.parseColorValue(atom.value)) {
                is ColorSugar.ColorParse.Error -> err(parsed.message)
                is ColorSugar.ColorParse.Count -> ok { c -> numericCompare(atom.op, get(c.view).size, parsed.value) }
                is ColorSugar.ColorParse.Colorless -> when (atom.op) {
                    Op.EQ, Op.EXACT -> ok { c -> get(c.view).isEmpty() }
                    else -> ok(ALWAYS_FALSE)
                }
                is ColorSugar.ColorParse.Multi -> when (atom.op) {
                    Op.EQ, Op.EXACT -> ok { c -> get(c.view).size >= 2 }
                    else -> ok(ALWAYS_FALSE)
                }
                is ColorSugar.ColorParse.Mono -> when (atom.op) {
                    Op.EQ, Op.EXACT -> ok { c -> get(c.view).size == 1 }
                    else -> ok(ALWAYS_FALSE)
                }
                is ColorSugar.ColorParse.Colors -> {
                    val wanted = parsed.set
                    val wantedArray = wanted.toTypedArray()
                    when (atom.op) {
                        Op.EQ, Op.GE -> ok { c -> containsAll(get(c.view), wantedArray) }
                        Op.EXACT -> ok { c -> val cs = get(c.view); cs.size == wanted.size && containsAll(cs, wantedArray) }
                        Op.LE ->
                            if (atMostByCastability) {
                                val pips = ManaCostMatch.pipsOf(wanted)
                                ok { c -> val v = c.view; ManaCostMatch.playableWithin(v.costColors, v.identityPips, pips) }
                            } else ok { c -> allIn(get(c.view), wantedArray) }
                        Op.GT -> ok { c -> val cs = get(c.view); cs.size > wanted.size && containsAll(cs, wantedArray) }
                        Op.LT -> ok { c -> val cs = get(c.view); cs.size < wanted.size && allIn(cs, wantedArray) }
                        Op.NEQ -> err("Unsupported operator \"${atom.op.text}\" on color.")
                    }
                }
            }
        },
        explain = { atom ->
            when (val parsed = ColorSugar.parseColorValue(atom.value)) {
                is ColorSugar.ColorParse.Error -> "$label is ${atom.value}"
                is ColorSugar.ColorParse.Count -> "$label has ${symbol(atom.op)} ${parsed.value} colours"
                is ColorSugar.ColorParse.Colorless -> "$label is colourless"
                is ColorSugar.ColorParse.Multi -> "$label is multicoloured"
                is ColorSugar.ColorParse.Mono -> "$label is a single colour"
                is ColorSugar.ColorParse.Colors -> {
                    val cs = colorList(parsed.set)
                    when (atom.op) {
                        Op.EQ, Op.GE -> "$label includes $cs"
                        Op.EXACT -> "$label is exactly $cs"
                        Op.LE -> if (atMostByCastability) "playable using only $cs" else "$label is within $cs"
                        Op.GT -> "$label includes $cs and more"
                        Op.LT -> "$label is strictly within $cs"
                        Op.NEQ -> "$label is not $cs"
                    }
                }
            }
        },
    )

    // ---- discrete keys ----------------------------------------------------

    private val RARITY_ALIAS = mapOf(
        "c" to "COMMON", "common" to "COMMON",
        "u" to "UNCOMMON", "uncommon" to "UNCOMMON",
        "r" to "RARE", "rare" to "RARE",
        "m" to "MYTHIC", "mythic" to "MYTHIC",
        "s" to "SPECIAL", "special" to "SPECIAL",
        "bonus" to "BONUS",
    )

    /**
     * Ordinal rank for `r>=...` / `r<=...` comparators. Scryfall orders
     * common < uncommon < rare < mythic; we mirror that and place the
     * non-standard rarities at the extremes so they don't accidentally
     * sort into the middle.
     */
    private fun rarityRank(rarity: String): Int? = when (rarity) {
        "COMMON" -> 1
        "UNCOMMON" -> 2
        "RARE" -> 3
        "MYTHIC" -> 4
        "SPECIAL" -> 5
        "BONUS" -> 6
        else -> null
    }

    private val rarity: (AtomNode) -> MatcherResult = { atom ->
        val target = RARITY_ALIAS[atom.value.lowercase()] ?: atom.value.uppercase()
        when (atom.op) {
            Op.EQ, Op.EXACT -> ok { c -> c.rarity == target }
            Op.NEQ -> ok { c -> c.rarity != target }
            Op.LT, Op.LE, Op.GT, Op.GE -> {
                val wantedRank = rarityRank(target)
                if (wantedRank == null) err("Cannot order \"${atom.value}\" — known rarities are common/uncommon/rare/mythic.")
                else ok { c ->
                    val rank = rarityRank(c.rarity) ?: return@ok false
                    numericCompare(atom.op, rank, wantedRank)
                }
            }
        }
    }

    private val set: (AtomNode) -> MatcherResult = { atom ->
        val target = atom.value.lowercase().trim()
        if (target.isEmpty()) ok(ALWAYS_TRUE)
        else ok { c ->
            // Match the canonical printing or any reprint set the card has a printing in.
            // Reprint coverage is what makes `s:EOE Banishing Light` work for cards whose
            // canonical [setCode] is the original printing (BLB) but which also have an
            // EOE reprint row in PrintingRegistry.
            c.setCode?.lowercase() == target ||
                c.printingSetCodes.any { it.lowercase() == target }
        }
    }

    private val format: (AtomNode) -> MatcherResult = { atom ->
        val target = atom.value.uppercase().trim()
        if (target.isEmpty()) ok(ALWAYS_TRUE) else ok { c -> target in c.legalFormats }
    }

    private val keyword: (AtomNode) -> MatcherResult = { atom ->
        val target = atom.value.lowercase().replace(Regex("\\s+"), "_")
        if (target.isEmpty()) ok(ALWAYS_TRUE)
        else ok { c -> target in c.view.keywords }
    }

    private val layout: (AtomNode) -> MatcherResult = { atom ->
        when (atom.value.lowercase()) {
            "transform", "mdfc", "modal_dfc", "dfc", "doublefaced", "double_faced" ->
                ok { c -> c.isDoubleFaced }
            "normal" -> ok { c -> !c.isDoubleFaced }
            else -> err("Unknown layout \"${atom.value}\". Try transform / mdfc / normal.")
        }
    }

    /**
     * Scryfall `is:` flags we recognise but can't evaluate yet because the
     * underlying data isn't on `SearchCard`. We emit a clear "not yet
     * supported" error rather than silently falling through to keyword
     * lookup — silent fallthrough is exactly the bug this query rewrite
     * is meant to fix.
     */
    private val IS_KNOWN_UNSUPPORTED = mapOf(
        "reprint" to "print metadata not stamped on the catalog",
        "firstprint" to "print metadata not stamped on the catalog",
        "unique" to "print metadata not stamped on the catalog",
        "hybrid" to "mana-symbol metadata not parsed",
        "phyrexian" to "mana-symbol metadata not parsed",
        "split" to "split-card layout not modelled",
        "flip" to "flip-card layout not modelled",
        "meld" to "meld-card layout not modelled",
        "leveler" to "leveler-card layout not modelled",
        "modal" to "modal-spell metadata not surfaced",
        "party" to "party-tribe metadata not surfaced",
        "outlaw" to "outlaw-supertypes metadata not surfaced",
        "manland" to "land-cycle metadata not surfaced",
        "bikeland" to "land-cycle metadata not surfaced",
        "cycleland" to "land-cycle metadata not surfaced",
        "checkland" to "land-cycle metadata not surfaced",
        "shockland" to "land-cycle metadata not surfaced",
        "fetchland" to "land-cycle metadata not surfaced",
        "painland" to "land-cycle metadata not surfaced",
        "scryland" to "land-cycle metadata not surfaced",
        "fastland" to "land-cycle metadata not surfaced",
        "slowland" to "land-cycle metadata not surfaced",
        "filterland" to "land-cycle metadata not surfaced",
        "gainland" to "land-cycle metadata not surfaced",
        "triland" to "land-cycle metadata not surfaced",
        "triome" to "land-cycle metadata not surfaced",
        "creatureland" to "land-cycle metadata not surfaced",
        "dual" to "land-cycle metadata not surfaced",
        "funny" to "set-style metadata not surfaced",
        "digital" to "print game-mode not surfaced",
        "alchemy" to "print game-mode not surfaced",
        "rebalanced" to "print game-mode not surfaced",
        "promo" to "print metadata not surfaced",
        "spotlight" to "print metadata not surfaced",
        "scryfallpreview" to "print metadata not surfaced",
        "foil" to "physical printing not surfaced",
        "nonfoil" to "physical printing not surfaced",
        "etched" to "physical printing not surfaced",
        "glossy" to "physical printing not surfaced",
        "hires" to "image metadata not surfaced",
        "universesbeyond" to "set-style metadata not surfaced",
        "default" to "print-prefer metadata not surfaced",
        "atypical" to "print-prefer metadata not surfaced",
        "old" to "border metadata not surfaced",
        "new" to "border metadata not surfaced",
        "datestamped" to "print metadata not surfaced",
        "booster" to "set metadata not surfaced",
        "league" to "set metadata not surfaced",
        "buyabox" to "set metadata not surfaced",
        "giftbox" to "set metadata not surfaced",
        "intro_pack" to "set metadata not surfaced",
        "gameday" to "set metadata not surfaced",
        "prerelease" to "set metadata not surfaced",
        "release" to "set metadata not surfaced",
        "fnm" to "set metadata not surfaced",
        "judge_gift" to "set metadata not surfaced",
        "arena_league" to "set metadata not surfaced",
        "player_rewards" to "set metadata not surfaced",
        "media_insert" to "set metadata not surfaced",
        "instore" to "set metadata not surfaced",
        "convention" to "set metadata not surfaced",
        "set_promo" to "set metadata not surfaced",
        "planeswalker_deck" to "set metadata not surfaced",
        "commander" to "ambiguous (use t:legendary t:creature instead)",
        "brawler" to "format-eligibility metadata not surfaced",
        "companion" to "format-eligibility metadata not surfaced",
        "duelcommander" to "format-eligibility metadata not surfaced",
        "oathbreaker" to "format-eligibility metadata not surfaced",
        "partner" to "format-eligibility metadata not surfaced",
        "gamechanger" to "format-eligibility metadata not surfaced",
        "reserved" to "reserved-list metadata not surfaced",
        "tdfc" to "DFC layout subtype not surfaced (use is:dfc)",
        "meldpart" to "meld-card layout not modelled",
        "meldresult" to "meld-card layout not modelled",
        "newinpauper" to "format-eligibility delta not surfaced",
    )

    private val isMatcher: (AtomNode) -> MatcherResult = { atom ->
        val v = atom.value.lowercase()
        when (v) {
            "land" -> ok { c -> "LAND" in c.cardTypes }
            "creature" -> ok { c -> "CREATURE" in c.cardTypes }
            "instant" -> ok { c -> "INSTANT" in c.cardTypes }
            "sorcery" -> ok { c -> "SORCERY" in c.cardTypes }
            "enchantment" -> ok { c -> "ENCHANTMENT" in c.cardTypes }
            "artifact" -> ok { c -> "ARTIFACT" in c.cardTypes }
            "planeswalker" -> ok { c -> "PLANESWALKER" in c.cardTypes }
            "kindred" -> ok { c -> "KINDRED" in c.cardTypes }
            "permanent" -> ok { c ->
                "CREATURE" in c.cardTypes || "LAND" in c.cardTypes ||
                "ARTIFACT" in c.cardTypes || "ENCHANTMENT" in c.cardTypes ||
                "PLANESWALKER" in c.cardTypes
            }
            "spell" -> ok { c -> "LAND" !in c.cardTypes }
            "legendary" -> ok { c -> "LEGENDARY" in c.supertypes }
            "basic" -> ok { c -> c.basicLand }
            "colorless" -> ok { c -> c.view.colorIdentity.isEmpty() }
            "multicolor", "multicolour" -> ok { c -> c.view.colorIdentity.size >= 2 }
            "monocolor", "monocolour", "mono" -> ok { c -> c.view.colorIdentity.size == 1 }
            "dfc", "mdfc", "transform", "doublefaced", "double_faced" -> ok { c -> c.isDoubleFaced }
            "vanilla" -> ok { c -> "CREATURE" in c.cardTypes && (c.oracleText.isNullOrBlank()) }
            "frenchvanilla", "french_vanilla" -> ok { c -> "CREATURE" in c.cardTypes && c.keywords.isNotEmpty() }
            "bear" -> ok { c ->
                "CREATURE" in c.cardTypes && c.cmc == 2 &&
                parseLeadingInt(c.power) == 2 && parseLeadingInt(c.toughness) == 2
            }
            "historic" -> ok { c -> "LEGENDARY" in c.supertypes || "ARTIFACT" in c.cardTypes || "SAGA" in c.subtypes }
            in IS_KNOWN_UNSUPPORTED -> err("`is:$v` is not yet supported (${IS_KNOWN_UNSUPPORTED[v]}).")
            else -> keyword(atom.copy(value = v))
        }
    }

    private val mana: (AtomNode) -> MatcherResult = { atom ->
        val test = ManaCostMatch.predicate(atom.op, atom.value)
        ok { c -> test(c.view.manaSymbols) }
    }

    // ---- registry ---------------------------------------------------------

    private val IS_PHRASES = mapOf(
        "land" to "is a land", "creature" to "is a creature", "instant" to "is an instant",
        "sorcery" to "is a sorcery", "enchantment" to "is an enchantment", "artifact" to "is an artifact",
        "planeswalker" to "is a planeswalker", "kindred" to "is kindred", "permanent" to "is a permanent",
        "spell" to "is a spell (not a land)", "legendary" to "is legendary", "basic" to "is a basic land",
        "colorless" to "colour identity is colourless",
        "multicolor" to "is multicoloured", "multicolour" to "is multicoloured",
        "monocolor" to "is a single colour", "monocolour" to "is a single colour", "mono" to "is a single colour",
        "dfc" to "is double-faced", "mdfc" to "is double-faced", "transform" to "is double-faced",
        "doublefaced" to "is double-faced", "double_faced" to "is double-faced",
        "vanilla" to "is a creature with no rules text",
        "frenchvanilla" to "is a creature with keyword abilities",
        "french_vanilla" to "is a creature with keyword abilities",
        "bear" to "is a 2-mana 2/2 creature", "historic" to "is historic",
    )

    private val RARITY_WORD = mapOf(
        "COMMON" to "common", "UNCOMMON" to "uncommon", "RARE" to "rare",
        "MYTHIC" to "mythic", "SPECIAL" to "special", "BONUS" to "bonus",
    )

    private val ENTRIES: List<Matcher> = listOf(
        nameMatcher,
        oracleMatcher,
        typeMatcher,
        color(listOf("c", "color", "colour", "id", "identity"), "Colour identity", "colour identity", atMostByCastability = true) { it.colorIdentity },
        color(listOf("cost"), "Colours in the mana cost", "mana cost colour", atMostByCastability = false) { it.colors },
        entry(listOf("mana", "m"), NUMERIC_OPS, "Mana cost, e.g. {2}{U}", mana) { atom ->
            val cost = ManaCostMatch.parseQueryManaSymbols(atom.value).joinToString("") { "{$it}" }
            when (atom.op) {
                Op.EQ, Op.EXACT -> "mana cost is $cost"
                Op.NEQ -> "mana cost is not $cost"
                Op.GE -> "mana cost contains $cost"
                Op.GT -> "mana cost contains $cost and more"
                Op.LE -> "mana cost is within $cost"
                Op.LT -> "mana cost is strictly within $cost"
            }
        },
        numeric(listOf("cmc", "mv", "manavalue"), "Mana value"),
        numeric(listOf("pow", "power"), "Power"),
        numeric(listOf("tou", "toughness"), "Toughness"),
        numeric(listOf("loy", "loyalty"), "Starting loyalty"),
        entry(listOf("r", "rarity"), NUMERIC_OPS, "Rarity: common, uncommon, rare, mythic", rarity) { atom ->
            val target = RARITY_ALIAS[atom.value.lowercase()] ?: atom.value.uppercase()
            "rarity ${symbol(atom.op)} ${RARITY_WORD[target] ?: atom.value}"
        },
        entry(listOf("s", "set", "e", "edition"), STRING_OPS, "Set code, e.g. blb", set) { atom -> "printed in ${atom.value.trim().uppercase()}" },
        entry(listOf("f", "format", "legal"), STRING_OPS, "Legal in a format, e.g. standard", format) { atom ->
            "legal in ${atom.value.trim().lowercase().replaceFirstChar { it.uppercase() }}"
        },
        entry(listOf("kw", "keyword"), STRING_OPS, "Keyword ability, e.g. flying", keyword) { atom -> "has ${atom.value.lowercase()}" },
        entry(listOf("layout"), STRING_OPS, "Layout: normal or double-faced", layout) { atom ->
            if (atom.value.lowercase() == "normal") "is single-faced" else "is double-faced"
        },
        entry(listOf("is"), STRING_OPS, "Shortcuts: land, creature, permanent, legendary, vanilla, dfc, …", isMatcher) { atom ->
            val v = atom.value.lowercase()
            IS_PHRASES[v] ?: "has $v"
        },
    )

    val REGISTRY: Map<String, Matcher> = buildMap {
        for (e in ENTRIES) for (a in e.aliases) put(a, e)
    }

    val ALL_KEYS: List<String> = ENTRIES.flatMap { it.aliases }.distinct().sorted()

    fun suggestKey(unknown: String): String? {
        var best: String? = null
        var bestDist = Int.MAX_VALUE
        for (k in ALL_KEYS) {
            val d = levenshtein(unknown, k)
            if (d < bestDist) { bestDist = d; best = k }
        }
        return if (bestDist <= 2) best else null
    }

    private fun entry(
        aliases: List<String>,
        ops: List<Op>,
        description: String,
        build: (AtomNode) -> MatcherResult,
        explain: (AtomNode) -> String,
    ): Matcher =
        object : Matcher {
            override val aliases = aliases
            override val ops = ops
            override val description = description
            override fun build(atom: AtomNode) = build.invoke(atom)
            override fun explain(atom: AtomNode) = explain.invoke(atom)
        }

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(curr[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
            }
            prev = curr.copyOf()
        }
        return prev[b.length]
    }
}
