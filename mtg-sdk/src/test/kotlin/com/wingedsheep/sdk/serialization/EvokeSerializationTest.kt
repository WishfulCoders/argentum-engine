package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.evokeWith
import com.wingedsheep.sdk.scripting.CostZone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain

/**
 * Evoke (CR 702.74) with and without a non-mana part. A mana-only evoke must serialize exactly as
 * before — `additionalCosts` is a default and `encodeDefaults = false` omits it — so no existing
 * evoke card's golden snapshot moves.
 */
class EvokeSerializationTest : StringSpec({
    val pitchWhite = Costs.additional.ExileCards(
        count = 1,
        filter = GameObjectFilter.Any.withColor(Color.WHITE),
        fromZone = CostZone.HAND
    )

    fun roundTrip(original: KeywordAbility): KeywordAbility =
        CardSerialization.json.decodeFromString(
            KeywordAbility.serializer(),
            CardSerialization.json.encodeToString(KeywordAbility.serializer(), original)
        )

    "a mana-only evoke round-trips and its JSON carries no additionalCosts field" {
        val original = KeywordAbility.evoke("{2}{U}")
        roundTrip(original) shouldBe original
        CardSerialization.json.encodeToString(KeywordAbility.serializer(), original) shouldNotContain "additionalCosts"
        original.description shouldBe "Evoke {2}{U}"
        (original as KeywordAbility.Evoke).additionalCosts shouldBe emptyList()
    }

    "a non-mana evoke keeps its exile-from-hand cost through serialization" {
        val original = KeywordAbility.Evoke(ManaCost.parse("{0}"), listOf(pitchWhite))
        val restored = roundTrip(original)
        restored shouldBe original
        restored.keyword shouldBe Keyword.EVOKE
        (restored as KeywordAbility.Evoke).additionalCosts shouldBe listOf(pitchWhite)
    }

    "the description reads the way the cost is printed" {
        KeywordAbility.Evoke(ManaCost.parse("{0}"), listOf(pitchWhite)).description shouldBe
            "Evoke—${pitchWhite.description}"
        KeywordAbility.Evoke(ManaCost.parse("{1}"), listOf(pitchWhite)).description shouldBe
            "Evoke—{1}, " + pitchWhite.description.replaceFirstChar { it.lowercaseChar() }
    }

    "the evokeWith DSL helper attaches one Evoke keyword ability with the non-mana cost" {
        val def = card("Pitch Evoke Probe") {
            manaCost = "{3}{W}{W}"
            typeLine = "Creature — Elemental Incarnation"
            power = 3
            toughness = 2
            evokeWith(pitchWhite)
        }
        val evokes = def.keywordAbilities.filterIsInstance<KeywordAbility.Evoke>()
        evokes shouldBe listOf(KeywordAbility.Evoke(ManaCost.parse("{0}"), listOf(pitchWhite)))
        (Keyword.EVOKE in def.keywords) shouldBe true
    }

    "evokeWith refuses an empty non-mana cost and a second evoke cost" {
        shouldThrow<IllegalArgumentException> {
            card("Empty Evoke Probe") {
                manaCost = "{1}"
                typeLine = "Creature — Elemental"
                power = 1
                toughness = 1
                evokeWith()
            }
        }
        shouldThrow<IllegalArgumentException> {
            card("Double Evoke Probe") {
                manaCost = "{1}"
                typeLine = "Creature — Elemental"
                power = 1
                toughness = 1
                evoke = "{W}"
                evokeWith(pitchWhite)
            }
        }
    }
})
