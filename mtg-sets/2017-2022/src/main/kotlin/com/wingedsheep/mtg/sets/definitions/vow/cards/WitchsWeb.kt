package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Witch's Web
 * {1}{G}
 * Instant
 * Target creature gets +3/+3 and gains reach until end of turn. Untap it.
 */
val WitchsWeb = card("Witch's Web") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature gets +3/+3 and gains reach until end of turn. Untap it."
    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(3, 3, t) then
            Effects.GrantKeyword(Keyword.REACH, t) then
            Effects.Untap(t)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "227"
        artist = "Yeong-Hao Han"
        flavorText = "Ever since the ritual, Ryla didn't have an appetite at the dinner table, but her long walks in the forest always seemed to satiate her hunger."
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d021207-76be-4bc9-bb71-df991a04d8d8.jpg?1782703032"
    }
}
