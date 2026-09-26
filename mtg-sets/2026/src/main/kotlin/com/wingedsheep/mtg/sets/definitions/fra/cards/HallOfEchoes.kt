package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LegendRuleDoesNotApplyTo
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hall of Echoes — Reality Fracture #179
 * Land
 *
 * {T}: Add {C}.
 * {5}: This land becomes a copy of target creature you control until end of turn. The "legend
 * rule" doesn't apply to permanents you control this turn.
 *
 * The copy is the single-permanent shape of [Effects.EachPermanentBecomesCopyOfTarget]
 * (`affected = Self`, the Deepfathom Echo spelling) with `Duration.EndOfTurn`: the land takes the
 * creature's copiable values only (CR 707.2), so it stops being a land and loses both of its own
 * abilities until the cleanup step reverts it — no mana ability, and no way to re-activate the {5}
 * while it is a copy. Counters, tapped state and damage are its own, and it can attack if it has
 * been under your control since the turn began.
 *
 * The legend-rule clause is a rule about the player, not about the land, so it is granted to the
 * controller: a player-anchored `GrantStaticAbility(LegendRuleDoesNotApplyTo(Permanent),
 * Controller, EndOfTurn)` that `LegendRuleCheck` reads alongside printed exemptions. It lasts the
 * rest of the turn even if Hall of Echoes leaves the battlefield, which is what lets the copy of a
 * legendary creature live beside the original.
 */
val HallOfEchoes = card("Hall of Echoes") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{5}: This land becomes a copy of target creature you control until end of turn. The " +
        "\"legend rule\" doesn't apply to permanents you control this turn."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{5}")
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.EachPermanentBecomesCopyOfTarget(
            target = creature,
            affected = EffectTarget.Self,
            duration = Duration.EndOfTurn,
        ) then
            Effects.GrantStaticAbility(
                ability = LegendRuleDoesNotApplyTo(GameObjectFilter.Permanent),
                target = EffectTarget.Controller,
                duration = Duration.EndOfTurn,
            )
        description = "{5}: This land becomes a copy of target creature you control until end of " +
            "turn. The \"legend rule\" doesn't apply to permanents you control this turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "179"
        artist = "Leon Tukker"
        flavorText = "The tunnels below Hexhaven murmur with ripples of first breaths."
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a771010-b397-4849-ac9b-08e4dd5d6a72.jpg?1789644859"
        inBooster = false
    }
}
