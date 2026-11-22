package com.wingedsheep.mtg.sets.definitions.dst.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Skullclamp — Darksteel #140 (canonical printing)
 * {1} · Artifact — Equipment
 *
 * Equipped creature gets +1/-1.
 * Whenever equipped creature dies, draw two cards.
 * Equip {1}
 */
val Skullclamp = card("Skullclamp") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/-1.\nWhenever equipped creature dies, draw two cards.\nEquip {1}"

    staticAbility {
        ability = ModifyStats(+1, -1, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.DrawCards(2)
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "140"
        artist = "Luca Zontini"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/55318397-de3c-47ea-a088-72a24df5c8fa.jpg?1783944419"
    }
}
