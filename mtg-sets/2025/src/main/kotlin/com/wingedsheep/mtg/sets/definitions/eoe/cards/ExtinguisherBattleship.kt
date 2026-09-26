package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.station

/**
 * Extinguisher Battleship
 * {8}
 * Artifact — Spacecraft
 * When this Spacecraft enters, destroy target noncreature permanent. Then this Spacecraft deals 4 damage to each creature.
 * Station (Tap another creature you control: Put charge counters equal to its power on this Spacecraft. Station only as a sorcery. It's an artifact creature at 5+.)
 * 5+ | Flying, trample
 * 10/10
 */
val ExtinguisherBattleship = card("Extinguisher Battleship") {
    manaCost = "{8}"
    colorIdentity = ""
    typeLine = "Artifact — Spacecraft"
    power = 10
    toughness = 10
    oracleText = "When this Spacecraft enters, destroy target noncreature permanent. Then this Spacecraft deals 4 damage to each creature.\nStation (Tap another creature you control: Put charge counters equal to its power on this Spacecraft. Station only as a sorcery. It's an artifact creature at 5+.)\n5+ | Flying, trample"

    // ETB: destroy target noncreature permanent, then deal 4 damage to each creature
    triggeredAbility {
        trigger = Triggers.self.enters()
        val target = target(TargetFilter.NoncreaturePermanent)
        effect = Effects.Destroy(target) then
            Effects.ForEachInGroup(
                filter = GroupFilter.AllCreatures,
                effect = Effects.DealDamage(4, EffectTarget.IterationEntity)
            )
        description = "When this Spacecraft enters, destroy target noncreature permanent. Then this Spacecraft deals 4 damage to each creature."
    }

    // Station activated ability: tap another creature → add charge counters equal to its power
    station()

    // Station threshold: 5+ charge counters
    val charge5 = Conditions.SourceCounterCountAtLeast(CounterType.CHARGE, 5)

    staticAbility {
        condition = charge5
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }

    staticAbility {
        condition = charge5
        ability = GrantKeyword(Keyword.FLYING.name, GroupFilter.source())
    }

    staticAbility {
        condition = charge5
        ability = GrantKeyword(Keyword.TRAMPLE.name, GroupFilter.source())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "242"
        artist = "Danny Schwartz"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/5541cdd2-84a6-4667-83eb-fffbe5b3cd3d.jpg?1755341322"
    }
}
