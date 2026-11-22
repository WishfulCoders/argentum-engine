package com.wingedsheep.gameserver.deck

import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MtgaDeckFormatTest : FunSpec({

    test("parses a simple Deck-only block") {
        val text = """
            Deck
            4 Lightning Bolt
            20 Mountain
        """.trimIndent()

        val deck = MtgaDeckFormat.parse(text)
        deck.commander shouldBe null
        deck.cards.count { it == "Lightning Bolt" } shouldBe 4
        deck.cards.count { it == "Mountain" } shouldBe 20
        deck.size shouldBe 24
    }

    test("parses an explicit Commander section into the commander slot") {
        val text = """
            Deck
            1 Sol Ring
            1 Arcane Signet

            Commander
            1 Yuriko, the Tiger's Shadow
        """.trimIndent()

        val deck = MtgaDeckFormat.parse(text, DeckFormat.COMMANDER)
        deck.commander shouldBe "Yuriko, the Tiger's Shadow"
        deck.cards shouldBe listOf("Sol Ring", "Arcane Signet")
    }

    test("explicit Commander section beats first-card fallback") {
        // Commander format AND a first card present — the section header still wins, no
        // promotion of "Sol Ring" should happen.
        val text = """
            Deck
            1 Sol Ring

            Commander
            1 Yuriko, the Tiger's Shadow
        """.trimIndent()

        val deck = MtgaDeckFormat.parse(text, DeckFormat.COMMANDER)
        deck.commander shouldBe "Yuriko, the Tiger's Shadow"
        deck.cards shouldBe listOf("Sol Ring")
    }

    test("first-card fallback fires for commander-shaped formats with no Commander section") {
        val text = """
            1 Yuriko, the Tiger's Shadow
            1 Sol Ring
            1 Arcane Signet
        """.trimIndent()

        val deck = MtgaDeckFormat.parse(text, DeckFormat.COMMANDER)
        deck.commander shouldBe "Yuriko, the Tiger's Shadow"
        deck.cards shouldBe listOf("Sol Ring", "Arcane Signet")
    }

    test("first-card fallback is suppressed for non-commander formats") {
        // 60-card Standard list — line 1 must NOT silently become a commander even though it
        // happens to be a legendary creature name. The format gate is what protects the
        // singleton-format invariant from leaking into constructed.
        val text = """
            4 Lightning Bolt
            56 Mountain
        """.trimIndent()

        val deck = MtgaDeckFormat.parse(text, DeckFormat.STANDARD)
        deck.commander shouldBe null
        deck.cards.count { it == "Lightning Bolt" } shouldBe 4
        deck.cards.count { it == "Mountain" } shouldBe 56
    }

    test("first-card fallback is suppressed when format is unknown") {
        val text = """
            1 Yuriko, the Tiger's Shadow
            1 Sol Ring
        """.trimIndent()

        val deck = MtgaDeckFormat.parse(text, format = null)
        deck.commander shouldBe null
        deck.cards.size shouldBe 2
    }

    test("section headers are case-insensitive and accept Mainboard/Main") {
        val text = """
            Mainboard
            4 Lightning Bolt

            COMMANDER
            1 Yuriko, the Tiger's Shadow
        """.trimIndent()

        val deck = MtgaDeckFormat.parse(text, DeckFormat.COMMANDER)
        deck.commander shouldBe "Yuriko, the Tiger's Shadow"
        deck.cards.count { it == "Lightning Bolt" } shouldBe 4
    }

    test("strips MTGA's '(SET) <collector-number>' suffix from card names") {
        // Real Arena export shape — without the suffix strip these would be unknown card names
        // when the deck is later validated against the registry.
        val text = """
            Deck
            4 Lightning Bolt (M21) 162
            20 Mountain (M21) 269

            Commander
            1 Yuriko, the Tiger's Shadow (PLST) MIC-242
        """.trimIndent()

        val deck = MtgaDeckFormat.parse(text, DeckFormat.COMMANDER)
        deck.commander shouldBe "Yuriko, the Tiger's Shadow"
        deck.cards.count { it == "Lightning Bolt" } shouldBe 4
        deck.cards.count { it == "Mountain" } shouldBe 20
    }

    test("sideboard and companion sections are recognised but dropped") {
        val text = """
            Deck
            4 Lightning Bolt

            Sideboard
            2 Negate

            Companion
            1 Yorion, Sky Nomad
        """.trimIndent()

        val deck = MtgaDeckFormat.parse(text)
        deck.cards shouldBe listOf("Lightning Bolt", "Lightning Bolt", "Lightning Bolt", "Lightning Bolt")
        deck.commander shouldBe null
    }

    test("blank lines and unrecognised non-numeric lines are ignored") {
        val text = """

            About
            Name My Deck

            Deck
            4 Lightning Bolt
        """.trimIndent()

        val deck = MtgaDeckFormat.parse(text)
        deck.cards shouldBe listOf("Lightning Bolt", "Lightning Bolt", "Lightning Bolt", "Lightning Bolt")
    }

    test("empty Commander section header without entries does NOT trigger first-card fallback") {
        // We saw the section header so the intent was explicit-mode; if there's no entry we
        // honor that and don't silently promote line 1 of Deck as a fallback.
        val text = """
            Deck
            1 Yuriko, the Tiger's Shadow
            1 Sol Ring

            Commander
        """.trimIndent()

        val deck = MtgaDeckFormat.parse(text, DeckFormat.COMMANDER)
        deck.commander shouldBe null
        deck.cards.size shouldBe 2
    }

    test("serialize round-trips a deck with a commander") {
        val deck = Deck(
            cards = listOf("Sol Ring", "Arcane Signet", "Sol Ring"),
            commander = "Yuriko, the Tiger's Shadow",
        )
        val text = MtgaDeckFormat.serialize(deck)
        val parsed = MtgaDeckFormat.parse(text, DeckFormat.COMMANDER)

        parsed.commander shouldBe "Yuriko, the Tiger's Shadow"
        parsed.cards.count { it == "Sol Ring" } shouldBe 2
        parsed.cards.count { it == "Arcane Signet" } shouldBe 1
    }

    test("serialize a deck without commander emits no Commander section") {
        val deck = Deck(cards = List(20) { "Mountain" })
        val text = MtgaDeckFormat.serialize(deck)
        text shouldBe "Deck\n20 Mountain\n"
    }

    test("two cards under Commander become a partner pair (CR 702.124)") {
        val text = """
            Deck
            1 Sol Ring

            Commanders
            1 Kodama of the East Tree (BLC) 227
            1 Ishai, Ojutai Dragonspeaker
        """.trimIndent()

        val deck = MtgaDeckFormat.parse(text, DeckFormat.COMMANDER)
        deck.commander shouldBe "Kodama of the East Tree"
        deck.secondCommander shouldBe "Ishai, Ojutai Dragonspeaker"
        deck.commanders shouldBe listOf("Kodama of the East Tree", "Ishai, Ojutai Dragonspeaker")
        deck.cards shouldBe listOf("Sol Ring")
    }

    test("a third commander line goes back into the deck — no deck has more than two") {
        val text = """
            Commander
            1 First Legend
            1 Second Legend
            1 Third Legend
        """.trimIndent()

        val deck = MtgaDeckFormat.parse(text, DeckFormat.COMMANDER)
        deck.commanders shouldBe listOf("First Legend", "Second Legend")
        deck.cards shouldBe listOf("Third Legend")
    }

    test("serialize round-trips a partner pair") {
        val deck = Deck(
            cards = listOf("Sol Ring"),
            commander = "Kodama of the East Tree",
            secondCommander = "Ishai, Ojutai Dragonspeaker",
        )
        val parsed = MtgaDeckFormat.parse(MtgaDeckFormat.serialize(deck), DeckFormat.COMMANDER)
        parsed.commanders shouldBe deck.commanders
        parsed.cards shouldBe deck.cards
    }
})
