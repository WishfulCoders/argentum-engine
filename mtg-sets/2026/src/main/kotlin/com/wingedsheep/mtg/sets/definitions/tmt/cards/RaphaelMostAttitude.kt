package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Raphael, Most Attitude
 * {3}{R}
 * Legendary Creature — Mutant Ninja Turtle
 * 4/3
 *
 * Menace
 * Alliance — Whenever another creature you control enters, you may exile the top
 * card of your library.
 * Whenever Raphael attacks, until end of turn, you may play a card exiled with
 * Raphael.
 */
val RaphaelMostAttitude = card("Raphael, Most Attitude") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Mutant Ninja Turtle"
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\nAlliance — Whenever another creature you control enters, you may exile the top card of your library.\nWhenever Raphael attacks, until end of turn, you may play a card exiled with Raphael."
    power = 4
    toughness = 3

    keywords(Keyword.MENACE)

    // Alliance: exile the top card linked to Raphael (accumulates the impulse pile).
    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.May(
            Effects.Pipeline {
                val raphaelTop = gather(CardSource.TopOfLibrary(1))
                exile(raphaelTop, linkToSource = true)
            }
        )
        description = "Alliance — Whenever another creature you control enters, you may exile the top card of your library."
    }

    // Attack: grant permission to play any card exiled with Raphael until end of turn.
    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Pipeline {
            val raphaelExile = gather(CardSource.FromLinkedExile())
            run(Effects.GrantMayPlayFromExile(raphaelExile, MayPlayExpiry.EndOfTurn))
        }
        description = "Whenever Raphael attacks, until end of turn, you may play a card exiled with Raphael."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "101"
        artist = "Thomas Chamberlain-Keen"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88385a87-f931-409f-8a21-250f0866d63d.jpg?1771502673"
    }
}
