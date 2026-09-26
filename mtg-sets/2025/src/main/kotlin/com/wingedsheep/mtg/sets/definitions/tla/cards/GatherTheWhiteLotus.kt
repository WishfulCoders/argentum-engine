package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Gather the White Lotus
 * {4}{W}
 * Sorcery
 * Create a 1/1 white Ally creature token for each Plains you control. Scry 2. (Look at the
 * top two cards of your library, then put any number of them on the bottom and the rest on
 * top in any order.)
 *
 * The token count is a resolution-time snapshot of Plains you control via
 * [DynamicAmount.Count] over the battlefield (cf. Marauding Knight). The two clauses run in
 * order with [Effects.Composite]: tokens first, then [Effects.Scry] 2.
 */
val GatherTheWhiteLotus = card("Gather the White Lotus") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Create a 1/1 white Ally creature token for each Plains you control. Scry 2. " +
        "(Look at the top two cards of your library, then put any number of them on the bottom " +
        "and the rest on top in any order.)"

    spell {
        effect = Effects.CreateToken(
            count = DynamicAmounts.count(
                Player.You,
                Zone.BATTLEFIELD,
                GameObjectFilter.Land.withSubtype("Plains"),
            ),
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Ally"),
        ) then
            Effects.Scry(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "20"
        artist = "Kozato"
        flavorText = "\"I see you favor the white lotus gambit. Not many still cling to the ancient ways.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/6/96e2b3d0-f060-4f3c-9d12-a0444b202008.jpg?1764120007"
    }
}
