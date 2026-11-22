package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Markov Baron — March of the Machine: The Aftermath #14
 * {2}{B} · Creature — Vampire Noble · 2/2
 *
 * Convoke
 * Lifelink
 * Other Vampires you control get +1/+1.
 * Madness {2}{B}
 */
val MarkovBaron = card("Markov Baron") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Noble"
    power = 2
    toughness = 2
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while " +
        "casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "Lifelink\n" +
        "Other Vampires you control get +1/+1.\n" +
        "Madness {2}{B} (If you discard this card, discard it into exile. When you do, cast it " +
        "for its madness cost or put it into your graveyard.)"

    keywords(Keyword.CONVOKE, Keyword.LIFELINK)

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Permanent.withSubtype(Subtype.VAMPIRE).youControl(),
                excludeSelf = true
            )
        )
    }

    madness("{2}{B}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "14"
        artist = "Ina Wong"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a1dc6b0f-09bb-4489-8022-42c700eb1191.jpg?1783916524"
    }
}
