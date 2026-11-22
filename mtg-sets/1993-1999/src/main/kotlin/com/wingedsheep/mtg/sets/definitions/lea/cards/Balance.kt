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
 * Three parts, run in order, each with its own count (2016-06-08 ruling): lands, then cards in
 * hand (counted after the lands are gone), then creatures (counted after the discards, so a land
 * creature sacrificed in part one isn't counted again).
 *
 * Each part is `forEachPlayerCollecting(ActivePlayerFirst)`: every player, in APNAP order, keeps
 * exactly N of their own objects — N being [DynamicAmounts.fewestControlledBySinglePlayer] (or
 * [DynamicAmounts.leastAmongPlayers] over hand size), measured once per player — and the unkept
 * remainder is collected across the table. Only after every player has chosen is that collection
 * sacrificed (or discarded) in one move, so all of a part's sacrifices or discards happen
 * simultaneously and no hand choice is revealed before everyone has made theirs. Nothing moves
 * while players choose, so N is the same for each of them.
 *
 * `ChooseExactly` clamps to the pool, so the player with the fewest keeps everything without a
 * prompt. Nothing here targets: shroud and protection don't stop a creature being sacrificed.
 */
val Balance = card("Balance") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Each player chooses a number of lands they control equal to the number of " +
        "lands controlled by the player who controls the fewest, then sacrifices the rest. " +
        "Players discard cards and sacrifice creatures the same way."

    spell {
        effect = Effects.Pipeline {
            // Part one: lands.
            val (landsToSacrifice) = forEachPlayerCollecting(Player.ActivePlayerFirst) {
                val lands = gather(CardSource.ControlledPermanents(Player.You, GameObjectFilter.Land))
                val (_, rest) = chooseExactlySplit(
                    DynamicAmounts.fewestControlledBySinglePlayer(GameObjectFilter.Land),
                    from = lands,
                    chooser = Chooser.Controller,
                    selectedLabel = "Keep",
                    remainderLabel = "Sacrifice",
                    prompt = "Choose lands to keep; the rest are sacrificed.",
                    useTargetingUI = true
                )
                listOf(rest)
            }
            sacrifice(landsToSacrifice)

            // Part two: cards in hand, counted after the lands are gone.
            val (cardsToDiscard) = forEachPlayerCollecting(Player.ActivePlayerFirst) {
                val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
                val (_, rest) = chooseExactlySplit(
                    DynamicAmounts.leastAmongPlayers(DynamicAmounts.cardsInYourHand()),
                    from = hand,
                    chooser = Chooser.Controller,
                    selectedLabel = "Keep",
                    remainderLabel = "Discard",
                    prompt = "Choose cards to keep; the rest are discarded."
                )
                listOf(rest)
            }
            discard(cardsToDiscard)

            // Part three: creatures, counted after the discards.
            val (creaturesToSacrifice) = forEachPlayerCollecting(Player.ActivePlayerFirst) {
                val creatures = gather(CardSource.ControlledPermanents(Player.You, GameObjectFilter.Creature))
                val (_, rest) = chooseExactlySplit(
                    DynamicAmounts.fewestControlledBySinglePlayer(GameObjectFilter.Creature),
                    from = creatures,
                    chooser = Chooser.Controller,
                    selectedLabel = "Keep",
                    remainderLabel = "Sacrifice",
                    prompt = "Choose creatures to keep; the rest are sacrificed.",
                    useTargetingUI = true
                )
                listOf(rest)
            }
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
            "First the player whose turn it is chooses which lands to keep, then each other player " +
                "in turn order does the same, knowing the choices made before them. All unchosen " +
                "lands are sacrificed at the same time. The same process is then used for cards in " +
                "hand, except no cards are revealed until all players have chosen what to discard; " +
                "then all are discarded at the same time. Lastly the same is done for creatures."
        )
        ruling(
            "2016-06-08",
            "Balance doesn't target any players or permanents. You may sacrifice creatures with " +
                "shroud or protection, for example."
        )
        ruling(
            "2016-06-08",
            "Each type is counted during its own part of the resolution. The number of cards in " +
                "hand is counted after lands have been sacrificed, and the number of creatures is " +
                "counted after cards have been discarded. A land creature sacrificed in the first " +
                "part isn't counted when determining how many creatures to keep."
        )
    }
}
