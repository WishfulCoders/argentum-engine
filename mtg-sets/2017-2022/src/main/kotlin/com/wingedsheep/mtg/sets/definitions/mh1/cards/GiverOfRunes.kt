package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Giver of Runes — Modern Horizons #13
 * {W} · Creature — Kor Cleric · 1/2
 *
 * {T}: Another target creature you control gains protection from colorless or from the color of
 * your choice until end of turn.
 *
 * "Another" is [TargetFilter.OtherCreatureYouControl] (Giver can't protect itself, unlike Mother of
 * Runes). The colorless-or-color choice is made on resolution by
 * [Effects.GrantProtectionFromColorlessOrChosenColor], the Angelic Intervention primitive.
 */
val GiverOfRunes = card("Giver of Runes") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Kor Cleric"
    power = 1
    toughness = 2
    oracleText = "{T}: Another target creature you control gains protection from colorless or from the color of your choice until end of turn."

    activatedAbility {
        cost = Costs.Tap
        val t = target(TargetFilter.OtherCreatureYouControl)
        effect = Effects.GrantProtectionFromColorlessOrChosenColor(t)
        description = "{T}: Another target creature you control gains protection from colorless or from the color of your choice until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "13"
        artist = "Seb McKinnon"
        flavorText = "She provides marks of protection to those she chooses as family."
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e117771-5a8b-4812-b487-32ba34b7f724.jpg?1783933162"
    }
}
