package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val FateshaperAspirant = card("Fateshaper Aspirant") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Rhino Cleric"
    power = 3
    toughness = 4
    oracleText = "When this creature enters, choose one —\n" +
        "• Return target legendary card from your graveyard to your hand.\n" +
        "• Put a +1/+1 counter on target creature. It gains vigilance and indestructible until end of turn. " +
        "(Damage and effects that say \"destroy\" don't destroy it.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            // "Legendary card" is any card type — not narrowed to permanents or creatures.
            mode("Return target legendary card from your graveyard to your hand.") {
                val cardInGraveyard = target(TargetFilter.CardInGraveyard.legendary().ownedByYou())
                effect = Effects.Move(cardInGraveyard, Zone.HAND)
            },
            mode("Put a +1/+1 counter on target creature. It gains vigilance and indestructible until end of turn.") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
                    Effects.GrantKeyword(Keyword.VIGILANCE, creature) then
                    Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, creature)
            }
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "6"
        artist = "Paul Dainton"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0c8400d-824f-4d79-84bc-7615a0deb831.jpg?1789556683"
        inBooster = false
    }
}
