package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Blade Splicer
 * {2}{W}
 * Creature — Phyrexian Human Artificer
 * 1/1
 *
 * When this creature enters, create a 3/3 colorless Phyrexian Golem artifact creature token.
 * Golems you control have first strike.
 */
val BladeSplicer = card("Blade Splicer") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Phyrexian Human Artificer"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, create a 3/3 colorless Phyrexian Golem artifact creature token.\n" +
        "Golems you control have first strike."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            creatureTypes = setOf("Phyrexian", "Golem"),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/f/e/fe9e8d3b-ebc0-448b-bd14-a9f418e196e7.jpg?1783941286"
        )
    }

    staticAbility {
        ability = GrantKeyword(
            Keyword.FIRST_STRIKE,
            GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.GOLEM).youControl()),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "4"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b8e56a28-713b-4a13-a601-1128cf117539.jpg?1783941327"
    }
}
