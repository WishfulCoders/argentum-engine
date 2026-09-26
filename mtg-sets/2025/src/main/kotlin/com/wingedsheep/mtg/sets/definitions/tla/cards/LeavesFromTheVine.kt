package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Leaves from the Vine
 * {1}{G}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Mill three cards, then create a Food token. (It's an artifact with "{2}, {T}, Sacrifice this
 *     token: You gain 3 life.")
 * II — Put a +1/+1 counter on each of up to two target creatures you control.
 * III — Draw a card if there's a creature or Lesson card in your graveyard.
 */
val LeavesFromTheVine = card("Leaves from the Vine") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Mill three cards, then create a Food token. (It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n" +
        "II — Put a +1/+1 counter on each of up to two target creatures you control.\n" +
        "III — Draw a card if there's a creature or Lesson card in your graveyard."

    sagaChapter(1) {
        effect = Patterns.Library.mill(3) then Effects.CreateFood()
    }

    sagaChapter(2) {
        targets(TargetFilter.CreatureYouControl, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0))
        )
    }

    sagaChapter(3) {
        effect = Effects.If(
            condition = Conditions.GraveyardContains(
                GameObjectFilter.Creature or GameObjectFilter.Any.withSubtype("Lesson")
            ),
            then = Effects.DrawCards(1)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "185"
        artist = "Ittoku"
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b18995c2-efe3-46ca-8204-e2dc0e42f6e3.jpg?1782135327"
    }
}
