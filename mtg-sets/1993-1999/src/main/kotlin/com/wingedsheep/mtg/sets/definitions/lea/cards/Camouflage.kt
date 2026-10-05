package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.scripting.conditions.IsInStep
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Rarity

// Current Oracle replaces the original face-down procedure with randomly assigned blocker piles.
val Camouflage = card("Camouflage") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Cast this spell only during your declare attackers step.\nThis turn, instead of declaring blockers, each defending player chooses any number of creatures they control and divides them into a number of piles equal to the number of attacking creatures for whom that player is the defending player. Creatures those players control that can block additional creatures may likewise be put into additional piles. Assign each pile to a different one of those attacking creatures at random. Each creature in a pile that can block the creature that pile is assigned to does so. (Piles can be empty.)"
    spell {
        castOnlyIf(IsInStep(listOf(Step.DECLARE_ATTACKERS)))
        effect = Effects.RandomizedBlockerPiles()
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "187"
        artist = "Jesper Myrfors"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/3838c2a3-7fab-4976-9c1b-2891aee24e52.jpg?1783948678"
    }
}
