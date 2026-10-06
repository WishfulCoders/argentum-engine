package com.wingedsheep.gameserver.jumpstart

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.sdk.model.CardDefinition
import kotlin.random.Random

/** Published paper packs. A missing or banned card disables the entire variant, never substitutes. */
data class JumpstartPack(val id: String, val cards: List<CardDefinition>) {
    val theme: String get() = id.substringBefore(" (")
}

class JumpstartPacks(generator: BoosterGenerator) {
    // JMP packs also contain M21 cards, whose canonical definition can live in any earlier set.
    private val cardsByName = generator.availableSets.values
        .flatMap { it.cards + it.basicLands }
        .associateBy { it.name } + generator.getSetConfig("JMP")?.let {
            (it.cards + it.basicLands).associateBy { card -> card.name }
        }.orEmpty()

    val packs: List<JumpstartPack> = lists.mapNotNull { (id, names) ->
        val cards = names.map { cardsByName[it] ?: return@mapNotNull null }
        JumpstartPack(id, cards)
    }

    fun available(banned: Set<String>): List<JumpstartPack> = packs.filter { pack ->
        pack.cards.none { card -> banned.any { it.equals(card.name, ignoreCase = true) } }
    }

    /** Offer distinct themes, then roll a variant within each theme. Same themes may recur in pick two. */
    fun offer(banned: Set<String>, random: Random = Random.Default): List<String> =
        available(banned).groupBy { it.theme }.values.shuffled(random).take(3)
            .map { it.random(random).id }

    companion object {
        val lists: Map<String, List<String>> by lazy {
            val result = linkedMapOf<String, MutableList<String>>()
            var current: MutableList<String>? = null
            requireNotNull(JumpstartPacks::class.java.getResourceAsStream("/jumpstart/jmp.txt"))
                .bufferedReader().useLines { lines ->
                    lines.filter { it.isNotBlank() && !it.startsWith("#") }.forEach { line ->
                        if (line.startsWith("[")) {
                            current = mutableListOf<String>().also { result[line.removeSurrounding("[", "]")] = it }
                        } else {
                            val (count, name) = line.split(" ", limit = 2)
                            repeat(count.toInt()) { requireNotNull(current).add(name) }
                        }
                    }
                }
            require(result.size == 121)
            require(result.values.all { it.size == 20 }) { "Every Jumpstart pack must contain exactly 20 cards" }
            result
        }
    }
}
