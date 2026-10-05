package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

val YuanShaoTheIndecisive = card("Yuan Shao, the Indecisive") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Soldier"
    power = 2
    toughness = 3
    oracleText = "Horsemanship (This creature can't be blocked except by creatures with horsemanship.)\nEach creature you control can't be blocked by more than one creature."

    keywords(Keyword.HORSEMANSHIP)

    // Project the restriction onto every creature, including Yuan Shao himself.
    // The printed CantBeBlockedByMoreThan form is only enforced with source scope.
    staticAbility {
        ability = GrantKeyword(
            AbilityFlag.CANT_BE_BLOCKED_BY_MORE_THAN_ONE.name,
            GroupFilter.AllCreaturesYouControl
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "128"
        artist = "Inoue Junichi"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e9b3794-d0f0-4d28-85cb-22492e195ae1.jpg?1783946103"
    }
}
