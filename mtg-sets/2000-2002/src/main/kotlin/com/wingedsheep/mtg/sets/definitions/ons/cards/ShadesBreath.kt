package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.Duration

/**
 * Shade's Breath
 * {1}{B}
 * Instant
 * Until end of turn, each creature you control becomes a black Shade and gains
 * "{B}: This creature gets +1/+1 until end of turn."
 */
val ShadesBreath = card("Shade's Breath") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"

    spell {
        effect = Effects.SetGroupCreatureSubtypes(
            subtypes = setOf("Shade")
        ) then Effects.ChangeGroupColor(
            colors = setOf(Color.BLACK)
        ) then Effects.GrantActivatedAbilityToGroup(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Mana(ManaCost.parse("{B}")),
                effect = Effects.ModifyStats(
                    power = 1,
                    toughness = 1,
                    target = EffectTarget.Self,
                    duration = Duration.EndOfTurn
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "167"
        artist = "Franz Vohwinkel"
        flavorText = "The grim business of the Coliseum casts long shadows."
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a37be9a8-ef69-4c62-8455-e129e62fe69a.jpg?1562933592"
    }
}
