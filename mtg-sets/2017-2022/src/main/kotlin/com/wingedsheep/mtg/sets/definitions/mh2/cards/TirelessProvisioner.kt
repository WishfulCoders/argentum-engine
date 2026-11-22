package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode

/**
 * Tireless Provisioner — Modern Horizons 2 #180 (canonical printing)
 * {2}{G} · Creature — Elf Scout · 3/2
 *
 * Landfall — Whenever a land you control enters, create a Food token or a Treasure token.
 *
 * The landfall trigger of Zendikar's Roil carrying Ant-Man's Army's resolution-time token choice:
 * a [ModalEffect.chooseOne] with `countsAsModalSpell = false`, since "a Food token or a Treasure
 * token" is not a printed "choose one —".
 */
val TirelessProvisioner = card("Tireless Provisioner") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Scout"
    power = 3
    toughness = 2
    oracleText = "Landfall — Whenever a land you control enters, create a Food token or a Treasure token. " +
        "(Food is an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\" " +
        "Treasure is an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = ModalEffect.chooseOne(
            Mode.noTarget(Effects.CreateFood(), "Create a Food token"),
            Mode.noTarget(Effects.CreateTreasure(), "Create a Treasure token"),
            countsAsModalSpell = false
        )
        description = "Landfall — Whenever a land you control enters, create a Food token or a Treasure token."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "180"
        artist = "Josu Hernaiz"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c5be54d-660e-42ab-b5ea-5e1cf3bad0bc.jpg?1783926824"
    }
}
