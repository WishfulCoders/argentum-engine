package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lay Down Arms — The Brothers' War #11
 * {W} · Sorcery · Uncommon
 * Artist: Liiga Smilshkalne
 *
 * Exile target creature with mana value less than or equal to the number of Plains you control.
 * Its controller gains 3 life.
 *
 * The mana-value cap is part of the *targeting* restriction, so it rides on the target's filter via
 * [GameObjectFilter.manaValueAtMostDynamic] (Dominating Vampire shape). That makes the Plains count
 * re-read both at cast time and on the CR 608.2b resolution re-check: lose a Plains in response and
 * the creature may no longer be a legal target, so the spell does nothing (no exile, no life). Counts
 * any land with the Plains subtype, not just basics. The life goes to the exiled creature's
 * controller via [EffectTarget.TargetController] after the exile (Inevitable Defeat shape).
 */
val LayDownArms = card("Lay Down Arms") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Exile target creature with mana value less than or equal to the number of Plains " +
        "you control. Its controller gains 3 life."

    spell {
        val creature = target(
            TargetFilter(
                GameObjectFilter.Creature.manaValueAtMostDynamic(
                    DynamicAmounts.battlefield(
                        Player.You,
                        GameObjectFilter.Land.withSubtype(Subtype.PLAINS)
                    ).count()
                )
            ),
        )
        effect = Effects.Exile(creature) then
            Effects.GainLife(3, EffectTarget.TargetController)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "11"
        artist = "Liiga Smilshkalne"
        flavorText = "She'll swing a blade all her life, but never again for war."
        imageUri = "https://cards.scryfall.io/normal/front/5/6/5649ee5a-4485-40a2-96d1-2e061905a71e.jpg?1783920130"
    }
}
