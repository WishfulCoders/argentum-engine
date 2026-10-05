package com.wingedsheep.mtg.sets.definitions.mbs.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardNamePool
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities

/**
 * Phyrexian Revoker — Mirrodin Besieged #122
 * {2} · Artifact Creature — Phyrexian Horror · 2/1 · Rare
 *
 * As this creature enters, choose a nonland card name.
 * Activated abilities of sources with the chosen name can't be activated.
 *
 * The Sorcerous Spyglass shape with two differences: the name pool is [CardNamePool.NONLAND]
 * (as on Skyseer's Chariot), and there is no mana-ability exemption — Revoker stops mana abilities
 * too, so `nonManaAbilitiesOnly = false`. `anyZone = true` covers the ruling that the lock reaches
 * sources in every zone (a cycling ability in hand). Static and triggered abilities are untouched.
 */
val PhyrexianRevoker = card("Phyrexian Revoker") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Phyrexian Horror"
    power = 2
    toughness = 1
    oracleText = "As this creature enters, choose a nonland card name.\n" +
        "Activated abilities of sources with the chosen name can't be activated."

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.CARD_NAME,
            cardNamePool = CardNamePool.NONLAND,
        )
    )

    staticAbility {
        ability = PreventActivatedAbilities(
            filter = GameObjectFilter.Any.namedFromChosenComponent(),
            nonManaAbilitiesOnly = false,
            anyZone = true,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "122"
        artist = "Kev Walker"
        flavorText = "Basic senses like sight and taste are reserved for those in power."
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c7bec21-61b0-4e72-848b-82f38e1910e0.jpg?1783941366"

        ruling(
            "2020-08-07",
            "Activated abilities are written in the form \"Cost: Effect.\" Some keywords are activated " +
                "abilities (such as equip) and will have colons in their reminder texts. Static and " +
                "triggered abilities of sources with the chosen name are unaffected."
        )
        ruling(
            "2020-08-07",
            "Phyrexian Revoker's ability affects sources with the chosen name no matter what zone they " +
                "are in. For example, a cycling ability of a card with the chosen name can't be activated " +
                "from hand."
        )
    }
}
