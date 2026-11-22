package com.wingedsheep.search

/**
 * Color name expansion for `c:` / `id:` / `cost:` queries.
 *
 * Mirrors the frontend `colorSugar.ts`. Single source of truth: any change
 * here must be mirrored client-side and vice versa, since both implementations
 * have to agree on the grammar that backs `/api/cards/search`.
 */

internal const val W = "WHITE"
internal const val U = "BLUE"
internal const val B = "BLACK"
internal const val R = "RED"
internal const val G = "GREEN"

object ColorSugar {

    val COLOR_LETTER: Map<Char, String> = mapOf(
        'w' to W, 'u' to U, 'b' to B, 'r' to R, 'g' to G,
    )

    /** Full color words — Scryfall accepts both `c:r` and `c:red`. */
    val COLOR_WORD: Map<String, String> = mapOf(
        "white" to W, "blue" to U, "black" to B, "red" to R, "green" to G,
    )

    val NAMED: Map<String, List<String>> = mapOf(
        // Guilds (Ravnica)
        "azorius" to listOf(W, U),
        "dimir" to listOf(U, B),
        "rakdos" to listOf(B, R),
        "gruul" to listOf(R, G),
        "selesnya" to listOf(G, W),
        "orzhov" to listOf(W, B),
        "izzet" to listOf(U, R),
        "golgari" to listOf(B, G),
        "boros" to listOf(R, W),
        "simic" to listOf(G, U),
        // Shards (Alara)
        "bant" to listOf(G, W, U),
        "esper" to listOf(W, U, B),
        "grixis" to listOf(U, B, R),
        "jund" to listOf(B, R, G),
        "naya" to listOf(R, G, W),
        // Wedges (Khans)
        "abzan" to listOf(W, B, G),
        "jeskai" to listOf(U, R, W),
        "sultai" to listOf(B, G, U),
        "mardu" to listOf(R, W, B),
        "temur" to listOf(G, U, R),
        // 4-color
        "yore-tiller" to listOf(W, U, B, R),
        "glint-eye"   to listOf(U, B, R, G),
        "dune-brood"  to listOf(B, R, G, W),
        "ink-treader" to listOf(R, G, W, U),
        "witch-maw"   to listOf(G, W, U, B),
        // Five-color
        "wubrg" to listOf(W, U, B, R, G),
        "fivecolor" to listOf(W, U, B, R, G),
    )

    sealed interface ColorParse {
        object Colorless : ColorParse
        object Multi : ColorParse
        object Mono : ColorParse
        data class Colors(val set: Set<String>) : ColorParse
        data class Count(val value: Int) : ColorParse
        data class Error(val message: String) : ColorParse
    }

    fun parseColorValue(raw: String): ColorParse {
        val v = raw.lowercase().trim()
        if (v.isEmpty()) return ColorParse.Error("Empty color value.")

        v.toIntOrNull()?.let { return ColorParse.Count(it) }

        if (v == "c" || v == "colorless") return ColorParse.Colorless
        if (v == "m" || v == "multi" || v == "multicolor" || v == "multicolour") return ColorParse.Multi
        if (v == "mono" || v == "monocolor" || v == "monocolour") return ColorParse.Mono

        COLOR_WORD[v]?.let { return ColorParse.Colors(setOf(it)) }
        NAMED[v]?.let { return ColorParse.Colors(it.toSet()) }

        val set = HashSet<String>()
        for (ch in v) {
            val color = COLOR_LETTER[ch]
                ?: return ColorParse.Error(
                    "Unknown color \"$ch\". Use w/u/b/r/g, a guild/shard/wedge name, or a number."
                )
            set += color
        }
        return ColorParse.Colors(set)
    }
}
