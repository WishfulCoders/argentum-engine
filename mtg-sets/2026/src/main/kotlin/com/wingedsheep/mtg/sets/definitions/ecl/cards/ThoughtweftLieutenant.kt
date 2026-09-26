package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Thoughtweft Lieutenant
 * {G}{W}
 * Creature — Kithkin Soldier
 * 2/2
 *
 * Whenever this creature or another Kithkin you control enters, target creature you control gets
 * +1/+1 and gains trample until end of turn.
 */
val ThoughtweftLieutenant = card("Thoughtweft Lieutenant") {
    manaCost = "{G}{W}"
    colorIdentity = "WG"
    typeLine = "Creature — Kithkin Soldier"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature or another Kithkin you control enters, target creature " +
        "you control gets +1/+1 and gains trample until end of turn."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.youControl().withSubtype("Kithkin")).enters()
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.ModifyStats(1, 1, creature) then Effects.GrantKeyword(Keyword.TRAMPLE, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "246"
        artist = "Matt Stewart"
        flavorText = "He draws the threads of the thoughtweft tighter, turning it from cloth to armor."
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c54ec67-9317-455e-a045-fa4ed9cb676f.jpg?1767658549"
    }
}
