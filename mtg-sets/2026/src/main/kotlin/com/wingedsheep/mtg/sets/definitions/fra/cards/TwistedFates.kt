package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val TwistedFates = card("Twisted Fates") {
    manaCost = "{2}{W}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Sorcery"
    oracleText = "Destroy target nonland permanent. Put a +1/+1 counter on each creature target player controls."

    spell {
        val permanent = target(TargetFilter.NonlandPermanent)
        val player = target(Targets.Player)
        effect = Effects.Destroy(permanent) then
            Effects.ForEachInGroup(
                filter = GroupFilter(GameObjectFilter.Creature.targetPlayerControls(player)),
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "158"
        artist = "Lorenzo Mastroianni"
        flavorText = "\"Josu's ashes choke you even now. You failed, while I will have our brother live forever,\" said one Liliana to the other."
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7c0765d-38fd-4d7b-bfb4-49b10ff5939b.jpg?1789614785"
        inBooster = false
    }
}
