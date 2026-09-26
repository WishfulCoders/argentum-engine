package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mausoleum Wanderer
 * {U}
 * Creature — Spirit
 * 1/1
 *
 * Flying
 * Whenever another Spirit you control enters, this creature gets +1/+1 until end of turn.
 * Sacrifice this creature: Counter target instant or sorcery spell unless its controller pays {X},
 * where X is this creature's power.
 *
 * X reads the source's power through [EffectTarget.Self], which resolves with last-known
 * information (CR 112.7a / 608.2h): the sacrifice-self cost has already moved the Wanderer to the
 * graveyard by the time the ability resolves, so the engine's pre-sacrifice snapshot supplies the
 * *pumped* power rather than the printed 1. That is the whole point of the card — flash in a Spirit,
 * then trade the Wanderer for a bigger tax.
 */
val MausoleumWanderer = card("Mausoleum Wanderer") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "Whenever another Spirit you control enters, this creature gets +1/+1 until end of turn.\n" +
        "Sacrifice this creature: Counter target instant or sorcery spell unless its controller " +
        "pays {X}, where X is this creature's power."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl().withSubtype(Subtype.SPIRIT)).enters()
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
        description = "Whenever another Spirit you control enters, this creature gets +1/+1 until end of turn."
    }

    activatedAbility {
        val instantOrSorcerySpell = target(TargetFilter.InstantOrSorcerySpellOnStack)
        cost = Costs.SacrificeSelf
        effect = Effects.CounterUnlessDynamicPays(
            DynamicAmounts.sourcePower()
        )
        description = "Counter target instant or sorcery spell unless its controller pays {X}, " +
            "where X is this creature's power."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "69"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42391fa7-6172-487c-b8aa-d41ab7c64973.jpg?1783937494"
    }
}
