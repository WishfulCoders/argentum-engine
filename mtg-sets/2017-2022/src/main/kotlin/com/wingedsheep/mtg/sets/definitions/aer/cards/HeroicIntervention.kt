package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Heroic Intervention — Aether Revolt #109 (canonical printing)
 * {1}{G} · Instant
 *
 * Permanents you control gain hexproof and indestructible until end of turn.
 *
 * The affected set is locked in as the spell resolves (CR 611.2c): [Effects.ForEachInGroup] grants
 * each keyword to the permanents you control right then, so permanents you gain control of later in
 * the turn get nothing.
 */
val HeroicIntervention = card("Heroic Intervention") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Permanents you control gain hexproof and indestructible until end of turn."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Permanent.youControl()),
            Effects.GrantKeyword(Keyword.HEXPROOF, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "109"
        artist = "James Ryman"
        flavorText = "\"Wherever the strong would harm the weak, I will be there.\"\n—Ajani Goldmane"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f5a620c-fde7-4b72-bf8a-efc4f14560c5.jpg?1783936743"
        ruling(
            "2020-06-23",
            "The set of permanents affected by Heroic Intervention is determined as the spell resolves. " +
                "Permanents you begin to control later in the turn won't gain hexproof and indestructible."
        )
    }
}
