package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * "For each opponent, … up to one target creature that player controls" is Kaya, Spirits'
 * Justice's one-per-player distribution: up to one target per opponent, no two sharing a
 * controller.
 */
val HapatraTheDesertFrost = card("Hapatra, the Desert Frost") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Wizard"
    power = 4
    toughness = 3
    oracleText = "When Hapatra enters, for each opponent, tap up to one target creature that player controls. " +
        "Put a stun counter on each of those creatures. (If a permanent with a stun counter would become " +
        "untapped, remove one from it instead.)\n" +
        "{2}{U}: Untap target creature."

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(
            TargetFilter.CreatureOpponentControls,
            optional = true,
            dynamicMaxCount = DynamicAmounts.playerCount(Player.EachOpponent),
            differentControllers = true,
        )
        effect = Effects.ForEachTarget(
            Effects.Tap(EffectTarget.ContextTarget(0)),
            Effects.AddCounters(CounterType.STUN, 1, EffectTarget.ContextTarget(0))
        )
        description = "When Hapatra enters, for each opponent, tap up to one target creature that player " +
            "controls. Put a stun counter on each of those creatures."
    }

    activatedAbility {
        cost = Costs.Mana("{2}{U}")
        val creature = target(TargetFilter.Creature)
        effect = Effects.Untap(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "215"
        artist = "Jodie Muir"
        flavorText = "\"Kefnet always told me to apply my knowledge. I merely chose to apply it in usurping him.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/5/85faaa9d-4656-4365-871d-7cba53ed0996.jpg?1789387113"
        inBooster = false
    }
}
