package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Retreat to Hagra
 * {2}{B}
 * Enchantment
 * Landfall — Whenever a land you control enters, choose one —
 * • Target creature gets +1/+0 and gains deathtouch until end of turn.
 * • Each opponent loses 1 life and you gain 1 life.
 *
 * A modal *triggered* ability: the mode is chosen as the ability goes on the stack (CR 603.3c),
 * so the target is locked in before any land-drop response.
 */
val RetreatToHagra = card("Retreat to Hagra") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Landfall — Whenever a land you control enters, choose one —\n" +
        "• Target creature gets +1/+0 and gains deathtouch until end of turn.\n" +
        "• Each opponent loses 1 life and you gain 1 life."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = ModalEffect.chooseOne(
            mode("Target creature gets +1/+0 and gains deathtouch until end of turn") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.ModifyStats(1, 0, creature) then
                    Effects.GrantKeyword(Keyword.DEATHTOUCH, creature)
            },
            Mode.noTarget(
                Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(1),
                "Each opponent loses 1 life and you gain 1 life",
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "121"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/4/8/48505c90-ee73-40ad-b774-a6f4bfffff4b.jpg?1783938199"
    }
}
