package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Emberheart Challenger {1}{R}
 * Creature — Mouse Warrior
 * 2/2
 *
 * Haste
 * Prowess
 * Valiant — Whenever this creature becomes the target of a spell or ability you control
 * for the first time each turn, exile the top card of your library. Until end of turn,
 * you may play that card.
 */
val EmberheartChallenger = card("Emberheart Challenger") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Mouse Warrior"
    power = 2
    toughness = 2
    oracleText = "Haste\nProwess\nValiant — Whenever this creature becomes the target of a spell or ability you control for the first time each turn, exile the top card of your library. Until end of turn, you may play that card."

    keywords(Keyword.HASTE)
    prowess()

    triggeredAbility {
        trigger = Triggers.self.becomesTarget(byYou = true, firstTimeEachTurn = true)
        effect = Effects.Pipeline {
            val exiledCard = gather(CardSource.TopOfLibrary(1))
            exile(exiledCard)
            run(Effects.GrantMayPlayFromExile(exiledCard))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "133"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/0035082e-bb86-4f95-be48-ffc87fe5286d.jpg?1721426609"
    }
}
