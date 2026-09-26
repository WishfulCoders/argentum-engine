package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Garruk, Veiled Butcher
 *
 * - The static "exile it instead" is Liesa's [RedirectZoneChange] over opposing creatures going
 *   battlefield → graveyard; it applies only while Garruk is on the battlefield.
 * - +2 is an optional ("up to one") creature target and a -4/-1 with [Duration.UntilYourNextTurn].
 * - −2 is the Rise of the Witch-king edict: every player sacrifices a creature of their choice,
 *   and [Conditions.YouSacrificedThisWay] gates the Beast.
 * - −3 iterates each opponent: they discard two cards of their choice, then — because the context
 *   controller is rebound to the iterated opponent inside the loop — the "you draw" reads
 *   [Player.ControllerOfSource] (Garruk's controller, via last-known information if the −3 took
 *   him to zero loyalty). The draw is skipped only when at least two nonland cards were discarded,
 *   so an opponent who discards fewer than two cards (small hand) or any land also gives a card.
 */
val GarrukVeiledButcher = card("Garruk, Veiled Butcher") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Planeswalker — Garruk"
    startingLoyalty = 5
    oracleText = "If a creature an opponent controls would die, exile it instead.\n" +
        "+2: Up to one target creature gets -4/-1 until your next turn.\n" +
        "−2: Each player sacrifices a creature of their choice. If you sacrificed a creature this way, " +
        "create a 4/4 green Beast creature token with trample.\n" +
        "−3: Each opponent discards two cards. For each opponent who didn't discard two nonland cards " +
        "this way, you draw a card."

    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.opponentControls(),
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD,
            ),
        ),
    )

    // +2: Up to one target creature gets -4/-1 until your next turn.
    loyaltyAbility(+2) {
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.ModifyStats(-4, -1, creature, Duration.UntilYourNextTurn)
    }

    // −2: Each player sacrifices a creature of their choice. If you sacrificed a creature this
    // way, create a 4/4 green Beast creature token with trample.
    loyaltyAbility(-2) {
        effect = Effects.Sacrifice(
            GameObjectFilter.Creature,
            count = 1,
            target = EffectTarget.PlayerRef(Player.Each),
        ) then Effects.If(
            condition = Conditions.YouSacrificedThisWay,
            then = Effects.CreateToken(
                power = 4,
                toughness = 4,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Beast"),
                keywords = setOf(Keyword.TRAMPLE),
                imageUri = "https://cards.scryfall.io/normal/front/8/5/859bda9a-fa90-4ad3-b0c1-6fc62e27c12f.jpg?1789736256",
            ),
        )
    }

    // −3: Each opponent discards two cards. For each opponent who didn't discard two nonland
    // cards this way, you draw a card.
    loyaltyAbility(-3) {
        effect = Effects.ForEachPlayer(
            players = Player.EachOpponent,
            Effects.Pipeline {
                val garrukHand = gather(CardSource.FromZone(Zone.HAND, Player.You))
                val garrukDiscard = chooseExactly(2, from = garrukHand, prompt = "Choose two cards to discard")
                val garrukDiscarded = moveTracked(
                    garrukDiscard,
                    CardDestination.ToZone(Zone.GRAVEYARD),
                    moveType = MoveType.Discard
                )
                ifNotEmpty(garrukDiscarded, filter = GameObjectFilter.Nonland, minSize = 2) {
                    run(Effects.Nothing)
                } orElse {
                    run(Effects.DrawCards(1, EffectTarget.PlayerRef(Player.ControllerOfSource)))
                }
            },
        )
        description = "Each opponent discards two cards. For each opponent who didn't discard two " +
            "nonland cards this way, you draw a card."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "229"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d48bfb8a-d135-45f3-be99-4694b4b9ab93.jpg?1788329269"
        inBooster = false
    }
}
