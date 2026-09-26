package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.CollectionFilter

/**
 * The target opponent's creatures and planeswalkers are gathered, narrowed to those tied for the
 * greatest mana value, and that player picks which of the tied permanents to sacrifice. The life
 * gain is unconditional — it happens even when the opponent controls nothing to sacrifice.
 */
val BreakUnderPressure = card("Break Under Pressure") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target opponent sacrifices a creature or planeswalker with the greatest mana value " +
        "among creatures and planeswalkers they control. You gain 2 life."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            val candidates = gather(
                CardSource.ControlledPermanents(
                    player = opponent.asPlayer,
                    filter = GameObjectFilter.CreatureOrPlaneswalker
                )
            )
            val greatest = filter(candidates, CollectionFilter.GreatestManaValue)
            val sacrificed = chooseExactly(
                1,
                from = greatest,
                chooser = Chooser.TargetPlayer,
                prompt = "Choose a creature or planeswalker with the greatest mana value to sacrifice",
                useTargetingUI = true
            )
            sacrifice(sacrificed)
            run(Effects.GainLife(2))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "50"
        artist = "Jeff Miracola"
        flavorText = "\"Please, do take it personally.\"\n—Ingris Stingerquill"
        imageUri = "https://cards.scryfall.io/normal/front/4/6/46974d94-e900-43e4-92b5-4fb9b9f7cf46.jpg?1789385622"
        inBooster = false
    }
}
