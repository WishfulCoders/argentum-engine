package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/** Sentinel of Lost Lore — Wilds of Eldraine #184. */
val SentinelOfLostLore = card("Sentinel of Lost Lore") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Knight"
    oracleText = "When this creature enters, choose one or more —\n" +
        "• Return target card you own in exile that has an Adventure to your hand.\n" +
        "• Put target card you don't own in exile that has an Adventure on the bottom of its owner's library.\n" +
        "• Exile target player's graveyard."
    power = 3
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Modal(
            modes = listOf(
                mode("Return target card you own in exile that has an Adventure to your hand.") {
                    val targetedObject = target(TargetFilter(Filters.HasAdventure.ownedByYou(), zone = Zone.EXILE))
                    effect = Effects.ReturnToHand(targetedObject)
                },
                mode("Put target card you don't own in exile that has an Adventure " +
                    "on the bottom of its owner's library.") {
                    val targetedObject = target(TargetFilter(Filters.HasAdventure.ownedByOpponent(), zone = Zone.EXILE))
                    effect = Effects.PutOnBottomOfLibrary(targetedObject)
                },
                mode("Exile target player's graveyard.") {
                    val player = target(Targets.Player)
                    effect = Effects.Pipeline {
                        val sentinelTargetGraveyard = gather(
                            CardSource.FromZone(Zone.GRAVEYARD, player.asPlayer)
                        )
                        exile(sentinelTargetGraveyard, player.asPlayer)
                    }
                },
            ),
            chooseCount = 3,
            minChooseCount = 1,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "184"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f109a5bf-1472-4b87-b3d3-70db0e123693.jpg?1783915078"

        ruling(
            "2023-09-01",
            "The first and second modes can target any face-up exiled card that has an Adventure " +
                "and has an appropriate owner, whether or not it was cast as an Adventure.",
        )
        ruling(
            "2023-09-01",
            "You must choose at least one mode if possible. The last mode can target a player " +
                "whose graveyard is empty.",
        )
    }
}
