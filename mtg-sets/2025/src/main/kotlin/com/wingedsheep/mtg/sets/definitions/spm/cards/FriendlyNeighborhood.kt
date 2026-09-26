package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Friendly Neighborhood
 * {3}{W}
 * Enchantment — Aura
 * Enchant land
 * When this Aura enters, create three 1/1 green and white Human Citizen creature tokens.
 * Enchanted land has "{1}, {T}: Target creature gets +1/+1 until end of turn for each
 * creature you control. Activate only as a sorcery."
 *
 * The pump is granted to the enchanted land as a *separate* activated ability
 * ([GrantActivatedAbility]) — a targeted, sorcery-speed ability whose +N/+N modifier scales
 * with the number of creatures the ability's controller controls (dynamic
 * [DynamicAmount.Count] over the battlefield). Modelled after New Horizons / Lavamancer's
 * Skill (grant an activated ability to the enchanted permanent).
 */
val FriendlyNeighborhood = card("Friendly Neighborhood") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\n" +
        "When this Aura enters, create three 1/1 green and white Human Citizen creature tokens.\n" +
        "Enchanted land has \"{1}, {T}: Target creature gets +1/+1 until end of turn for each " +
        "creature you control. Activate only as a sorcery.\""

    auraTarget = TargetObject(filter = TargetFilter.Land)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN, Color.WHITE),
            creatureTypes = setOf("Human", "Citizen"),
            count = 3
        )
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
                val creature = target(TargetFilter.Creature)
                effect = Effects.ModifyStats(
                    power = DynamicAmounts.count(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature),
                    toughness = DynamicAmounts.count(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature),
                    target = creature
                )
                timing = TimingRule.SorcerySpeed
            }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "8"
        artist = "Pablo Mendoza"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/17a18e2f-221f-4fc2-8dab-25bf12fb8756.jpg?1783905362"
    }
}
