package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dragon Mantle
 * {R}
 * Enchantment — Aura
 * Enchant creature
 * When this Aura enters, draw a card.
 * Enchanted creature has "{R}: This creature gets +1/+0 until end of turn."
 *
 * The granted firebreathing is the creature's own ability (as for Ringing Strike Mastery's granted
 * untap), so "this creature" is [EffectTarget.Self] inside it.
 */
val DragonMantle = card("Dragon Mantle") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nWhen this Aura enters, draw a card.\n" +
        "Enchanted creature has \"{R}: This creature gets +1/+0 until end of turn.\""
    auraTarget = Targets.Creature

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.DrawCards(1)
    }
    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.generate(),
                cost = Costs.Mana("{R}"),
                effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "119"
        artist = "Anthony Palumbo"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d97b1080-9001-4751-b2f5-7f56d9f58dff.jpg?1783939764"
        ruling("2020-11-10", "If the target creature is an illegal target by the time Dragon Mantle tries to resolve, it doesn't resolve. It won't enter the battlefield, so its enters-the-battlefield ability won't trigger.")
    }
}
