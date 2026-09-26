package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Triggers

/**
 * Angry Rabble
 * {1}{R}
 * Creature — Human Citizen, 2/2
 * Trample
 * Whenever you cast a spell with mana value 4 or greater, this creature deals 1 damage to each opponent.
 * {5}{R}: Put two +1/+1 counters on this creature. Activate only as a sorcery.
 */
val AngryRabble = card("Angry Rabble") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Citizen"
    power = 2
    toughness = 2
    oracleText = "Trample\nWhenever you cast a spell with mana value 4 or greater, this creature deals 1 damage to each opponent.\n{5}{R}: Put two +1/+1 counters on this creature. Activate only as a sorcery."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.manaValueAtLeast(4))
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    activatedAbility {
        cost = Costs.Mana("{5}{R}")
        timing = TimingRule.SorcerySpeed
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "75"
        artist = "Bartek Fedyczak"
        flavorText = "\"You're a menace and a crook, Spider-Man! Jameson was right!\""
        imageUri = "https://cards.scryfall.io/normal/front/9/3/938730fa-496f-4871-80ec-3e9843ecb219.jpg?1757377232"
    }
}
