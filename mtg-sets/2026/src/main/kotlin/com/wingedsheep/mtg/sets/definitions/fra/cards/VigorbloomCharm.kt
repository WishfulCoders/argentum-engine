package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val VigorbloomCharm = card("Vigorbloom Charm") {
    manaCost = "{G}{W}"
    colorIdentity = "GW"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Target permanent you control gains hexproof and indestructible until end of turn.\n" +
        "• You draw a card and gain 3 life.\n" +
        "• Put a +1/+1 counter on target creature you control. Then it fights target creature an opponent controls. (Each deals damage equal to its power to the other.)"

    spell {
        modal(chooseCount = 1) {
            mode("Target permanent you control gains hexproof and indestructible until end of turn") {
                val t = target(TargetFilter.PermanentYouControl)
                effect = Effects.GrantKeyword(Keyword.HEXPROOF, t) then
                    Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, t)
            }
            mode("You draw a card and gain 3 life") {
                effect = Effects.DrawCards(1) then Effects.GainLife(3)
            }
            mode("Put a +1/+1 counter on target creature you control. Then it fights target creature an opponent controls") {
                val yours = target(TargetFilter.CreatureYouControl)
                val theirs = target(TargetFilter.CreatureOpponentControls)
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, yours) then
                    Effects.Fight(yours, theirs)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "160"
        artist = "Kaitlyn McCulley"
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b198e10-b507-4314-a29c-a219f06e48b7.jpg?1789127656"
        inBooster = false
    }
}
