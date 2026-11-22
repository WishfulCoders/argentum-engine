package com.wingedsheep.search

/**
 * Mana-cost-symbol multiset parsing & comparison for `mana:` / `m:` queries.
 *
 * Accepts both `{2}{u}{u}` and `2uu`. Comparators:
 *   `:` / `=`  card mana == requested
 *   `>=`       card mana ⊇ requested
 *   `<=`       card mana ⊆ requested
 *   `>`        ⊇ and not equal
 *   `<`        ⊆ and not equal
 *
 * Hybrid (`{u/r}`) and Phyrexian (`{w/p}`) symbols match literally — we don't
 * expand them.
 */
object ManaCostMatch {

    fun parseQueryManaSymbols(value: String): List<String> {
        val symbols = ArrayList<String>()
        var i = 0
        while (i < value.length) {
            val ch = value[i]
            when {
                ch == '{' -> {
                    val close = value.indexOf('}', i + 1)
                    if (close == -1) break
                    symbols += value.substring(i + 1, close).uppercase()
                    i = close + 1
                }
                ch.isWhitespace() -> i++
                ch.isDigit() -> {
                    var j = i
                    while (j < value.length && value[j].isDigit()) j++
                    symbols += value.substring(i, j)
                    i = j
                }
                ch.isLetter() -> {
                    symbols += ch.uppercase()
                    i++
                }
                else -> i++
            }
        }
        return symbols
    }

    /** The symbols of a printed cost (`{2}{U/R}` → `2`, `U/R`), uppercased. A plain scan — no regex. */
    fun parseCardManaSymbols(manaCost: String): List<String> {
        val symbols = ArrayList<String>()
        var i = manaCost.indexOf('{')
        while (i != -1) {
            val close = manaCost.indexOf('}', i + 1)
            if (close == -1) break
            if (close > i + 1) symbols += manaCost.substring(i + 1, close).uppercase()
            i = manaCost.indexOf('{', close + 1)
        }
        return symbols
    }

    /** The single-letter pip of a colour name (`RED` → `R`), or null for anything else. */
    fun pipOf(color: String): Char? = when (color) {
        W -> 'W'
        U -> 'U'
        B -> 'B'
        R -> 'R'
        G -> 'G'
        else -> null
    }

    /** Single-letter pips (`"WR"`) for colour names (`WHITE`, `RED`). */
    fun pipsOf(colors: Iterable<String>): String = colors.mapNotNull(::pipOf).joinToString("")

    private fun isPip(half: String) = half.length == 1 && half[0] in "WUBRG"

    /**
     * What a printed cost asks of a deck's colours, worked out once per card ([SearchView.costColors]).
     *
     * @property pips every colour pip anywhere in the cost, both halves of a hybrid included.
     * @property requirements one entry per symbol that forces a colour: the pips that can pay it —
     *   `"R"` for `{R}`, `"RW"` for `{R/W}` (CR 107.4e: either half pays). Phyrexian (`{W/P}`) and
     *   twobrid (`{2/W}`) symbols always have a colourless payment, so they force nothing.
     */
    class CostColors(val pips: String, val requirements: Array<String>)

    fun costColors(symbols: Array<String>): CostColors {
        val pips = StringBuilder()
        val requirements = ArrayList<String>()
        for (sym in symbols) {
            val halves = sym.split('/')
            for (h in halves) if (isPip(h) && h[0] !in pips) pips.append(h[0])
            if (halves.size == 1) {
                if (isPip(sym)) requirements += sym
                continue
            }
            if ("P" in halves) continue
            if (halves.any { h -> h.isNotEmpty() && h.all { it.isDigit() } }) continue
            requirements += halves.filter(::isPip).joinToString("")
        }
        return CostColors(pips.toString(), requirements.toTypedArray())
    }

    /**
     * "At most" colour semantics (`c<=wr`): can this card be *played* in a deck limited to the
     * colours of [allowedPips] (`"WR"`, from [pipsOf])? Every symbol its [cost] forces must be
     * payable from those colours, and any identity colour ([identityPips]) the cost doesn't
     * account for — an off-colour activation cost, a dual land's subtypes — must itself be allowed.
     */
    fun playableWithin(cost: CostColors, identityPips: String, allowedPips: String): Boolean {
        for (pip in identityPips) if (pip !in allowedPips && pip !in cost.pips) return false
        for (req in cost.requirements) if (req.none { it in allowedPips }) return false
        return true
    }

    /**
     * The `mana:` predicate over a printed cost's sorted symbols ([SearchView.manaSymbols]).
     * Costs compare as multisets, so with both sides sorted the check is a merge walk.
     */
    fun predicate(op: Op, queryValue: String): (Array<String>) -> Boolean {
        val wanted = parseQueryManaSymbols(queryValue).sorted().toTypedArray()
        return { have ->
            when (op) {
                Op.EQ, Op.EXACT -> have.contentEquals(wanted)
                Op.NEQ -> !have.contentEquals(wanted)
                Op.GE -> isSubBag(wanted, have)
                Op.LE -> isSubBag(have, wanted)
                Op.GT -> have.size > wanted.size && isSubBag(wanted, have)
                Op.LT -> have.size < wanted.size && isSubBag(have, wanted)
            }
        }
    }

    /** Whether sorted [sub] is contained, with multiplicity, in sorted [sup]. */
    private fun isSubBag(sub: Array<String>, sup: Array<String>): Boolean {
        var j = 0
        for (s in sub) {
            while (j < sup.size && sup[j] < s) j++
            if (j == sup.size || sup[j] != s) return false
            j++
        }
        return true
    }
}
