package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Longstalk Brawl
 * {G}
 * Sorcery
 *
 * Gift a tapped Fish (You may promise an opponent a gift as you cast this spell.
 * If you do, they create a tapped 1/1 blue Fish creature token before its other effects.)
 *
 * Choose target creature you control and target creature you don't control.
 * Put a +1/+1 counter on the creature you control if the gift was promised.
 * Then those creatures fight each other.
 *
 * Gift modeled as modal:
 * Mode 1 (no gift): fight
 * Mode 2 (gift): opponent gets tapped Fish token, +1/+1 counter on your creature, then fight
 */
val LongstalkBrawl = card("Longstalk Brawl") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Gift a tapped Fish (You may promise an opponent a gift as you cast this spell. If you do, they create a tapped 1/1 blue Fish creature token before its other effects.)\nChoose target creature you control and target creature you don't control. Put a +1/+1 counter on the creature you control if the gift was promised. Then those creatures fight each other."

    spell {
        effect = Patterns.Mechanic.giftSpell(
            // Mode 1: No gift — just fight
            mode("Don't promise a gift — target creature you control fights target creature you don't control") {
                val creatureYouControl = target(TargetFilter.CreatureYouControl)
                val creatureOpponentControls = target(TargetFilter.CreatureOpponentControls)
                effect = Effects.Fight(creatureYouControl, creatureOpponentControls)
            },
            // Mode 2: Gift a tapped Fish — token + counter + fight
            mode("Promise a gift — opponent creates a tapped 1/1 blue Fish token, +1/+1 counter on your creature, then fight") {
                val creatureYouControl = target(TargetFilter.CreatureYouControl)
                val creatureOpponentControls = target(TargetFilter.CreatureOpponentControls)
                effect = Effects.CreateToken(
                    count = 1,
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.BLUE),
                    creatureTypes = setOf("Fish"),
                    tapped = true,
                    controller = EffectTarget.PlayerRef(Player.ChosenOpponent),
                    imageUri = "https://cards.scryfall.io/normal/front/d/e/de0d6700-49f0-4233-97ba-cef7821c30ed.jpg?1721431109"
                ) then
                    Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creatureYouControl) then
                    Effects.Fight(creatureYouControl, creatureOpponentControls) then
                    Effects.GiftGiven()
            }
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "182"
        artist = "Serena Malyon"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7ef748c-b5e5-4e7d-bf2e-d3e6c08edb42.jpg?1721426861"
    }
}
