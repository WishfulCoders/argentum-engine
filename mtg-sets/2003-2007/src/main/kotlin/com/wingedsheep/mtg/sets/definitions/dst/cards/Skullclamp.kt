package com.wingedsheep.mtg.sets.definitions.dst.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Skullclamp
 * {1}
 * Artifact — Equipment
 * Equipped creature gets +1/-1.
 * Whenever equipped creature dies, draw two cards.
 * Equip {1}
 *
 * The draw is the Equipment controller's ("you"). A 1-toughness creature dies to the state-based
 * check the moment Skullclamp attaches, and the dies trigger still fires: it looks back at the
 * creature as it last existed on the battlefield, equipped (CR 603.10a).
 */
val Skullclamp = card("Skullclamp") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/-1.\n" +
        "Whenever equipped creature dies, draw two cards.\n" +
        "Equip {1}"

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
