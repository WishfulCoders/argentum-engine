package com.wingedsheep.mtg.sets.definitions.wwk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Bojuka Bog — Worldwake #132 (canonical printing)
 * Land
 *
 * This land enters tapped.
 * When this land enters, exile target player's graveyard.
 * {T}: Add {B}.
 *
 * The graveyard exile is Tormod's Crypt's pipeline, carried on an enters trigger.
 */
val BojukaBog = card("Bojuka Bog") {
    manaCost = ""
    colorIdentity = "B"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n" +
        "When this land enters, exile target player's graveyard.\n" +
        "{T}: Add {B}."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.self.enters()
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, player.asPlayer))
            exile(graveyard, player.asPlayer)
        }
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "132"
        artist = "Howard Lyon"
        imageUri = "https://cards.scryfall.io/normal/front/5/2/529c38b3-7397-4dac-9859-acd9cd451c32.jpg?1783942037"
    }
}
