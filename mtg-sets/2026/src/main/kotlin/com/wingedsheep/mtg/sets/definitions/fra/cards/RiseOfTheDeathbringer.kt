package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * "Cards drawn this way" is the actual number drawn, not the number asked for: a short library or
 * a replaced draw draws fewer. It is read as the growth of your cards-drawn-this-turn tally across
 * the draw — nothing else can draw for you in the middle of the spell's resolution.
 */
val RiseOfTheDeathbringer = card("Rise of the Deathbringer") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Draw cards equal to the greatest power among creatures you control. You lose life equal to the " +
        "number of cards drawn this way.\n" +
        "• All creatures get -3/-3 until end of turn."

    spell {
        val drawnThisTurn = DynamicAmounts.cardsDrawnThisTurn(Player.You)
        effect = ModalEffect.chooseOne(
            Mode(
                effect = Effects.Pipeline {
                    val drawnBefore = storeNumber(drawnThisTurn)
                    run(Effects.DrawCards(
                        DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxPower()
                    ))
                    run(Effects.LoseLife(
                        drawnThisTurn - drawnBefore.amount,
                        EffectTarget.Controller
                    ))
                },
                description = "Draw cards equal to the greatest power among creatures you control. You lose " +
                    "life equal to the number of cards drawn this way."
            ),
            Mode(
                effect = Patterns.Group.modifyStatsForAll(-3, -3, GroupFilter.AllCreatures),
                description = "All creatures get -3/-3 until end of turn."
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "63"
        artist = "Lie Setiawan"
        flavorText = "\"I am the curse.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/1/811719ad-b5a3-4d31-8c6f-5dbdfccf7c1f.jpg?1789470807"
        inBooster = false
    }
}
