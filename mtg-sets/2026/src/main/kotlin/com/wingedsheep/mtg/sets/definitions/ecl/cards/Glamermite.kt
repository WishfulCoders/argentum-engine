package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val Glamermite = card("Glamermite") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Faerie Rogue"
    power = 2
    toughness = 2
    oracleText = "Flash\nFlying\nWhen this creature enters, choose one —\n• Tap target creature.\n• Untap target creature."

    keywords(Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            mode("Tap target creature") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.Tap(creature)
            },
            mode("Untap target creature") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.Untap(creature)
            }
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "50"
        artist = "Pauline Voss"
        flavorText = "\"And watch, my queen, he'll do as I please!\""
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b8b7c23a-0034-453c-ab44-f6ec0f31d1eb.jpg?1767956988"
    }
}
