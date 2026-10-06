package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fallaji Archaeologist
 * {1}{U}
 * Creature — Human Scout
 * 0/3
 *
 * When this creature enters, mill three cards. You may put a noncreature, nonland card from
 * among the cards milled this way into your hand. If you don't, put a +1/+1 counter on this
 * creature.
 *
 * "If you don't" reads the picked collection: declining, or milling no noncreature nonland
 * card, adds the counter.
 */
val FallajiArchaeologist = card("Fallaji Archaeologist") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Scout"
    power = 0
    toughness = 3
    oracleText = "When this creature enters, mill three cards. You may put a noncreature, nonland card from among the cards milled this way into your hand. If you don't, put a +1/+1 counter on this creature. (To mill a card, put the top card of your library into your graveyard.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val milled = mill(3)
            val picked = chooseUpTo(
                1,
                from = milled,
                filter = GameObjectFilter.Noncreature and GameObjectFilter.Nonland,
                showAllCards = true,
                prompt = "You may put a noncreature, nonland card milled this way into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Leave in graveyard"
            )
            toHand(picked)
            run(Effects.If(
                condition = Conditions.Not(whenMatches(picked)),
                then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
            ))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "48"
        artist = "Caroline Gariba"
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b0eab397-25a6-4377-8e12-e8acef9675cf.jpg?1783920114"
    }
}
