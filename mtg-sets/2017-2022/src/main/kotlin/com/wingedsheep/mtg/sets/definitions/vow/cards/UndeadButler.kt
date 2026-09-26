package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Undead Butler
 * {1}{B}
 * Creature — Zombie
 * 1/2
 *
 * When this creature enters, mill three cards.
 * When this creature dies, you may exile it. When you do, return target creature card from your
 * graveyard to your hand.
 */
val UndeadButler = card("Undead Butler") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 1
    toughness = 2
    oracleText = "When this creature enters, mill three cards. (Put the top three cards of your " +
        "library into your graveyard.)\n" +
        "When this creature dies, you may exile it. When you do, return target creature card from " +
        "your graveyard to your hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.mill(3)
    }

    // Dies trigger functions from the graveyard so "exile it" can reference Self. Exiling is
    // optional ("you may"); once it happens the reflexive trigger returns a targeted creature card.
    triggeredAbility {
        trigger = Triggers.self.dies()
        triggerZone = Zone.GRAVEYARD
        effect = Effects.ReflexiveTrigger(
            action = Effects.Exile(EffectTarget.Self),
            optional = true,
            descriptionOverride = "You may exile this creature. When you do, return target " +
                "creature card from your graveyard to your hand."
        ) {
            val creatureInYourGraveyard = target(TargetFilter.CreatureInYourGraveyard)
            effect = Effects.Move(creatureInYourGraveyard, Zone.HAND)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "133"
        artist = "Néstor Ossandón Leal"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c9b8582-8887-4652-82e2-f9b11ee21545.jpg?1782703094"
    }
}
