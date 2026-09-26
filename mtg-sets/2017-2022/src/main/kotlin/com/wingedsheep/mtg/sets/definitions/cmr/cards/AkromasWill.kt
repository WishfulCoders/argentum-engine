package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Akroma's Will
 * {3}{W}
 * Instant
 * Choose one. If you control a commander as you cast this spell, you may choose both instead.
 * • Creatures you control gain flying, vigilance, and double strike until end of turn.
 * • Creatures you control gain lifelink, indestructible, and protection from each color until end
 *   of turn.
 *
 * Each mode grants keywords to the creatures you control as it resolves ([Effects.ForEachInGroup]);
 * protection from each color is the five single-color grants (colorless is not a color, ruling).
 * The engine has no commanders, so the "choose both" clause can never apply and the spell is a
 * plain choose-one here.
 */
val AkromasWill = card("Akroma's Will") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Choose one. If you control a commander as you cast this spell, you may choose both instead.\n" +
        "• Creatures you control gain flying, vigilance, and double strike until end of turn.\n" +
        "• Creatures you control gain lifelink, indestructible, and protection from each color until end of turn."

    spell {
        modal(chooseCount = 1) {
            mode("Creatures you control gain flying, vigilance, and double strike until end of turn.") {
                effect = Effects.ForEachInGroup(
                    GroupFilter.AllCreaturesYouControl,
                    Effects.Composite(
                        listOf(Keyword.FLYING, Keyword.VIGILANCE, Keyword.DOUBLE_STRIKE)
                            .map { Effects.GrantKeyword(it, EffectTarget.IterationEntity) }
                    )
                )
            }
            mode("Creatures you control gain lifelink, indestructible, and protection from each color until end of turn.") {
                effect = Effects.ForEachInGroup(
                    GroupFilter.AllCreaturesYouControl,
                    Effects.Composite(
                        listOf(Keyword.LIFELINK, Keyword.INDESTRUCTIBLE).map { Effects.GrantKeyword(it, EffectTarget.IterationEntity) } +
                            Color.entries.map { Effects.GrantProtectionFromColor(it, EffectTarget.IterationEntity) }
                    )
                )
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "3"
        artist = "Antonio José Manzanedo"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c281997b-1566-4469-a14c-6645f81ab023.jpg?1783928892"
        ruling("2025-11-17", "\"Protection from each color\" is shorthand for protection from white, from blue, from black, from red, and from green. Colorless is not a color.")
    }
}
