package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * General's Enforcer — Ikoria: Lair of Behemoths #188 (canonical printing)
 * {W}{B} · Creature — Human Soldier · 2/3
 *
 * Legendary Humans you control have indestructible.
 * {2}{W}{B}: Exile target card from a graveyard. If it was a creature card, create a 1/1 white
 * Human Soldier creature token.
 *
 * "Legendary Humans" is the bare tribal noun (a permanent filter); only creatures are Humans, so
 * it covers legendary Human creatures — including the Enforcer itself if it ever became
 * legendary. The activated ability is the Scavenging Ooze shape: exile, then gate the payoff on
 * the exiled card's printed type.
 */
val GeneralsEnforcer = card("General's Enforcer") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 3
    oracleText = "Legendary Humans you control have indestructible.\n" +
        "{2}{W}{B}: Exile target card from a graveyard. If it was a creature card, " +
        "create a 1/1 white Human Soldier creature token."

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.INDESTRUCTIBLE,
            filter = GroupFilter(GameObjectFilter.Permanent.legendary().youControl().withSubtype(Subtype.HUMAN)),
        )
    }

    activatedAbility {
        cost = Costs.Mana(ManaCost.parse("{2}{W}{B}"))
        val exiled = target(TargetFilter.CardInGraveyard)
        effect = Effects.Exile(exiled) then
            Effects.If(
                condition = Conditions.TargetIsCreatureCard(0),
                then = Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.WHITE),
                    creatureTypes = setOf("Human", "Soldier"),
                    imageUri = "https://cards.scryfall.io/normal/front/9/0/90de6d1e-e654-4f4a-8014-061ce69e540f.jpg?1783930948",
                ),
            )
        description = "{2}{W}{B}: Exile target card from a graveyard. If it was a creature card, " +
            "create a 1/1 white Human Soldier creature token."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "188"
        artist = "Wisnu Tan"
        flavorText = "\"More stubborn than Drannith's walls.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6a45d0e-582a-4ec1-b83b-71b6a0ff6249.jpg?1783931024"
        ruling("2020-04-17", "Because damage remains marked on a creature until the damage is removed as the turn ends, nonlethal damage dealt to a legendary Human you control may become lethal if General's Enforcer leaves the battlefield during that turn.")
        ruling("2020-04-17", "Only creatures are Humans. Planeswalkers such as Lukka and Narset who represent human characters aren't affected by General's Enforcer.")
    }
}
