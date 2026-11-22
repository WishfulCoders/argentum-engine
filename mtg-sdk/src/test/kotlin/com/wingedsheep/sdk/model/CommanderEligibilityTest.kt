package com.wingedsheep.sdk.model

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.core.TypeLine
import io.kotest.core.spec.style.FunSpec
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class CommanderEligibilityTest : FunSpec({

    test("legendary creature is a legal commander") {
        val card = CardDefinition.creature(
            name = "Yuriko, the Tiger's Shadow",
            manaCost = ManaCost.parse("{1}{U}{B}"),
            subtypes = setOf(Subtype("Human"), Subtype("Ninja")),
            power = 1,
            toughness = 3,
            supertypes = setOf(Supertype.LEGENDARY),
        )
        CommanderEligibility.isLegalCommander(card) shouldBe true
    }

    test("non-legendary creature is not a legal commander by default") {
        val card = CardDefinition.creature(
            name = "Grizzly Bears",
            manaCost = ManaCost.parse("{1}{G}"),
            subtypes = setOf(Subtype("Bear")),
            power = 2,
            toughness = 2,
        )
        CommanderEligibility.isLegalCommander(card) shouldBe false
    }

    test("planeswalker without override clause is not a legal commander") {
        val card = CardDefinition.planeswalker(
            name = "Jace, the Mind Sculptor",
            manaCost = ManaCost.parse("{2}{U}{U}"),
            subtypes = setOf(Subtype("Jace")),
            startingLoyalty = 3,
            oracleText = "+2: Look at the top card of target player's library.",
        )
        CommanderEligibility.isLegalCommander(card) shouldBe false
    }

    test("planeswalker with 'can be your commander' clause is a legal commander") {
        // Daretti, Scrap Savant — and several Commander-precon planeswalkers — have an explicit
        // override clause. The matcher is case-insensitive and doesn't require the card name to
        // appear in the same sentence (some printings phrase it as "This card can be your
        // commander").
        val card = CardDefinition.planeswalker(
            name = "Daretti, Scrap Savant",
            manaCost = ManaCost.parse("{4}{R}"),
            subtypes = setOf(Subtype("Daretti")),
            startingLoyalty = 3,
            oracleText = "+1: Draw two cards, then discard two cards.\nDaretti, Scrap Savant can be your commander.",
        )
        CommanderEligibility.isLegalCommander(card) shouldBe true
    }

    test("non-creature non-planeswalker with override clause is a legal commander") {
        // Faceless One — colorless changeling that explicitly says it can be your commander.
        // Modeled here as a vanilla creature with the override clause to verify the regex
        // matches even when no other branch fires.
        val card = CardDefinition(
            name = "Faceless One",
            manaCost = ManaCost.parse("{2}"),
            typeLine = TypeLine(
                cardTypes = setOf(CardType.CREATURE),
                subtypes = setOf(Subtype("Shapeshifter")),
            ),
            creatureStats = CreatureStats(2, 2),
            oracleText = "Changeling.\nFaceless One can be your commander.",
        )
        CommanderEligibility.isLegalCommander(card) shouldBe true
    }

    test("oracle text is matched case-insensitively") {
        val card = CardDefinition.planeswalker(
            name = "Test Planeswalker",
            manaCost = ManaCost.parse("{3}{R}"),
            subtypes = setOf(Subtype("Test")),
            startingLoyalty = 4,
            oracleText = "TEST PLANESWALKER CAN BE YOUR COMMANDER.",
        )
        CommanderEligibility.isLegalCommander(card) shouldBe true
    }

    // =========================================================================
    // Two commanders — the partner abilities (CR 702.124)
    // =========================================================================

    fun legend(
        name: String,
        vararg partner: KeywordAbility,
        subtypes: Set<Subtype> = setOf(Subtype("Human")),
        legendary: Boolean = true,
    ) = CardDefinition.creature(
        name = name,
        manaCost = ManaCost.parse("{1}{W}"),
        subtypes = subtypes,
        power = 2,
        toughness = 2,
        supertypes = if (legendary) setOf(Supertype.LEGENDARY) else emptySet(),
    ).copy(keywordAbilities = partner.toList())

    fun background(name: String, legendary: Boolean = true) = CardDefinition.enchantment(
        name = name,
        manaCost = ManaCost.parse("{1}{B}"),
        subtypes = setOf(Subtype.BACKGROUND),
        supertypes = if (legendary) setOf(Supertype.LEGENDARY) else emptySet(),
    )

    fun legal(a: CardDefinition, b: CardDefinition) {
        CommanderEligibility.pairingError(a, b) shouldBe null
        CommanderEligibility.pairingError(b, a) shouldBe null
    }

    fun illegal(a: CardDefinition, b: CardDefinition) {
        CommanderEligibility.pairingError(a, b) shouldNotBe null
        CommanderEligibility.pairingError(b, a) shouldNotBe null
    }

    test("partner pairs with partner (702.124h)") {
        legal(legend("Kodama", KeywordAbility.partner()), legend("Ishai", KeywordAbility.partner()))
    }

    test("a commander without a partner ability can't take a second commander") {
        illegal(legend("Kodama", KeywordAbility.partner()), legend("Yuriko"))
        illegal(legend("Yuriko"), legend("Edgar"))
    }

    test("a non-legendary card with partner is still not a commander (903.3)") {
        illegal(legend("Kodama", KeywordAbility.partner()), legend("Commoner", KeywordAbility.partner(), legendary = false))
    }

    test("the same card can't be both commanders") {
        val kodama = legend("Kodama", KeywordAbility.partner())
        CommanderEligibility.pairingError(kodama, kodama) shouldNotBe null
    }

    test("partner—[text] pairs only with the same text (702.124i)") {
        val friendsA = legend("Will", KeywordAbility.partnerNamed("Friends forever"))
        val friendsB = legend("Mike", KeywordAbility.partnerNamed("Friends forever"))
        val survivors = legend("Ellie", KeywordAbility.partnerNamed("Survivors"))
        legal(friendsA, friendsB)
        illegal(friendsA, survivors)
    }

    test("different partner abilities never combine (702.124f)") {
        illegal(legend("Kodama", KeywordAbility.partner()), legend("Will", KeywordAbility.partnerNamed("Friends forever")))
        illegal(legend("Kodama", KeywordAbility.partner()), legend("Pir", KeywordAbility.partnerWith("Kodama")))
    }

    test("partner with [name] pairs only with the named card, naming it back (702.124j)") {
        val pir = legend("Pir, Imaginative Rascal", KeywordAbility.partnerWith("Toothy, Imaginary Friend"))
        val toothy = legend("Toothy, Imaginary Friend", KeywordAbility.partnerWith("Pir, Imaginative Rascal"))
        val stranger = legend("Stranger", KeywordAbility.partnerWith("Pir, Imaginative Rascal"))
        legal(pir, toothy)
        // Stranger names Pir, but Pir doesn't name Stranger back.
        illegal(pir, stranger)
    }

    test("a card with several partner abilities may use any one of them (702.124g)") {
        val both = legend("Both", KeywordAbility.partner(), KeywordAbility.partnerNamed("Friends forever"))
        legal(both, legend("Kodama", KeywordAbility.partner()))
        legal(both, legend("Will", KeywordAbility.partnerNamed("Friends forever")))
    }

    test("choose a Background pairs with a legendary Background enchantment (702.124k)") {
        val chooser = legend("Wilson", KeywordAbility.chooseABackground())
        legal(chooser, background("Raised by Giants"))
        // Two Backgrounds, a non-legendary Background, or a Background beside a plain partner: no.
        illegal(background("Raised by Giants"), background("Cultist of the Absolute"))
        illegal(chooser, background("Nonlegendary Background", legendary = false))
        illegal(legend("Kodama", KeywordAbility.partner()), background("Raised by Giants"))
        illegal(legend("Yuriko"), background("Raised by Giants"))
        // And choose a Background doesn't pair with another creature.
        illegal(chooser, legend("Wilson's Twin", KeywordAbility.chooseABackground()))
    }

    test("a Background is never a commander on its own") {
        CommanderEligibility.isLegalCommander(background("Raised by Giants")) shouldBe false
        CommanderEligibility.isBackground(background("Raised by Giants")) shouldBe true
    }

    test("Doctor's companion pairs with a Time Lord Doctor with no other creature types (702.124m)") {
        val companion = legend("Clara Oswald", KeywordAbility.doctorsCompanion())
        // The parser splits "Time Lord Doctor" on spaces; both spellings are accepted.
        val parsedDoctor = legend("The Eleventh Doctor", subtypes = setOf(Subtype("Time"), Subtype("Lord"), Subtype("Doctor")))
        val canonicalDoctor = legend("The Tenth Doctor", subtypes = setOf(Subtype("Time Lord"), Subtype.DOCTOR))
        legal(companion, parsedDoctor)
        legal(companion, canonicalDoctor)

        // A Doctor with another creature type, a non-Doctor Time Lord, and a non-legendary Doctor: no.
        illegal(companion, legend("Doctor Human", subtypes = setOf(Subtype("Time Lord"), Subtype.DOCTOR, Subtype("Human"))))
        illegal(companion, legend("The Master", subtypes = setOf(Subtype("Time Lord"), Subtype("Rogue"))))
        illegal(companion, legend("Doctor Clone", subtypes = setOf(Subtype("Time Lord"), Subtype.DOCTOR), legendary = false))
        // Two companions don't pair with each other.
        illegal(companion, legend("Rose Tyler", KeywordAbility.doctorsCompanion()))
    }

    test("a pair's color identity is the union of both commanders' (702.124c)") {
        val white = legend("Kodama", KeywordAbility.partner())
        val black = background("Raised by Giants")
        CommanderEligibility.identityOf(listOf(white, black)) shouldBe setOf(Color.WHITE, Color.BLACK)
    }
})
