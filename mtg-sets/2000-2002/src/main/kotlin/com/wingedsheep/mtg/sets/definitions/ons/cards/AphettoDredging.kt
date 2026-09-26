package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Aphetto Dredging
 * {3}{B}
 * Sorcery
 * Return up to three target creature cards of the creature type of your choice
 * from your graveyard to your hand.
 *
 * The cards are real targets chosen as the spell is cast (CR 601.2c). "Of the creature type of
 * your choice" is the targets' shared creature type: naming a type and targeting only cards of it
 * admits exactly the sets whose members all share one, so the requirement carries
 * [TargetObject.sameCreatureType] (printed types on graveyard cards; changelings share every type).
 * Each surviving target is returned on resolution (CR 608.2b).
 */
val AphettoDredging = card("Aphetto Dredging") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Return up to three target creature cards of the creature type of your choice from your graveyard to your hand."

    spell {
        targets(TargetFilter.CreatureInYourGraveyard, count = 3, optional = true, sameCreatureType = true)
        effect = Effects.ForEachTarget(Effects.ReturnToHand(EffectTarget.ContextTarget(0)))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "125"
        artist = "Monte Michael Moore"
        flavorText = "Phage became both executioner and savior, helping others to the same rebirth she had found."
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4e7fadf-40f1-45ff-97ef-5830381accc9.jpg?1562941515"
    }
}
