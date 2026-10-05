package com.wingedsheep.mtg.sets.definitions.eve.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Flickerwisp
 * {1}{W}{W}
 * Creature — Elemental
 * 3/1
 *
 * Flying
 * When this creature enters, exile another target permanent. Return that card to the battlefield
 * under its owner's control at the beginning of the next end step.
 */
val Flickerwisp = card("Flickerwisp") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Elemental"
    power = 3
    toughness = 1
    oracleText = "Flying\nWhen this creature enters, exile another target permanent. Return that card to the " +
        "battlefield under its owner's control at the beginning of the next end step."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(TargetFilter(GameObjectFilter.Permanent, excludeSelf = true))
        effect = Patterns.Exile.exileUntilEndStep(permanent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "6"
        artist = "Jeremy Enecio"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5bb3cb5c-8d66-4f5e-a9a9-917e6045f024.jpg?1783942694"
        ruling("2021-03-19", "The exiled card will return to the battlefield at the beginning of the end step even if Flickerwisp is no longer on the battlefield.")
        ruling("2021-03-19", "If the permanent that returns to the battlefield has any abilities that trigger at the beginning of the end step, those abilities won't trigger that turn.")
        ruling("2021-03-19", "If a token is exiled this way, it will cease to exist and won't return to the battlefield.")
    }
}
