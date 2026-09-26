package com.wingedsheep.mtg.sets

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.serialization.ScriptTreeWalker
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan

/**
 * Every counter kind a card names is one the SDK names.
 *
 * [CounterType] is open — any word is a kind — so a typo (`"+1+1"`) or a private spelling
 * (`"IMPOSTOR"`) compiles and runs as a counter nothing else ever reads. The closed enum used to
 * rule that out at compile time; this gate rules it out across the corpus instead, by walking each
 * card's whole compiled tree with [ScriptTreeWalker] and checking every [CounterType] it reaches
 * against [CounterType.KNOWN]. A new kind is a constant in [CounterType], not a string on a card.
 */
class CounterTypeVocabularyTest : FunSpec({

    val referenced = MtgSetCatalog.all.flatMap { set ->
        set.cards.flatMap { card ->
            buildList {
                ScriptTreeWalker.forEachNode(card) { node ->
                    if (node is CounterType) add(Triple(set.code, card.name, node))
                }
            }
        }
    }

    test("every counter kind a card names is in CounterType.KNOWN") {
        val unknown = referenced.filter { (_, _, kind) -> kind !in CounterType.KNOWN }
            .map { (set, card, kind) -> "[$set] $card: ${kind.name}" }
            .distinct()
        if (unknown.isNotEmpty()) {
            println("=== counter kinds CounterType does not name: ${unknown.size} ===")
            unknown.forEach { println("  $it") }
        }
        unknown.shouldBeEmpty()
    }

    // The walk can only reject kinds it reaches; a floor keeps a walker change that stops reaching
    // them from turning this gate green by checking nothing.
    test("the walk reaches the corpus's counters") {
        referenced.size shouldBeGreaterThan 1000
    }
})
