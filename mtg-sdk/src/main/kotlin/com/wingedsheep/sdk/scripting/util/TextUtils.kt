package com.wingedsheep.sdk.scripting.util

/**
 * "a Goblin" / "three Goblins" — the article-or-count phrase shared by the selection atoms. Small
 * counts are spelled out (oracle convention); the article respects a vowel-leading filter description.
 */
fun quantify(count: Int, filterDescription: String): String =
    if (count == 1) {
        val article = if (filterDescription.firstOrNull()?.lowercaseChar() in listOf('a', 'e', 'i', 'o', 'u')) "an" else "a"
        "$article $filterDescription"
    } else {
        "${numberToWord(count)} ${pluralNounPhrase(filterDescription)}"
    }

/**
 * The plural of a singular object noun phrase as a filter describes it: "creature you control" →
 * "creatures you control", "artifact or enchantment" → "artifacts or enchantments", "creature card
 * in your graveyard" → "creature cards in your graveyard".
 *
 * Filter descriptions put their qualifiers *after* the type word, so appending "s" to the phrase
 * ("creature you controls") is wrong; every type noun that heads a noun group takes the plural
 * instead, and one that modifies the type noun after it ("creature card") stays singular. A phrase
 * with no recognizable type noun pluralizes its last word.
 */
fun pluralNounPhrase(singular: String): String {
    val words = singular.split(" ")
    fun bare(word: String) = word.trimEnd(',').lowercase()
    fun isHead(index: Int): Boolean {
        val word = words[index]
        val next = words.getOrNull(index + 1)?.let(::bare)
        return bare(word) in TYPE_NOUNS && (next !in TYPE_NOUNS || word.endsWith(','))
    }
    // "creature or planeswalker card" is a group of *cards*: only the object noun pluralizes
    val objectHeads = words.indices.filter { isHead(it) && bare(words[it]) in OBJECT_NOUNS }
    var any = false
    val plural = words.mapIndexed { index, word ->
        if (isHead(index) && (objectHeads.isEmpty() || index in objectHeads)) {
            any = true
            val comma = if (word.endsWith(',')) "," else ""
            pluralizeTypeNoun(word.trimEnd(',')) + comma
        } else {
            word
        }
    }
    if (any) return plural.joinToString(" ")
    // no type noun ("Mount or Vehicle", "Goblin"): each alternative pluralizes its last word
    return singular.split(" or ").joinToString(" or ") { part ->
        val w = part.split(" ")
        w.dropLast(1).plus(com.wingedsheep.sdk.scripting.pluralizeHeadNoun(w.last())).joinToString(" ")
    }
}

private fun pluralizeTypeNoun(word: String): String = when (word.lowercase()) {
    "ability" -> word.dropLast(1) + "ies"
    else -> "${word}s"
}

/** Nouns naming the object itself rather than its card type ("creature *card*", "instant *spell*"). */
private val OBJECT_NOUNS = setOf("card", "spell", "ability")

/** The object nouns a filter or target description heads with. */
private val TYPE_NOUNS = setOf(
    "creature", "permanent", "artifact", "enchantment", "land", "planeswalker", "battle",
    "token", "card", "spell", "ability", "player", "opponent", "object",
)

fun numberToWord(n: Int): String = when (n) {
    1 -> "one"
    2 -> "two"
    3 -> "three"
    4 -> "four"
    5 -> "five"
    6 -> "six"
    7 -> "seven"
    8 -> "eight"
    9 -> "nine"
    10 -> "ten"
    else -> n.toString()
}
