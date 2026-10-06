package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Balance
 * {1}{W}
 * Sorcery
 * Each player chooses a number of lands they control equal to the number of lands controlled by
 * the player who controls the fewest, then sacrifices the rest. Players discard cards and
 * sacrifice creatures the same way.
 *
 * Three passes — lands, then cards in hand, then creatures — each counted when its own pass
 * begins (2016-06-08 ruling: a land creature sacrificed in the first pass is not counted for the
 * last). The count is [com.wingedsheep.sdk.scripting.values.DynamicAmount.LeastAmongPlayers]
 * around the measured player's own count, so it is "the player who controls the fewest" at any
 * table size, not a pairwise `Min(you, opponent)`.
 *
 * Each pass follows the ruling's order: every player chooses in turn order (`ActivePlayerFirst`,
 * CR 101.4) what to keep, knowing earlier choices, and only then does anything happen.
 * `forEachPlayerCollecting` gathers every player's unkept remainder into one collection, so the
 * lands (and later the creatures) are all sacrificed in one simultaneous move. Cards in hand are
 * chosen the same way without being revealed; the discard then runs once per owner
 * (`forEachCaptured`), because a discard is attributed to the player whose hand it leaves. No
 * player can act in between, so splitting the discard by owner is not observable.
 *
 * The keep is `ChooseExactly(fewest)`: the player with the fewest keeps all of theirs without a
 * prompt, and a player with more must choose exactly that many. No targets — shroud and
 * protection don't matter (2016-06-08).
 */
val Balance = card("Balance") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Each player chooses a number of lands they control equal to the number of lands " +
        "controlled by the player who controls the fewest, then sacrifices the rest. Players discard " +
        "cards and sacrifice creatures the same way."

    spell {
        effect = Effects.Pipeline {
            // 1. Lands.
            val landsToSacrifice = forEachPlayerCollecting(Player.ActivePlayerFirst) {
                val lands = gather(CardSource.ControlledPermanents(Player.You, GameObjectFilter.Land))
                val (_, rest) = chooseExactlySplit(
                    DynamicAmounts.fewestControlledBySinglePlayer(GameObjectFilter.Land),
                    from = lands,
                    chooser = Chooser.Controller,
                    selectedLabel = "Keep",
                    remainderLabel = "Sacrifice",
                    prompt = "Choose lands to keep, as many as the player with the fewest lands controls; " +
                        "the rest are sacrificed.",
                    useTargetingUI = true
                )
                listOf(rest)
            }.single()
            sacrifice(landsToSacrifice)

            // 2. Cards in hand — counted after the lands are gone.
            val cardsToDiscard = forEachPlayerCollecting(Player.ActivePlayerFirst) {
                val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
                val (_, rest) = chooseExactlySplit(
                    DynamicAmounts.leastAmongPlayers(DynamicAmounts.zone(Player.You, Zone.HAND).count()),
                    from = hand,
                    chooser = Chooser.Controller,
                    selectedLabel = "Keep",
                    remainderLabel = "Discard",
                    prompt = "Choose cards to keep, as many as the player with the fewest cards in hand has; " +
                        "the rest are discarded."
                )
                listOf(rest)
            }.single()
            val owners = captureControllers(cardsToDiscard)
            forEachCaptured(cardsToDiscard, original = cardsToDiscard, controllers = owners) {
                discard(filter(cardsToDiscard, GameObjectFilter.Any.ownedByYou()))
            }

            // 3. Creatures — counted after the discards.
            val creaturesToSacrifice = forEachPlayerCollecting(Player.ActivePlayerFirst) {
                val creatures = gather(CardSource.ControlledPermanents(Player.You, GameObjectFilter.Creature))
                val (_, rest) = chooseExactlySplit(
                    DynamicAmounts.fewestControlledBySinglePlayer(GameObjectFilter.Creature),
                    from = creatures,
                    chooser = Chooser.Controller,
                    selectedLabel = "Keep",
                    remainderLabel = "Sacrifice",
                    prompt = "Choose creatures to keep, as many as the player with the fewest creatures controls; " +
                        "the rest are sacrificed.",
                    useTargetingUI = true
                )
                listOf(rest)
            }.single()
            sacrifice(creaturesToSacrifice)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "3"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f9ea46a-411f-40ce-a873-a905180093f4.jpg?1783948717"
        ruling(
            "2016-06-08",
            "First the player whose turn it is chooses which lands (if any) to keep, then each other player " +
                "in turn order does the same. Each player will know the choices made by the players who chose " +
                "before them. All of the unchosen lands are then sacrificed simultaneously. Then the process is " +
                "repeated for cards in hand, except that no cards are revealed until all players have chosen " +
                "what to discard, at which point those cards are all discarded simultaneously. Lastly, the " +
                "process is repeated for creatures, and players will again know earlier choices made when " +
                "deciding what to sacrifice. All of the unchosen creatures are then sacrificed simultaneously."
        )
        ruling(
            "2016-06-08",
            "Balance doesn't have targets, so permanents that can't be targeted, such as a creature with " +
                "shroud or protection from white, are valid choices to be sacrificed."
        )
        ruling(
            "2016-06-08",
            "Each type of object is counted during the corresponding part of the process. Cards in hand are " +
                "counted after lands have been sacrificed, and creatures on the battlefield are counted after " +
                "cards have been discarded. Thus, a land creature sacrificed to the first part of the spell " +
                "would not be counted when determining how many creatures are on the battlefield for the last part."
        )
    }
}
