package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Obscura Ascendancy
 * {W}{U}{B}
 * Enchantment
 *
 * Whenever you cast a spell, if its mana value is equal to 1 plus the number of soul counters on
 * this enchantment, put a soul counter on this enchantment, then create a 2/2 white Spirit
 * creature token with flying.
 * As long as there are five or more soul counters on this enchantment, Spirits you control get +3/+3.
 *
 * Modeling notes:
 *  - The "if" is an intervening-if (CR 603.4, 2022-04-29 ruling): checked when the spell is cast
 *    and again as the ability resolves, so two mana-value-1 spells cast back to back yield only one
 *    counter and one token — the second trigger fails its re-check once the first adds a counter.
 *  - "Its mana value" reads [DynamicAmounts.triggeringSpellManaValue], the mana value captured on
 *    the cast event (X included, CR 202.3e). It stays right at resolution even if the spell has
 *    since been countered or resolved and its card no longer counts X.
 */
val ObscuraAscendancy = card("Obscura Ascendancy") {
    manaCost = "{W}{U}{B}"
    colorIdentity = "WUB"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast a spell, if its mana value is equal to 1 plus the number of soul " +
        "counters on this enchantment, put a soul counter on this enchantment, then create a 2/2 " +
        "white Spirit creature token with flying.\n" +
        "As long as there are five or more soul counters on this enchantment, Spirits you control get +3/+3."

    triggeredAbility {
        trigger = Triggers.you.casts()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.triggeringSpellManaValue(),
            ComparisonOperator.EQ,
            DynamicAmounts.countersOnSelf(CounterType.SOUL) + 1
        )
        effect = Effects.AddCounters(CounterType.SOUL, 1, EffectTarget.Self) then
            Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Spirit"),
                keywords = setOf(Keyword.FLYING),
                imageUri = "https://cards.scryfall.io/normal/front/0/a/0a7a7f7c-29e3-4b98-ad2f-2d585ec7610e.jpg?1783922963"
            )
        description = "Whenever you cast a spell, if its mana value is equal to 1 plus the number of " +
            "soul counters on this enchantment, put a soul counter on this enchantment, then create a " +
            "2/2 white Spirit creature token with flying."
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(
                powerBonus = 3,
                toughnessBonus = 3,
                filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Spirit").youControl())
            ),
            condition = Conditions.CompareAmounts(
                DynamicAmounts.countersOnSelf(CounterType.SOUL),
                ComparisonOperator.GTE,
                5
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "207"
        artist = "Igor Krstic"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98a11e5e-d2a9-4490-90f3-a643fc7ed7c3.jpg?1783923077"
        ruling("2022-04-29", "If there are no soul counters on Obscura Ascendancy, its ability triggers if you cast a spell with mana value 1. If there is one counter, its ability triggers if you cast a spell with mana value 2, and so on.")
        ruling("2022-04-29", "Obscura Ascendancy's triggered ability has an “intervening ‘if'” clause. This means that it checks the number of counters both when the ability triggers and when it resolves. Notably, this means that casting two instants in a row with mana value 1 will never cause a player to create two tokens or put two counters on Obscura Ascendancy.")
    }
}
