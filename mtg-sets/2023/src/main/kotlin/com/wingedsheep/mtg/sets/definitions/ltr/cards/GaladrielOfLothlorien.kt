package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Galadriel of Lothlórien
 * {1}{G}{U}
 * Legendary Creature — Elf Noble
 * 3/3
 *
 * Whenever the Ring tempts you, if you chose a creature other than Galadriel as your
 * Ring-bearer, scry 3.
 * Whenever you scry, you may reveal the top card of your library. If a land card is
 * revealed this way, put it onto the battlefield tapped.
 *
 * The reveal/place half composes the gather→reveal→move pipeline with the new
 * `MoveCollectionEffect.filter` (move only the revealed land) into the battlefield tapped
 * (`ZonePlacement.Tapped`); a non-land simply stays on top.
 */
val GaladrielOfLothlorien = card("Galadriel of Lothlórien") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Elf Noble"
    power = 3
    toughness = 3
    oracleText = "Whenever the Ring tempts you, if you chose a creature other than Galadriel " +
        "as your Ring-bearer, scry 3.\n" +
        "Whenever you scry, you may reveal the top card of your library. If a land card is " +
        "revealed this way, put it onto the battlefield tapped."

    triggeredAbility {
        trigger = Triggers.you.isTemptedByTheRing()
        interveningIf = Conditions.YouChoseOtherCreatureAsRingBearer
        effect = Patterns.Library.scry(3)
    }

    triggeredAbility {
        trigger = Triggers.you.scries()
        effect = Effects.May(
            Effects.Pipeline {
                val revealedTop = gather(CardSource.TopOfLibrary(1, Player.You))
                reveal(revealedTop)
                move(
                    revealedTop,
                    CardDestination.ToZone(
                        Zone.BATTLEFIELD,
                        player = Player.You,
                        placement = ZonePlacement.Tapped
                    ),
                    filter = GameObjectFilter.Land
                )
            }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "206"
        flavorText = "\"I pass the test. I will diminish, and go into the West, and remain Galadriel.\""
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6e5c3b3-9a70-4a1c-bfb3-db27e51c4b8d.jpg?1686969798"
    }
}
