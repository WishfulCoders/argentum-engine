package com.wingedsheep.mtg.sets.definitions.usg.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Time Spiral
 * {4}{U}{U}
 * Sorcery
 *
 * Exile Time Spiral. Each player shuffles their hand and graveyard into their library, then draws
 * seven cards. You untap up to six lands.
 *
 * "Exile Time Spiral" is the `selfExile()` flag: the spell is on the stack throughout, so it is
 * never part of the shuffled graveyard, and it goes to exile instead of the graveyard on
 * resolution. The wheel is Timetwister's two-pass shape (everyone shuffles, then everyone draws
 * seven). "You untap up to six lands" is untargeted and may pick any lands (2022-12-08 ruling), so
 * the lands are gathered from the whole battlefield and chosen by the caster as the spell resolves.
 */
val TimeSpiral = card("Time Spiral") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Exile Time Spiral. Each player shuffles their hand and graveyard into their library, " +
        "then draws seven cards. You untap up to six lands."

    spell {
        selfExile()
        effect = Effects.ForEachPlayer(
            players = Player.Each,
            Effects.Pipeline {
                val cards = gather(
                    CardSource.FromMultipleZones(
                        zones = listOf(Zone.HAND, Zone.GRAVEYARD),
                        player = Player.You,
                    )
                )
                move(cards, CardDestination.ToZone(Zone.LIBRARY, Player.You, ZonePlacement.Shuffled))
            },
        ) then Effects.ForEachPlayer(Player.Each, Effects.DrawCards(7)) then
            Effects.Pipeline {
                val lands = gather(CardSource.BattlefieldMatching(GameObjectFilter.Land))
                val toUntap = chooseUpTo(6, from = lands)
                run(Effects.TapCollection(collection = toUntap, tap = false))
            }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "103"
        artist = "Michael Sutfin"
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f3d62dbd-63db-4ac9-950f-9852627f23f2.jpg?1783946354"
        ruling(
            "2022-12-08",
            "You choose which lands to untap as the spell resolves. They aren't targeted, and they don't " +
                "have to be lands that you control."
        )
    }
}
