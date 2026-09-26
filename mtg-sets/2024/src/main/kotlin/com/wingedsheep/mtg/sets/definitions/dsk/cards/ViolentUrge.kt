package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Violent Urge
 * {R}
 * Instant
 * Target creature gets +1/+0 and gains first strike until end of turn.
 * Delirium — If there are four or more card types among cards in your graveyard, that creature
 * gains double strike until end of turn.
 */
val ViolentUrge = card("Violent Urge") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target creature gets +1/+0 and gains first strike until end of turn.\n" +
        "Delirium — If there are four or more card types among cards in your graveyard, that creature gains double strike until end of turn."
    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(1, 0, t) then
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, t) then
            Effects.If(
                condition = Conditions.Delirium(4),
                then = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, t)
            )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "164"
        artist = "Mirko Failoni"
        flavorText = "Rage can be a potent antidote to fear."
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a47c968b-1edd-45ac-a67c-311647e7e2fc.jpg?1726286465"
    }
}
