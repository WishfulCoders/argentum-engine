package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Nimble Larcenist
 * {W}{U}{B}
 * Creature — Bird Rogue
 * 2/1
 * Flying
 * When this creature enters, target opponent reveals their hand. You choose an artifact,
 * instant, or sorcery card from it and exile that card.
 *
 * The ETB is Thought-Knot Seer's `Patterns.Hand.revealHandAndExileChosen` with the filter
 * narrowed to artifact-or-instant-or-sorcery (Keeper of the Cadence's union filter).
 */
val NimbleLarcenist = card("Nimble Larcenist") {
    manaCost = "{W}{U}{B}"
    colorIdentity = "WUB"
    typeLine = "Creature — Bird Rogue"
    power = 2
    toughness = 1
    oracleText = "Flying\nWhen this creature enters, target opponent reveals their hand. You choose " +
        "an artifact, instant, or sorcery card from it and exile that card."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Patterns.Hand.revealHandAndExileChosen(
            target = opponent,
            filter = GameObjectFilter.Artifact or GameObjectFilter.InstantOrSorcery,
            prompt = "Choose an artifact, instant, or sorcery card to exile",
        )
        description = "When this creature enters, target opponent reveals their hand. You choose " +
            "an artifact, instant, or sorcery card from it and exile that card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "205"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/0/1/0150d4c7-2c90-4a71-b2d3-3d9a0079fea1.jpg?1783923082"
    }
}
