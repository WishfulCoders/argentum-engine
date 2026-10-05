package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AssignCombatDamageAsUnblocked

/**
 * Wolf Pack
 * {6}{G}{G}
 * Creature — Wolf
 * 7/6
 * You may have this creature assign its combat damage as though it weren't blocked.
 */
val WolfPack = card("Wolf Pack") {
    manaCost = "{6}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wolf"
    power = 7
    toughness = 6
    oracleText = "You may have this creature assign its combat damage as though it weren't blocked."

    staticAbility {
        ability = AssignCombatDamageAsUnblocked()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "158"
        artist = "Yang Jun Kwon"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/290f395d-0827-4f62-b117-4766d4ab5dca.jpg?1783946095"
    }
}
