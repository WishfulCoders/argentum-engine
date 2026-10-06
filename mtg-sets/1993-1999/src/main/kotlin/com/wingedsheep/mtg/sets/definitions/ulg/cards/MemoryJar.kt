package com.wingedsheep.mtg.sets.definitions.ulg.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Memory Jar
 * {5}
 * Artifact
 * {T}, Sacrifice this artifact: Each player exiles all cards from their hand face down and draws
 * seven cards. At the beginning of the next end step, each player discards their hand and returns
 * to their hand each card they exiled this way.
 *
 * Every hand is gathered at once ([CardSource.FromZone] with [Player.Each]) and exiled in one
 * simultaneous move; each card lands in its owner's exile, face down with no look permission
 * ([FaceDownMode.HIDDEN]) — per the ruling, not even its owner may look at it until it returns.
 * `moveTracked` records the cards actually exiled, and that pile rides into the delayed trigger
 * (`carryCollections`), which drops any card that has left exile in the meantime (CR 603.7c).
 *
 * At the next end step each player discards their hand (a per-player loop, so each discard is that
 * player's own), then every remaining exiled card returns to its owner's hand. "The next end step"
 * is any player's: activated during an end step, it waits for the following turn's.
 */
val MemoryJar = card("Memory Jar") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}, Sacrifice this artifact: Each player exiles all cards from their hand face down and " +
        "draws seven cards. At the beginning of the next end step, each player discards their hand and " +
        "returns to their hand each card they exiled this way."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.Pipeline {
            val hands = gather(CardSource.FromZone(Zone.HAND, Player.Each))
            val jarExiled = moveTracked(
                hands,
                CardDestination.ToZone(Zone.EXILE),
                faceDown = FaceDownMode.HIDDEN
            )
            run(
                Effects.CreateDelayedTrigger(
                    step = Step.END,
                    effect = Effects.Pipeline {
                        run(
                            Effects.ForEachPlayer(
                                Player.Each,
                                Effects.Pipeline {
                                    val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
                                    discard(hand)
                                }
                            )
                        )
                        toHand(jarExiled)
                    },
                    carryCollections = listOf(jarExiled.key)
                )
            )
            run(Effects.ForEachPlayer(Player.Each, Effects.DrawCards(7)))
        }
        description = "{T}, Sacrifice this artifact: Each player exiles all cards from their hand face down " +
            "and draws seven cards. At the beginning of the next end step, each player discards their hand " +
            "and returns to their hand each card they exiled this way."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "129"
        artist = "Donato Giancola"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a15d33d6-7213-4482-a1be-ac0a73644af6.jpg?1783946221"
        ruling("2004-10-04", "You can't look at the cards you exiled until they return to your hand.")
    }
}
