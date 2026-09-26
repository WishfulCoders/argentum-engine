package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.Effects

/**
 * Clement, the Worrywort
 * {1}{G}{U}
 * Legendary Creature — Frog Druid
 * 3/3
 *
 * Vigilance
 * Whenever Clement or another creature you control enters, return up to one
 * target creature you control with lesser mana value to its owner's hand.
 * Frogs you control have "{T}: Add {G} or {U}. Spend this mana only to cast
 * a creature spell."
 */
val ClementTheWorrywort = card("Clement, the Worrywort") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "UG"
    typeLine = "Legendary Creature — Frog Druid"
    power = 3
    toughness = 3
    oracleText = "Vigilance\n" +
        "Whenever Clement or another creature you control enters, return up to one target creature " +
        "you control with lesser mana value to its owner's hand.\n" +
        "Frogs you control have \"{T}: Add {G} or {U}. Spend this mana only to cast a creature spell.\""

    keywords(Keyword.VIGILANCE)

    // Whenever Clement or another creature you control enters, return up to one target creature
    // you control with lesser mana value to its owner's hand. The target is chosen as the trigger
    // goes on the stack (CR 603.3d), capped below the entering creature's mana value; the entering
    // creature itself can never qualify, and a creature with shroud can't be chosen.
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).enters()
        val creature = target(
            TargetFilter.CreatureYouControl.manaValueAtMostDynamic(DynamicAmounts.triggeringManaValue() - 1),
            optional = true,
        )
        effect = Effects.ReturnToHand(creature)
    }

    // Frogs you control have "{T}: Add {G} or {U}. Spend this mana only to cast a creature spell."
    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Tap,
                effect = Effects.AddManaInAnyCombination(
                    amount = 1,
                    allowedColors = setOf(Color.GREEN, Color.BLUE),
                    restriction = ManaRestriction.CreatureSpellsOnly
                ),
                isManaAbility = true,
                timing = TimingRule.ManaAbility,
                descriptionOverride = "{T}: Add {G} or {U}. Spend this mana only to cast a creature spell."
            ),
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Frog").youControl())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "209"
        artist = "Ekaterina Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/7/0/7028130c-c91d-4bf7-b0b0-450f71107d7a.jpg?1721427029"
    }
}
