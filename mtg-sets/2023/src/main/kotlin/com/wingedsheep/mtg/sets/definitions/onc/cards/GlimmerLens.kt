package com.wingedsheep.mtg.sets.definitions.onc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.forMirrodin
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.AttackPredicate

/**
 * Glimmer Lens
 * {1}{W}
 * Artifact — Equipment
 * For Mirrodin! (When this Equipment enters, create a 2/2 red Rebel creature token, then attach this to it.)
 * Whenever equipped creature and at least one other creature attack, draw a card.
 * Equip {1}{W}
 *
 * "Equipped creature and at least one other creature" is the equipped creature plus one or more
 * more attackers in the same declaration — the ATTACHED-bound battalion shape
 * `AttackerCountAtLeast(2)` (Sokka, Lateral Strategist is the SELF-bound twin).
 */
val GlimmerLens = card("Glimmer Lens") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Equipment"
    oracleText = "For Mirrodin! (When this Equipment enters, create a 2/2 red Rebel creature token, then attach this to it.)\n" +
        "Whenever equipped creature and at least one other creature attack, draw a card.\n" +
        "Equip {1}{W}"

    forMirrodin()

    triggeredAbility {
        trigger = Triggers.attached.attacks(setOf(AttackPredicate.AttackerCountAtLeast(2)))
        effect = Effects.DrawCards(1)
        description = "Whenever equipped creature and at least one other creature attack, draw a card."
    }

    equipAbility("{1}{W}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "6"
        artist = "Sidharth Chaturvedi"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c9262000-e6f3-4da1-ad1c-038f65d3bef6.jpg?1783918163"
        ruling(
            "2023-02-04",
            "If the ability causes two Rebel tokens to be created (due to an effect such as that of " +
                "Mondrak, Glory Dominus), the Equipment becomes attached to only one of them."
        )
    }
}
