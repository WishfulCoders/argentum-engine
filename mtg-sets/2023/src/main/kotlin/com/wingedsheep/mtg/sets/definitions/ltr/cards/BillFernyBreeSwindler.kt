package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bill Ferny, Bree Swindler
 * {1}{U}
 * Legendary Creature — Human Rogue
 * 2/1
 * Whenever Bill Ferny becomes blocked, choose one —
 * • Create a Treasure token.
 * • Target opponent gains control of target Horse you control. If they do, remove Bill Ferny
 *   from combat and create three Treasure tokens.
 */
val BillFernyBreeSwindler = card("Bill Ferny, Bree Swindler") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Rogue"
    power = 2
    toughness = 1
    oracleText = "Whenever Bill Ferny becomes blocked, choose one —\n" +
        "• Create a Treasure token. (It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")\n" +
        "• Target opponent gains control of target Horse you control. If they do, remove Bill Ferny from combat and create three Treasure tokens."

    triggeredAbility {
        trigger = Triggers.self.becomesBlocked()
        effect = ModalEffect.chooseOne(
            // Mode 1: Create a Treasure token.
            Mode.noTarget(
                Effects.CreateTreasure(1),
                "Create a Treasure token"
            ),
            // Mode 2: Target opponent gains control of target Horse you control.
            mode("Target opponent gains control of target Horse you control. If they do, remove Bill Ferny from combat and create three Treasure tokens") {
                val opponent = target(Targets.Opponent)
                val creature = target(TargetFilter(GameObjectFilter.Creature.withSubtype("Horse").youControl()))
                effect = Effects.GiveControl(
                    permanent = creature,
                    newController = opponent
                ) then
                    Effects.RemoveFromCombat(EffectTarget.Self) then
                    Effects.CreateTreasure(3)
            }
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "42"
        artist = "Hristo D. Chukov"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/20ac63cb-fa4d-4340-8062-1029c8bd5ec8.jpg?1686968020"
    }
}
