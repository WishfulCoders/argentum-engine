package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Xiahou Dun, the One-Eyed
 * {2}{B}{B}
 * Legendary Creature — Human Soldier
 * 3/2
 * Horsemanship
 * Sacrifice Xiahou Dun: Return target black card from your graveyard to your hand.
 * Activate only during your turn, before attackers are declared.
 */
val XiahouDunTheOneEyed = card("Xiahou Dun, the One-Eyed") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Soldier"
    power = 3
    toughness = 2
    oracleText = "Horsemanship (This creature can't be blocked except by creatures with horsemanship.)\nSacrifice Xiahou Dun: Return target black card from your graveyard to your hand. Activate only during your turn, before attackers are declared."

    keywordAbility(KeywordAbility.Simple(Keyword.HORSEMANSHIP))

    activatedAbility {
        cost = Costs.SacrificeSelf
        restrictions = listOf(
            ActivationRestriction.OnlyDuringYourTurn,
            ActivationRestriction.BeforeStep(Step.DECLARE_ATTACKERS)
        )
        val card = target(TargetFilter(GameObjectFilter.Any.withColor(Color.BLACK).ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.ReturnToHand(card)
        description = "Sacrifice Xiahou Dun: Return target black card from your graveyard to your hand. Activate only during your turn, before attackers are declared."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "92"
        artist = "Junko Taguchi"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/91bed8a9-ede6-4fdc-966e-d1e4261c68e4.jpg?1783946111"
    }
}
