package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
/**
 * Molecular Modifier
 * {2}{R}
 * Creature — Kavu Artificer
 * At the beginning of combat on your turn, target creature you control gets +1/+0 and gains first strike until end of turn.
 * 2/2
 */
val MolecularModifier = card("Molecular Modifier") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Kavu Artificer"
    power = 2
    toughness = 2
    oracleText = "At the beginning of combat on your turn, target creature you control gets +1/+0 and gains first strike until end of turn."

    // At the beginning of combat on your turn, target creature you control gets +1/+0 and gains first strike until end of turn
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.ModifyStats(+1, 0, creature) then
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "146"
        artist = "Konstantin Porubov"
        flavorText = "\"Let's show those bugs our own style of splicin'.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b80b9c9-a871-4c04-b8be-feb81a900591.jpg?1753683209"
    }
}
