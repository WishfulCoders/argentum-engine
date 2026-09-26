package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Triggers

val MaiScornfulStriker = card("Mai, Scornful Striker") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Noble Ally"
    oracleText = "First strike\nWhenever a player casts a noncreature spell, they lose 2 life."
    power = 2
    toughness = 2

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.anyPlayer.casts(GameObjectFilter.Noncreature)
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "109"
        artist = "Hori Airi"
        flavorText = "\"You miscalculated. I love Zuko more than I fear you.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/4/74dd4c0e-27b8-4c47-b7a6-a281413cd6b4.jpg?1764120754"
    }
}
