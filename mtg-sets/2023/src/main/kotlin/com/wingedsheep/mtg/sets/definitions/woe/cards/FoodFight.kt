package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Targets

/**
 * Food Fight
 * {1}{R}
 * Enchantment
 *
 * Artifacts you control have "{2}, Sacrifice this artifact: It deals damage to any target equal
 * to 1 plus the number of permanents named Food Fight you control."
 */
val FoodFight = card("Food Fight") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Artifacts you control have \"{2}, Sacrifice this artifact: It deals damage to any target " +
        "equal to 1 plus the number of permanents named Food Fight you control.\""

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Composite(Costs.Mana("{2}"), Costs.SacrificeSelf)
                val anyTarget = target(Targets.Any)
                effect = Effects.DealDamage(
                    1 + DynamicAmounts.count(
                        Player.You,
                        Zone.BATTLEFIELD,
                        GameObjectFilter.Any.named("Food Fight")
                    ),
                    anyTarget,
                    damageSource = EffectTarget.Self
                )
            },
            filter = GroupFilter(GameObjectFilter.Artifact.youControl())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "129"
        artist = "Filipe Pagliuso"
        flavorText = "With no time to gather weapons, the dwarves fought the redcaps with anything within reach."
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a7cc43c-6e8c-41d2-a885-24604dfc7e7f.jpg"
    }
}
