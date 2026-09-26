package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Psychic Trance
 * {2}{U}{U}
 * Instant
 * Until end of turn, Wizards you control gain "{T}: Counter target spell."
 */
val PsychicTrance = card("Psychic Trance") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Until end of turn, Wizards you control gain \"{T}: Counter target spell.\""

    spell {
        effect = Effects.GrantActivatedAbilityToGroup(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = AbilityCost.Tap,
                effect = Effects.CounterSpell(),
                targetRequirement = TargetObject(filter = TargetFilter.SpellOnStack)
            ),
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Wizard").youControl()),
            duration = Duration.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "102"
        artist = "Rebecca Guay"
        flavorText = "The Riptide Project may be the only school devoted to preventing the spread of knowledge."
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5e55695-16cc-4373-8078-959f1ded4c6d.jpg?1562945989"
    }
}
