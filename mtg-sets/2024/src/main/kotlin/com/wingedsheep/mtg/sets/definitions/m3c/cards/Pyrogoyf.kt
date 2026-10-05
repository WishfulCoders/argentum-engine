package com.wingedsheep.mtg.sets.definitions.m3c.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pyrogoyf
 * {3}{R}
 * Creature — Lhurgoyf
 * Power/toughness: star / 1+star
 *
 * Pyrogoyf's power is equal to the number of card types among cards in all graveyards and its
 * toughness is equal to that number plus 1.
 * Whenever this creature or another Lhurgoyf creature you control enters, that creature deals
 * damage equal to its power to any target.
 *
 * The characteristic-defining ability is Tarmogoyf's shape (distinct card types across every
 * graveyard, toughness offset 1). The trigger is split into its two printed halves — "this
 * creature" ([Triggers.self]) and "another Lhurgoyf creature you control" ([Triggers.another]) — so
 * Pyrogoyf still sees itself enter even if it somehow isn't a Lhurgoyf, and never triggers twice
 * for itself. In both halves the entering creature is the damage source and its power the amount,
 * read through last-known information if it has left the battlefield (ruling 2024-06-07).
 */
val Pyrogoyf = card("Pyrogoyf") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Lhurgoyf"
    dynamicStats(
        DynamicAmounts.zone(
            Player.Each,
            Zone.GRAVEYARD,
        ).distinctTypes(),
        toughnessOffset = 1,
    )
    oracleText = "Pyrogoyf's power is equal to the number of card types among cards in all graveyards " +
        "and its toughness is equal to that number plus 1.\nWhenever this creature or another Lhurgoyf " +
        "creature you control enters, that creature deals damage equal to its power to any target."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val any = target(Targets.Any)
        effect = Effects.DealDamage(
            amount = DynamicAmounts.triggeringPower(),
            target = any,
            damageSource = EffectTarget.TriggeringEntity,
        )
        description = "Whenever this creature or another Lhurgoyf creature you control enters, " +
            "that creature deals damage equal to its power to any target."
    }

    triggeredAbility {
        trigger = Triggers.another(
            GameObjectFilter.Creature.withSubtype("Lhurgoyf").youControl(),
        ).enters()
        val any = target(Targets.Any)
        effect = Effects.DealDamage(
            amount = DynamicAmounts.triggeringPower(),
            target = any,
            damageSource = EffectTarget.TriggeringEntity,
        )
        description = "Whenever this creature or another Lhurgoyf creature you control enters, " +
            "that creature deals damage equal to its power to any target."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "59"
        artist = "Xabi Gaztelua"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f60be310-4461-4b84-95f0-b2095108bd79.jpg?1783911420"

        ruling("2024-06-07", "The ability that defines Pyrogoyf's power and toughness works in all zones, not just the battlefield.")
        ruling("2024-06-07", "The ability that defines Pyrogoyf's power and toughness counts card types, not cards. If the only card in all graveyards is a single artifact creature card, Pyrogoyf will be a 2/3. If the cards in all graveyards are ten artifact cards and ten creature cards, Pyrogoyf will still be a 2/3.")
        ruling("2024-06-07", "If Pyrogoyf or the relevant Lhurgoyf creature leaves the battlefield while Pyrogoyf's last ability is on the stack, use its power as it last existed on the battlefield to determine how much damage is dealt.")
    }
}
