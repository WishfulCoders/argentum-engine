package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rakish Crew — Outlaws of Thunder Junction #99
 * {2}{B} · Enchantment · Uncommon
 *
 * When this enchantment enters, create a 1/1 red Mercenary creature token with "{T}: Target
 * creature you control gets +1/+0 until end of turn. Activate only as a sorcery."
 * Whenever an outlaw you control dies, each opponent loses 1 life and you gain 1 life.
 *
 * The token is the standard OTJ Mercenary token (same as Prickly Pair / Mourner's Surprise).
 * The death payoff is an ANY-binding battlefield→graveyard trigger filtered to outlaw
 * creatures you control ([Filters.OutlawCreature] = the Assassin/Mercenary/Pirate/Rogue/Warlock
 * subtype group), draining each opponent 1 and gaining you 1 — it fires once per outlaw that
 * dies.
 */
val RakishCrew = card("Rakish Crew") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, create a 1/1 red Mercenary creature token with " +
        "\"{T}: Target creature you control gets +1/+0 until end of turn. Activate only as a sorcery.\"\n" +
        "Whenever an outlaw you control dies, each opponent loses 1 life and you gain 1 life. " +
        "(Assassins, Mercenaries, Pirates, Rogues, and Warlocks are outlaws.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Mercenary"),
            activatedAbilities = listOf(
                grantedActivatedAbility {
                    cost = AbilityCost.Tap
                    val creatureYouControl = target(TargetFilter.CreatureYouControl)
                    effect = Effects.ModifyStats(1, 0, creatureYouControl)
                    timing = TimingRule.SorcerySpeed
                }
            ),
            imageUri = "https://cards.scryfall.io/normal/front/5/f/5f04607f-eed2-462e-897f-82e41e5f7049.jpg?1712316319"
        )
        description = "create a 1/1 red Mercenary creature token with \"{T}: Target creature " +
            "you control gets +1/+0 until end of turn. Activate only as a sorcery.\""
    }

    triggeredAbility {
        trigger = Triggers.a(Filters.OutlawCreature.youControl()).dies()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.GainLife(1, EffectTarget.Controller)
        description = "Whenever an outlaw you control dies, each opponent loses 1 life and you gain 1 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "99"
        artist = "Ilse Gort"
        imageUri = "https://cards.scryfall.io/normal/front/7/0/70f64358-58db-40ab-90e8-6137c3bd0a29.jpg?1712355636"
    }
}
