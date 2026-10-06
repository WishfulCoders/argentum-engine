package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Yavimaya, Cradle of Growth — Modern Horizons 2 #261
 * (no mana cost) · Legendary Land
 *
 * Each land is a Forest in addition to its other land types.
 *
 * A layer-4 [GrantSubtype] over every land on the battlefield (both players', Yavimaya included),
 * the Leyline of the Guildpact shape with one subtype and no controller restriction. The engine
 * derives the Forest's intrinsic "{T}: Add {G}" (CR 305.6) from the projected subtype, so the
 * card needs no printed mana ability of its own. Cards off the battlefield are untouched — a
 * battlefield [GroupFilter] never reaches them, as the rulings require.
 *
 * `colorIdentity` is empty: the {G} ability is granted by type, not printed in its rules text
 * (CR 903.4).
 */
val YavimayaCradleOfGrowth = card("Yavimaya, Cradle of Growth") {
    manaCost = ""
    typeLine = "Legendary Land"
    oracleText = "Each land is a Forest in addition to its other land types."

    staticAbility {
        ability = GrantSubtype("Forest", GroupFilter(GameObjectFilter.Land))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "261"
        artist = "Sarah Finnigan"
        flavorText = "\"Multani's heart is a seed, and all of Yavimaya is its flower. There is as much life here as in the rest of Dominaria together.\"\n—Karn"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e4b6e22-93b2-4896-bba5-0ceaa5d8ea3c.jpg?1783926791"

        ruling("2021-06-18", "Yavimaya, Cradle of Growth isn't a Forest while it's not on the battlefield.")
        ruling("2021-06-18", "Land cards not on the battlefield aren't Forests while Yavimaya is on the battlefield.")
        ruling(
            "2021-06-18",
            "Yavimaya's ability causes each land on the battlefield to have the land type Forest. Any land that's a " +
                "Forest has the ability \"{T}: Add {G}.\" Nothing else changes about those lands, including their " +
                "names, other subtypes, and whether they're legendary, basic, or snow."
        )
    }
}
