package com.wingedsheep.search

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/** What a compiled query says about itself, and the semantics the web-client relies on. */
class SearchQueryTest : StringSpec({

    fun matching(q: String, cards: List<SearchCard>) = cards.filter(SearchQuery.compile(q)::matches).map { it.name }

    "explanation reads the query as a sentence" {
        SearchQuery.compile("bolt pow>=4").explanation shouldBe "name contains \"bolt\" and power ≥ 4"
        SearchQuery.compile("c<=rw").explanation shouldBe "playable using only white and red"
        SearchQuery.compile("(c:u or c:r) -t:creature").explanation shouldBe
            "(colour identity includes blue or colour identity includes red) and not (type line has \"creature\")"
        SearchQuery.compile("").explanation shouldBe null
        SearchQuery.compile("powr>3").explanation shouldBe null
    }

    "a mistyped key carries a fix replacing just the key" {
        val error = SearchQuery.compile("t:goblin -powr>=3").errors.single()
        error.fix shouldBe Fix(Span(10, 14), "pow")
    }

    "top-level terms are the AND's children; an or at the top has none" {
        val q = "t:goblin (c:r or c:g) -kw:haste"
        SearchQuery.compile(q).topLevelTerms.map { q.substring(it.start, it.end) } shouldBe
            listOf("t:goblin", "(c:r or c:g)", "-kw:haste")
        SearchQuery.compile("t:elf or t:goblin").topLevelTerms shouldBe emptyList()
        SearchQuery.compile("t:elf or t:goblin").isConjunction shouldBe false
        SearchQuery.compile("t:elf").isConjunction shouldBe true
    }

    "setFilter is the s: code on an AND-only path" {
        SearchQuery.compile("s:eoe banishing").setFilter shouldBe "EOE"
        SearchQuery.compile("s:eoe or t:creature").setFilter shouldBe null
        SearchQuery.compile("-s:eoe").setFilter shouldBe null
    }

    "matchedPrintedName finds the printing only when the printed name is what matched" {
        val spidey = TestCard(name = "Spectacular Spider-Man", printedNames = listOf("Ademi of the Silkchutes"))
        SearchQuery.compile("ademi").matchedPrintedName(spidey) shouldBe "Ademi of the Silkchutes"
        SearchQuery.compile("spectacular").matchedPrintedName(spidey) shouldBe null
        SearchQuery.compile("ademi or t:instant").matchedPrintedName(spidey) shouldBe null
        SearchQuery.compile("!\"Cut Down\" or !\"Ademi of the Silkchutes\"").matchedPrintedName(spidey) shouldBe
            "Ademi of the Silkchutes"
    }

    "c<= treats hybrid pips as a choice and gates on uncovered identity colours" {
        val cards = listOf(
            TestCard("Hybrid RW", manaCost = "{R/W}", colorIdentity = listOf("RED", "WHITE")),
            TestCard("Gold UR", manaCost = "{U}{R}", colorIdentity = listOf("BLUE", "RED")),
            TestCard("Phyrexian", manaCost = "{1}{W/P}", colorIdentity = listOf("WHITE")),
            TestCard("Green Land", colorIdentity = listOf("GREEN")),
        )
        matching("c<=w", cards) shouldBe listOf("Hybrid RW", "Phyrexian")
        matching("c<=r", cards) shouldBe listOf("Hybrid RW", "Phyrexian") // {W/P} can be paid with life
        matching("c<=ur", cards) shouldBe listOf("Hybrid RW", "Gold UR", "Phyrexian")
        matching("c<=u", cards) shouldBe listOf("Phyrexian")
        matching("c<=g", cards) shouldBe listOf("Phyrexian", "Green Land")
    }

    "power, toughness and loyalty compare their leading integers" {
        val cards = listOf(
            TestCard("Bear", power = "2", toughness = "2"),
            TestCard("Goyf", power = "*", toughness = "1+*"),
            TestCard("Walker", loyalty = "3"),
            TestCard("Loot", power = "-2", toughness = "4"),
        )
        matching("pow=2", cards) shouldBe listOf("Bear")
        matching("tou<=1", cards) shouldBe listOf("Goyf")
        matching("pow<0", cards) shouldBe listOf("Loot")
        matching("loy>=3", cards) shouldBe listOf("Walker")
    }

    "mana compares cost symbols as multisets" {
        val cards = listOf(
            TestCard("Counterspell", manaCost = "{U}{U}"),
            TestCard("Cancel", manaCost = "{1}{U}{U}"),
        )
        matching("mana:uu", cards) shouldBe listOf("Counterspell")
        matching("mana>={u}{u}", cards) shouldBe listOf("Counterspell", "Cancel")
        matching("mana>uu", cards) shouldBe listOf("Cancel")
        matching("mana!=uu", cards) shouldBe listOf("Cancel")
    }

    "regexes take only the flags both engines read alike" {
        SearchQuery.compile("o:/draw/s").errors shouldBe emptyList()
        SearchQuery.compile("o:/draw/g").errors.single().message shouldBe "Unknown regex flag \"g\" — use i, m or s."
        SearchQuery.compile("name:/[/").errors.single().message.startsWith("Invalid regex") shouldBe true
    }
})
