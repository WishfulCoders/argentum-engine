package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Nezumi Linkbreaker
 * {B}
 * Creature — Rat Warlock
 * 1/1
 *
 * When this creature dies, create a 1/1 red Mercenary creature token with "{T}: Target creature
 * you control gets +1/+0 until end of turn. Activate only as a sorcery."
 */
val NezumiLinkbreaker = card("Nezumi Linkbreaker") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Rat Warlock"
    power = 1
    toughness = 1
    oracleText = "When this creature dies, create a 1/1 red Mercenary creature token with " +
        "\"{T}: Target creature you control gets +1/+0 until end of turn. Activate only as a sorcery.\""

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Mercenary"),
            activatedAbilities = listOf(
                grantedActivatedAbility {
                    cost = AbilityCost.Tap
                    val creatureYouControl = target(TargetFilter.CreatureYouControl)
                    effect = Effects.ModifyStats(1, 0, creatureYouControl)
                    timing = TimingRule.SorcerySpeed
                },
            ),
            imageUri = "https://cards.scryfall.io/normal/front/5/f/5f04607f-eed2-462e-897f-82e41e5f7049.jpg?1712316319",
        )
        description = "When this creature dies, create a 1/1 red Mercenary creature token with " +
            "\"{T}: Target creature you control gets +1/+0 until end of turn. Activate only as a sorcery.\""
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "96"
        artist = "Miro Petrov"
        flavorText = "\"If they want to get to Prosperity so badly, they can walk there.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6dd0095b-6136-4368-94cb-4c82621aaf37.jpg?1712355626"
    }
}
