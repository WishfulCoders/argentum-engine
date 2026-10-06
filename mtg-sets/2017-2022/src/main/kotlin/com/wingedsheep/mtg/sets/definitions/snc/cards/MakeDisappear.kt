package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Make Disappear
 * {1}{U}
 * Instant
 * Casualty 1 (As you cast this spell, you may sacrifice a creature with power 1 or greater. When you do, copy this spell and you may choose a new target for the copy.)
 * Counter target spell unless its controller pays {2}.
 *
 * Casualty 1 (CR 702.153) is the printed [KeywordAbility.Casualty]; the reflexive copy it queues may
 * choose a new target spell, so one cast can tax two spells. The counter is the plain
 * [Effects.CounterUnlessPays] tax over [TargetFilter.SpellOnStack].
 */
val MakeDisappear = card("Make Disappear") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Casualty 1 (As you cast this spell, you may sacrifice a creature with power 1 or greater. When you do, copy this spell and you may choose a new target for the copy.)\nCounter target spell unless its controller pays {2}."

    keywordAbility(KeywordAbility.casualty(1))

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterUnlessPays("{2}")
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "49"
        artist = "Inka Schulz"
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3f2d6a21-ea77-484b-9e3a-1bd49806f907.jpg?1783923144"

        ruling(
            "2022-04-29",
            "If you pay the casualty cost of a spell, the copy will resolve before the original spell."
        )
        ruling(
            "2022-04-29",
            "The copy of the spell is created on the stack, so it's not \"cast.\" Abilities that " +
                "trigger when a player casts a spell won't trigger."
        )
    }
}
