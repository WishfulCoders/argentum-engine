package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val DiviningDuelist = card("Divining Duelist") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    power = 3
    toughness = 2
    oracleText = "Flash\nWhen this creature enters, choose one —\n• Tap target creature.\n• Untap target creature.\n• Draw a card, then discard a card."

    keywords(Keyword.FLASH)
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            mode("Tap target creature.") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.Tap(creature)
            },
            mode("Untap target creature.") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.Untap(creature)
            },
            Mode(effect = Effects.DrawCards(1) then Effects.Discard(1), description = "Draw a card, then discard a card.")
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "29"
        artist = "David Auden Nash"
        flavorText = "\"I predicted your quips yesterday, Stingerquill. They weren't clever then either.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/6/960c7335-331d-488b-be68-2ad1c1c695dc.jpg?1789556709"
        inBooster = false
    }
}
