package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Gastal Raider — Aetherdrift #86
 * {2}{B} · Creature — Vampire Rogue · 2/1
 *
 * Start your engines!
 * When this creature enters, target opponent reveals their hand. You choose an instant or sorcery
 * card from it. That player discards that card.
 * Max speed — This creature gets +1/+1 and has menace.
 *
 * The gather is filtered to instants and sorceries so the choice is legal by construction: an
 * opponent holding none simply reveals and discards nothing, which is the printed outcome — the
 * card says "you choose an instant or sorcery card from it", not "a card". The discard is a real
 * discard ([MoveType.Discard]), so "whenever a player discards" triggers see it.
 *
 * The reveal and the discard both resolve against the *chosen* opponent
 * ([Player.ContextPlayer] 0), which matters in multiplayer — this is a targeted trigger, not an
 * each-opponent one.
 */
val GastalRaider = card("Gastal Raider") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Rogue"
    power = 2
    toughness = 1
    oracleText = "Start your engines!\n" +
        "When this creature enters, target opponent reveals their hand. You choose an instant or " +
        "sorcery card from it. That player discards that card.\n" +
        "Max speed — This creature gets +1/+1 and has menace."

    startYourEngines()

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(opponent))
            val revealedInstantsAndSorceries = gather(
                CardSource.FromZone(
                    Zone.HAND,
                    opponent.asPlayer,
                    GameObjectFilter.InstantOrSorcery
                )
            )
            val toDiscard = chooseExactly(
                1,
                from = revealedInstantsAndSorceries,
                chooser = Chooser.Controller,
                prompt = "Choose an instant or sorcery card to discard"
            )
            discard(toDiscard, opponent.asPlayer)
        }
        description = "When this creature enters, target opponent reveals their hand. You choose " +
            "an instant or sorcery card from it. That player discards that card."
    }

    maxSpeed {
        staticAbility { ability = ModifyStats(1, 1, GroupFilter.source()) }
        keywords(Keyword.MENACE)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "86"
        artist = "Lorenzo Mastroianni"
        flavorText = "Win or lose, she will sate her hunger."
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6e4877b5-4ce5-466a-810f-6501f2a0f217.jpg?1783907897"
    }
}
