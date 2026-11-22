package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Saiba Trespassers — Kamigawa: Neon Dynasty #77 (canonical printing)
 * {4}{U} · Artifact Creature — Moonfolk Rogue · 3/5
 *
 * Channel — {3}{U}, Discard this card: Tap up to two target creatures you don't control. Those
 * creatures don't untap during their controller's next untap step.
 *
 * "Up to two target" is one requirement taking zero to two distinct creatures; each chosen
 * creature is tapped and gets DOESNT_UNTAP keyed to its *own* controller's next untap step
 * (the Tamiyo, Field Researcher −2 pair).
 */
val SaibaTrespassers = card("Saiba Trespassers") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Moonfolk Rogue"
    power = 3
    toughness = 5
    oracleText = "Channel — {3}{U}, Discard this card: Tap up to two target creatures you don't control. " +
        "Those creatures don't untap during their controller's next untap step."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{U}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        targets(TargetFilter.CreatureOpponentControls, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.Tap(EffectTarget.ContextTarget(0)),
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                EffectTarget.ContextTarget(0),
                Duration.UntilAfterAffectedControllersNextUntap()
            )
        )
        description = "Channel — {3}{U}, Discard this card: Tap up to two target creatures you don't control. " +
            "Those creatures don't untap during their controller's next untap step."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "77"
        artist = "Evyn Fong"
        flavorText = "One to clear the way, another to erase their trail."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3dd7f1a-5d43-4218-a9d8-7cc4f6a18ecd.jpg?1783923894"
        ruling("2022-02-18", "Discarding the card is part of the cost to activate a channel ability.")
        ruling("2022-02-18", "If a channel ability requires a target, you may not activate it without a target just to discard the card.")
    }
}
