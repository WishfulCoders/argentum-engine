package com.wingedsheep.mtg.sets.definitions.usg.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Show and Tell
 * {2}{U}
 * Sorcery
 * Each player may put an artifact, creature, enchantment, or land card from their hand onto the
 * battlefield.
 *
 * Each player, in turn order starting with the active player, chooses up to one qualifying card in
 * their own hand during resolution; only after every choice is made do all the chosen cards enter
 * the battlefield together, each under its owner's control (rulings 2008-04-01 / 2004-10-04). The
 * per-player choice is collected by `forEachPlayerCollecting` and the single simultaneous move
 * comes after it, so nothing one player puts in can see — or react to — another player's card
 * before both are on the battlefield.
 */
val ShowAndTell = card("Show and Tell") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Each player may put an artifact, creature, enchantment, or land card from their " +
        "hand onto the battlefield."

    val showable = GameObjectFilter.Artifact or GameObjectFilter.Creature or
        GameObjectFilter.Enchantment or GameObjectFilter.Land

    spell {
        effect = Effects.Pipeline {
            val (shown) = forEachPlayerCollecting(Player.ActivePlayerFirst) {
                val candidates = gather(CardSource.FromZone(Zone.HAND, Player.You, showable))
                listOf(
                    chooseUpTo(
                        1,
                        from = candidates,
                        prompt = "You may put an artifact, creature, enchantment, or land card " +
                            "from your hand onto the battlefield"
                    )
                )
            }
            move(shown, CardDestination.ToZone(Zone.BATTLEFIELD), underOwnersControl = true)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "96"
        artist = "Jeff Laubenstein"
        flavorText = "At the academy, \"show and tell\" too often becomes \"run and hide.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b851c17-55ed-4671-b471-dc7b34944432.jpg?1783946356"
        ruling(
            "2008-04-01",
            "The current player chooses first, then each other player chooses in turn order. A player " +
                "does not have to reveal the chosen card, so long as it is clear which card was chosen. " +
                "After all choices are made, the cards are put onto the battlefield simultaneously."
        )
        ruling(
            "2004-10-04",
            "If the cards being put onto the battlefield also require choices, those choices are made " +
                "after all players choose their card. The active player makes choices for their card " +
                "(if any), then the other players (if any) in turn order."
        )
        ruling("2004-10-04", "Players choose cards during resolution, not announcement.")
    }
}
