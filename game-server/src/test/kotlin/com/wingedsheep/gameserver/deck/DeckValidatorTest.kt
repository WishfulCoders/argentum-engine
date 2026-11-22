package com.wingedsheep.gameserver.deck

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.registry.PrintingRegistry
import com.wingedsheep.gameserver.protocol.DeckEntryDTO
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.PrintingRef
import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain as shouldContainString

class DeckValidatorTest : FunSpec({

    val registry = CardRegistry().apply {
        register(PortalSet.cards)
        register(PortalSet.basicLands)

        // Commander-format fixtures. Portal has no legendary creatures, so register a small
        // hand-crafted pool with controlled color identity for the commander tests below.
        register(CardDefinition.creature(
            name = "Test Mono-Green Commander",
            manaCost = ManaCost.parse("{1}{G}"),
            subtypes = setOf(Subtype("Elf")),
            power = 2,
            toughness = 2,
            supertypes = setOf(Supertype.LEGENDARY),
        ))
        register(CardDefinition.creature(
            name = "Test Non-Legendary",
            manaCost = ManaCost.parse("{2}"),
            subtypes = setOf(Subtype("Beast")),
            power = 3,
            toughness = 3,
        ))
        register(CardDefinition.creature(
            name = "Test Green Bear",
            manaCost = ManaCost.parse("{1}{G}"),
            subtypes = setOf(Subtype("Bear")),
            power = 2,
            toughness = 2,
        ))
        register(CardDefinition.creature(
            name = "Test Red Goblin",
            manaCost = ManaCost.parse("{R}"),
            subtypes = setOf(Subtype("Goblin")),
            power = 1,
            toughness = 1,
        ))
        register(CardDefinition.planeswalker(
            name = "Test Walker With Override",
            manaCost = ManaCost.parse("{2}{G}"),
            subtypes = setOf(Subtype("Test")),
            startingLoyalty = 3,
            oracleText = "Test Walker With Override can be your commander.",
        ))

        // Two-commander fixtures (CR 702.124): one pair per partner ability, each half a
        // different color so the combined identity is observable.
        fun legend(name: String, cost: String, partner: KeywordAbility?, subtypes: Set<Subtype> = setOf(Subtype("Human"))) =
            CardDefinition.creature(
                name = name,
                manaCost = ManaCost.parse(cost),
                subtypes = subtypes,
                power = 2,
                toughness = 2,
                supertypes = setOf(Supertype.LEGENDARY),
            ).copy(keywordAbilities = listOfNotNull(partner))
        register(legend("Test Green Partner", "{1}{G}", KeywordAbility.partner()))
        register(legend("Test Red Partner", "{1}{R}", KeywordAbility.partner()))
        register(legend("Test Green Friend", "{1}{G}", KeywordAbility.partnerNamed("Friends forever")))
        register(legend("Test Red Friend", "{1}{R}", KeywordAbility.partnerNamed("Friends forever")))
        register(legend("Test Green Pir", "{1}{G}", KeywordAbility.partnerWith("Test Red Toothy")))
        register(legend("Test Red Toothy", "{1}{R}", KeywordAbility.partnerWith("Test Green Pir")))
        register(legend("Test Green Chooser", "{1}{G}", KeywordAbility.chooseABackground()))
        register(CardDefinition.enchantment(
            name = "Test Red Background",
            manaCost = ManaCost.parse("{1}{R}"),
            subtypes = setOf(Subtype.BACKGROUND),
            supertypes = setOf(Supertype.LEGENDARY),
        ))
        register(legend("Test Green Companion", "{1}{G}", KeywordAbility.doctorsCompanion()))
        register(legend("Test Red Doctor", "{1}{R}", null, subtypes = setOf(Subtype("Time Lord"), Subtype.DOCTOR)))

        // An ante card (CR 407.3). Scryfall marks real ante cards banned everywhere, so this one is
        // "legal" only in Pauper — the ante tests below check that the ban doesn't bite in an ante game.
        register(CardDefinition.creature(
            name = "Test Ante Bear",
            manaCost = ManaCost.parse("{1}{G}"),
            subtypes = setOf(Subtype("Bear")),
            power = 2,
            toughness = 2,
        ).copy(
            oracleText = "Remove Test Ante Bear from your deck before playing if you're not playing for ante.",
            legalFormats = setOf(DeckFormat.PAUPER),
        ))
    }
    val validator = DeckValidator(registry)

    /**
     * Build a Commander-shaped Deck of the given commander padded with Forests up to the
     * 100-card library + commander total. Useful for asserting a single rule in isolation
     * without having to spell out 99 entries per test.
     */
    fun commanderDeckOf(commander: String, extras: List<String> = emptyList()): Deck {
        val padded = extras + List(99 - extras.size) { "Forest" }
        return Deck(cards = padded, commander = commander)
    }

    test("empty deck reports too-few-cards error") {
        val result = validator.validate(emptyMap())
        result.valid shouldBe false
        result.totalCards shouldBe 0
        result.errors.map { it.code } shouldContain "TOO_FEW_CARDS"
    }

    test("legal 40-card mono-mountain deck validates") {
        val deck = mapOf("Mountain" to 40)
        val result = validator.validate(deck)
        result.valid shouldBe true
        result.totalCards shouldBe 40
        result.errors.shouldBeEmpty()
    }

    test("unknown card name produces UNKNOWN_CARD error") {
        val deck = mapOf("Not A Real Card" to 4, "Mountain" to 36)
        val result = validator.validate(deck)
        result.valid shouldBe false
        val unknown = result.errors.single { it.code == "UNKNOWN_CARD" }
        unknown.cardName shouldBe "Not A Real Card"
    }

    test("five copies of a non-basic produces TOO_MANY_COPIES") {
        val deck = mapOf("Test Green Bear" to 5, "Mountain" to 35)
        val result = validator.validate(deck)
        result.valid shouldBe false
        val tooMany = result.errors.single { it.code == "TOO_MANY_COPIES" }
        tooMany.cardName shouldBe "Test Green Bear"
    }

    test("twenty copies of a basic land is fine") {
        val deck = mapOf("Mountain" to 20, "Forest" to 20)
        val result = validator.validate(deck)
        result.valid shouldBe true
    }

    test("collector-number variants of basics stack toward the same name and stay legal") {
        // Two Plains variants both summing to >4 must NOT trigger TOO_MANY_COPIES because Plains is basic.
        val plainsVariants = registry.getCardsByName("Plains")
        if (plainsVariants.size >= 2) {
            val v1 = plainsVariants[0].metadata.collectorNumber!!
            val v2 = plainsVariants[1].metadata.collectorNumber!!
            val setCode = plainsVariants[0].setCode
            val key1 = if (setCode != null) "Plains#$setCode-$v1" else "Plains#$v1"
            val key2 = if (setCode != null) "Plains#$setCode-$v2" else "Plains#$v2"
            val deck = mapOf(key1 to 20, key2 to 20)
            val result = validator.validate(deck)
            result.valid shouldBe true
        }
    }

    test("zero/negative entries are ignored, not counted toward total") {
        val deck = mapOf("Mountain" to 40, "Forest" to 0)
        val result = validator.validate(deck)
        result.totalCards shouldBe 40
        result.valid shouldBe true
    }

    test("under-40 deck is flagged") {
        val deck = mapOf("Mountain" to 30)
        val result = validator.validate(deck)
        result.valid shouldBe false
        result.errors.map { it.code } shouldContain "TOO_FEW_CARDS"
    }

    // ---------------------------------------------------------------------------
    // Commander format
    // ---------------------------------------------------------------------------

    test("Commander rejects a deck that isn't exactly 100 cards") {
        // 99 mountains: legal in any other format, illegal under Commander's exact-100 rule.
        val deck = mapOf("Mountain" to 99)
        val result = validator.validate(deck, DeckFormat.COMMANDER)
        result.valid shouldBe false
        result.errors.map { it.code } shouldContain "TOO_FEW_CARDS"
    }

    test("Commander accepts an all-basics 100-card deck") {
        // Commander is a singleton format but basics override the cap, so 100 mountains is legal
        // (ignoring legality data for this test card pool).
        val deck = mapOf("Mountain" to 100)
        val result = validator.validate(deck, DeckFormat.COMMANDER)
        // The Portal test pool doesn't tag legalFormats so format-legality errors don't appear;
        // the only thing this test asserts is "no copy-cap or size error".
        result.errors.none { it.code == "TOO_MANY_COPIES" } shouldBe true
        result.errors.none { it.code == "TOO_FEW_CARDS" || it.code == "TOO_MANY_CARDS" } shouldBe true
    }

    test("Commander rejects two copies of a non-basic non-override card") {
        val deck = mapOf("Test Green Bear" to 2, "Mountain" to 98)
        val result = validator.validate(deck, DeckFormat.COMMANDER)
        result.valid shouldBe false
        val tooMany = result.errors.single { it.code == "TOO_MANY_COPIES" }
        tooMany.cardName shouldBe "Test Green Bear"
        // Singleton message is more informative than the raw 4-of variant.
        tooMany.message shouldContainString "singleton"
    }

    test("non-Commander formats keep the 4-of cap, not singleton") {
        val deck = mapOf("Test Green Bear" to 4, "Mountain" to 56)
        val result = validator.validate(deck, DeckFormat.MODERN)
        // Modern accepts 4-of so no copy-cap error fires here, even though Commander would reject.
        result.errors.none { it.code == "TOO_MANY_COPIES" } shouldBe true
    }

    // ---------------------------------------------------------------------------
    // Per-card "any number" override (parser-level)
    // ---------------------------------------------------------------------------

    test("parser detects 'any number of cards named' override") {
        val rule = DeckValidator.parseDeckSizeOverride(
            oracleText = "A deck can have any number of cards named Relentless Rats.",
            cardName = "Relentless Rats"
        )
        rule shouldBe DeckValidator.Companion.OverrideRule(cap = Int.MAX_VALUE, named = "Relentless Rats")
    }

    test("parser detects 'up to seven cards named' override and parses the word") {
        val rule = DeckValidator.parseDeckSizeOverride(
            oracleText = "A deck can have up to seven cards named Seven Dwarves.",
            cardName = "Seven Dwarves"
        )
        rule shouldBe DeckValidator.Companion.OverrideRule(cap = 7, named = "Seven Dwarves")
    }

    test("parser ignores override that names a different card") {
        val rule = DeckValidator.parseDeckSizeOverride(
            oracleText = "A deck can have any number of cards named Persistent Petitioners.",
            cardName = "Bog Imp"
        )
        rule shouldBe null
    }

    // ---------------------------------------------------------------------------
    // Commander rules — eligibility, color identity, deck shape via Deck overload
    // ---------------------------------------------------------------------------

    test("Commander Deck with legal commander and on-identity cards passes") {
        val deck = commanderDeckOf("Test Mono-Green Commander")
        val result = validator.validate(deck, DeckFormat.COMMANDER)
        result.errors.none {
            it.code in setOf("INVALID_COMMANDER", "COLOR_IDENTITY_VIOLATION", "MISSING_COMMANDER")
        } shouldBe true
        result.totalCards shouldBe 100
    }

    test("Commander Deck without a commander surfaces no error from the legacy Map path") {
        // Map overload still enforces the 100-card singleton shape but must NOT raise
        // MISSING_COMMANDER — the legacy submission surface doesn't carry a commander, and
        // existing callers shouldn't suddenly start failing.
        val result = validator.validate(mapOf("Forest" to 100), DeckFormat.COMMANDER)
        result.errors.none { it.code == "MISSING_COMMANDER" } shouldBe true
    }

    test("Commander Deck overload without a commander raises MISSING_COMMANDER") {
        val deck = Deck(cards = List(100) { "Forest" }, commander = null)
        val result = validator.validate(deck, DeckFormat.COMMANDER)
        result.errors.map { it.code } shouldContain "MISSING_COMMANDER"
    }

    test("non-legendary commander is rejected with INVALID_COMMANDER") {
        val deck = commanderDeckOf("Test Non-Legendary")
        val result = validator.validate(deck, DeckFormat.COMMANDER)
        result.valid shouldBe false
        val invalid = result.errors.single { it.code == "INVALID_COMMANDER" }
        invalid.cardName shouldBe "Test Non-Legendary"
        // No identity check should fire when the commander itself isn't legal — the comparison
        // would be misleading.
        result.errors.none { it.code == "COLOR_IDENTITY_VIOLATION" } shouldBe true
    }

    test("planeswalker commander with override clause is accepted") {
        val deck = commanderDeckOf("Test Walker With Override")
        val result = validator.validate(deck, DeckFormat.COMMANDER)
        result.errors.none { it.code == "INVALID_COMMANDER" } shouldBe true
    }

    test("off-color card under a mono-green commander raises COLOR_IDENTITY_VIOLATION") {
        // Mono-green commander, deck contains a red goblin — that's a color-identity break.
        val deck = commanderDeckOf("Test Mono-Green Commander", extras = listOf("Test Red Goblin"))
        val result = validator.validate(deck, DeckFormat.COMMANDER)
        result.valid shouldBe false
        val violation = result.errors.single { it.code == "COLOR_IDENTITY_VIOLATION" }
        violation.cardName shouldBe "Test Red Goblin"
        violation.message shouldContainString "Red"
    }

    test("on-color cards under a mono-green commander pass identity check") {
        val deck = commanderDeckOf(
            "Test Mono-Green Commander",
            extras = List(4) { "Test Green Bear" },
        )
        val result = validator.validate(deck, DeckFormat.COMMANDER)
        result.errors.none { it.code == "COLOR_IDENTITY_VIOLATION" } shouldBe true
    }

    test("on-color basics (Forest under green commander) pass identity check") {
        val deck = commanderDeckOf("Test Mono-Green Commander")
        val result = validator.validate(deck, DeckFormat.COMMANDER)
        result.errors.none { it.code == "COLOR_IDENTITY_VIOLATION" } shouldBe true
    }

    test("off-color basics (Mountain under green commander) raise COLOR_IDENTITY_VIOLATION") {
        // Mountains contribute red to color identity via their Mountain subtype (CR 903.4 —
        // basic land types are associated with their colors). A 99-Mountain deck under a
        // mono-green commander must therefore fail identity, not pass.
        val deck = Deck(
            cards = List(99) { "Mountain" },
            commander = "Test Mono-Green Commander",
        )
        val result = validator.validate(deck, DeckFormat.COMMANDER)
        result.valid shouldBe false
        val violation = result.errors.single { it.code == "COLOR_IDENTITY_VIOLATION" }
        violation.cardName shouldBe "Mountain"
        violation.message shouldContainString "Red"
    }

    test("non-commander formats ignore the Deck.commander field") {
        // If someone submits a Standard list via the Deck overload with a stray commander, the
        // commander rules don't apply — Standard's profile stays in charge.
        val deck = Deck(
            cards = List(60) { "Mountain" },
            commander = "Test Mono-Green Commander",  // ignored by Standard validation
        )
        val result = validator.validate(deck, DeckFormat.STANDARD)
        result.errors.none {
            it.code in setOf("INVALID_COMMANDER", "COLOR_IDENTITY_VIOLATION", "MISSING_COMMANDER")
        } shouldBe true
    }

    test("commander appearing again in main deck trips the singleton cap") {
        // CR 903.5b: the commander begins in the command zone and the deck is singleton, so
        // the commander cannot also appear in the 99. We surface this as TOO_MANY_COPIES via
        // the merged-counts check rather than a bespoke error.
        val deck = Deck(
            cards = listOf("Test Mono-Green Commander") + List(98) { "Forest" },
            commander = "Test Mono-Green Commander",
        )
        val result = validator.validate(deck, DeckFormat.COMMANDER)
        result.errors.any {
            it.code == "TOO_MANY_COPIES" && it.cardName == "Test Mono-Green Commander"
        } shouldBe true
    }

    // -----------------------------------------------------------------------
    // Pinned-printing validation (Phase 4 of the multi-printing plan).
    // The validator only enforces printings when a [PrintingRegistry] is wired —
    // legacy callers without one continue to pass-through silently.
    // -----------------------------------------------------------------------

    test("printing-aware: unknown PrintingRef raises INVALID_PRINTING") {
        val printings = PrintingRegistry().apply {
            register(Printing(
                oracleId = "balsam-oracle",
                name = "Balsam Wolf",
                setCode = "POR",
                collectorNumber = "194",
            ))
        }
        val printingValidator = DeckValidator(registry, printings)
        val entries = List(60) { DeckEntryDTO("Balsam Wolf", PrintingRef("UNKNOWN", "999")) }
        val result = printingValidator.validate(
            deckList = mapOf("Balsam Wolf" to 60),
            cardEntries = entries,
        )
        result.valid shouldBe false
        result.errors.any { it.code == "INVALID_PRINTING" } shouldBe true
    }

    test("printing-aware: PrintingRef pointing at a different card raises INVALID_PRINTING") {
        val printings = PrintingRegistry().apply {
            register(Printing(
                oracleId = "wolf-oracle",
                name = "Balsam Wolf",
                setCode = "POR",
                collectorNumber = "194",
            ))
            register(Printing(
                oracleId = "barbarian-oracle",
                name = "Mountain Bandit",
                setCode = "POR",
                collectorNumber = "143",
            ))
        }
        val printingValidator = DeckValidator(registry, printings)
        // Trying to bind a Balsam Wolf entry to the Mountain Bandit printing.
        val result = printingValidator.validate(
            deckList = mapOf("Balsam Wolf" to 60),
            cardEntries = List(60) { DeckEntryDTO("Balsam Wolf", PrintingRef("POR", "143")) },
        )
        result.valid shouldBe false
        val issue = result.errors.first { it.code == "INVALID_PRINTING" }
        issue.message shouldContainString "Mountain Bandit"
    }

    test("printing-aware: matching PrintingRef passes printing check") {
        val printings = PrintingRegistry().apply {
            register(Printing(
                oracleId = "wolf-oracle",
                name = "Balsam Wolf",
                setCode = "POR",
                collectorNumber = "194",
            ))
        }
        val printingValidator = DeckValidator(registry, printings)
        val entries = List(20) { DeckEntryDTO("Balsam Wolf", PrintingRef("POR", "194")) } +
            List(40) { DeckEntryDTO("Forest") }
        val result = printingValidator.validate(
            deckList = mapOf("Balsam Wolf" to 20, "Forest" to 40),
            cardEntries = entries,
        )
        result.errors.none { it.code == "INVALID_PRINTING" } shouldBe true
    }

    test("printing-aware: null entries are skipped (entry without pinned printing)") {
        val printings = PrintingRegistry()  // empty registry — would reject any printing
        val printingValidator = DeckValidator(registry, printings)
        // None of the entries pin a printing; the registry being empty is irrelevant.
        val entries = List(60) { DeckEntryDTO("Forest") }
        val result = printingValidator.validate(
            deckList = mapOf("Forest" to 60),
            cardEntries = entries,
        )
        result.errors.none { it.code == "INVALID_PRINTING" } shouldBe true
    }

    test("legacy validator without PrintingRegistry skips printing check entirely") {
        // Same payload as the failing tests above — but without [PrintingRegistry] the
        // validator silently accepts any printing string. This keeps existing call sites
        // backward-compatible until they migrate to the new constructor.
        val entries = List(60) { DeckEntryDTO("Balsam Wolf", PrintingRef("UNKNOWN", "999")) }
        val result = validator.validate(
            deckList = mapOf("Balsam Wolf" to 60),
            cardEntries = entries,
        )
        result.errors.none { it.code == "INVALID_PRINTING" } shouldBe true
    }

    // =========================================================================
    // Two commanders — the partner abilities (CR 702.124)
    // =========================================================================

    /** A two-commander deck: 98 [extras]-then-Forests in the library plus both commanders = 100. */
    fun pairDeckOf(first: String, second: String, extras: List<String> = emptyList()): Deck =
        Deck(cards = extras + List(98 - extras.size) { "Forest" }, commander = first, secondCommander = second)

    fun codes(deck: Deck) = validator.validate(deck, DeckFormat.COMMANDER).errors.map { it.code }

    test("each partner ability admits its own pair, with both commanders counting toward 100") {
        for ((first, second) in listOf(
            "Test Green Partner" to "Test Red Partner",
            "Test Green Friend" to "Test Red Friend",
            "Test Green Pir" to "Test Red Toothy",
            "Test Green Chooser" to "Test Red Background",
            "Test Green Companion" to "Test Red Doctor",
        )) {
            val result = validator.validate(pairDeckOf(first, second, extras = listOf("Mountain")), DeckFormat.COMMANDER)
            withClue("$first + $second: ${result.errors}") {
                result.valid shouldBe true
                result.totalCards shouldBe 100
            }
        }
    }

    test("a pair whose partner abilities don't match raises INVALID_COMMANDER_PAIR (702.124f)") {
        for ((first, second) in listOf(
            // Different partner abilities never combine.
            "Test Green Partner" to "Test Red Friend",
            "Test Green Friend" to "Test Red Toothy",
            // Partner with names a specific card.
            "Test Green Pir" to "Test Red Partner",
            // Choose a Background needs a Background; a Background needs choose a Background.
            "Test Green Chooser" to "Test Red Partner",
            "Test Green Partner" to "Test Red Background",
            // Doctor's companion needs a Time Lord Doctor; the Doctor has no partner of its own.
            "Test Green Companion" to "Test Red Partner",
            "Test Green Partner" to "Test Red Doctor",
            // A commander without any partner ability can't take a second.
            "Test Mono-Green Commander" to "Test Red Partner",
        )) {
            withClue("$first + $second") {
                codes(pairDeckOf(first, second)) shouldContain "INVALID_COMMANDER_PAIR"
            }
        }
    }

    test("a Background alone is not a commander") {
        val result = validator.validate(commanderDeckOf("Test Red Background", extras = listOf("Mountain")), DeckFormat.COMMANDER)
        result.errors.map { it.code } shouldContain "INVALID_COMMANDER"
        result.errors.first { it.code == "INVALID_COMMANDER" }.message shouldContainString "Choose a Background"
    }

    test("color identity is the union of both commanders' (702.124c)") {
        // A red card and a green card both fit a red + green pair…
        codes(pairDeckOf("Test Green Partner", "Test Red Partner", extras = listOf("Test Red Goblin", "Test Green Bear")))
            .none { it == "COLOR_IDENTITY_VIOLATION" } shouldBe true
        // …but a red card is off-identity for the green commander alone.
        codes(commanderDeckOf("Test Green Partner", extras = listOf("Test Red Goblin")))
            .shouldContain("COLOR_IDENTITY_VIOLATION")
        // And a white card is off-identity for the pair.
        codes(pairDeckOf("Test Green Partner", "Test Red Partner", extras = listOf("Plains")))
            .shouldContain("COLOR_IDENTITY_VIOLATION")
    }

    test("a pair's deck is 100 cards including both commanders (702.124b)") {
        // 99 library cards + 2 commanders = 101.
        val tooMany = Deck(cards = List(99) { "Forest" }, commander = "Test Green Partner", secondCommander = "Test Red Partner")
        codes(tooMany) shouldContain "TOO_MANY_CARDS"
    }

    test("the second commander listed again in the library trips the singleton cap") {
        codes(pairDeckOf("Test Green Partner", "Test Red Partner", extras = listOf("Test Red Partner")))
            .shouldContain("TOO_MANY_COPIES")
    }

    test("an ante card is recognised from its Oracle text (CR 407.3)") {
        registry.getCard("Test Ante Bear")!!.requiresAnte shouldBe true
        registry.getCard("Test Green Bear")!!.requiresAnte shouldBe false
    }

    test("an ante card in the deck is REQUIRES_ANTE when not playing for ante") {
        val result = validator.validate(mapOf("Test Ante Bear" to 1, "Forest" to 39))
        result.valid shouldBe false
        val issue = result.errors.single { it.code == "REQUIRES_ANTE" }
        issue.cardName shouldBe "Test Ante Bear"
        issue.message shouldContainString "played for ante"
    }

    test("an ante card is legal when playing for ante, even in a format that bans it") {
        val deck = mapOf("Test Ante Bear" to 1, "Forest" to 59)
        validator.validate(deck, playingForAnte = true).valid shouldBe true
        val vintage = validator.validate(deck, DeckFormat.VINTAGE, playingForAnte = true)
        withClue(vintage.errors) { vintage.valid shouldBe true }
        // Without ante the only complaint is the ante one, not a second NOT_LEGAL_IN_FORMAT.
        validator.validate(deck, DeckFormat.VINTAGE).errors.map { it.code } shouldBe listOf("REQUIRES_ANTE")
    }

    test("an ante card in the sideboard is REQUIRES_ANTE when not playing for ante") {
        val deck = Deck(
            cards = List(40) { "Forest" },
            sideboard = listOf(com.wingedsheep.sdk.model.CardEntry("Test Ante Bear")),
        )
        validator.validate(deck).errors.map { it.code } shouldBe listOf("REQUIRES_ANTE")
        validator.validate(deck, playingForAnte = true).valid shouldBe true
    }

    test("Commander-draft validation applies the same pairing rules against the pool") {
        val pool = listOf("Test Green Chooser", "Test Red Background", "Test Green Bear")
            .map { registry.getCard(it)!! }
        val legal = Deck(
            cards = listOf("Test Green Bear") + List(57) { "Forest" },
            commander = "Test Green Chooser",
            secondCommander = "Test Red Background",
        )
        validator.validateCommanderLimited(legal, pool).valid shouldBe true

        val illegal = legal.copy(secondCommander = "Test Red Partner")
        validator.validateCommanderLimited(illegal, pool + registry.getCard("Test Red Partner")!!)
            .errors.map { it.code } shouldContain "INVALID_COMMANDER_PAIR"
    }
})
