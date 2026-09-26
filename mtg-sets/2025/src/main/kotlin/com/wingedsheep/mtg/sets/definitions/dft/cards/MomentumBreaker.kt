package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Momentum Breaker
 * {1}{B}
 * Enchantment
 * Start your engines!
 * When this enchantment enters, each opponent sacrifices a creature or Vehicle of their choice.
 * Each opponent who can't discards a card.
 * {2}, Sacrifice this enchantment: You gain life equal to your speed.
 *
 * The "each opponent … / each opponent who can't …" split is Entropic Battlecruiser's idiom: one
 * [ForEachPlayerEffect] over [Player.EachOpponent] whose body is a [Effects.If] evaluated
 * per iterated opponent. Inside the loop the controller is rebound to that opponent, so
 * `Exists(Player.You, Zone.BATTLEFIELD, CreatureOrVehicle)` asks "does *this* opponent control a
 * creature or Vehicle" and [EffectTarget.Controller] is that opponent. Modelling it as one
 * conditional rather than a sacrifice followed by a "did anything die" check keeps the per-player
 * evaluation right in a multiplayer game, where opponents differ.
 *
 * A player who controls a creature or Vehicle has to sacrifice one — the sacrifice is mandatory and
 * only the choice of which is theirs — so an opponent with a board never gets the discard branch.
 *
 * The speed payoff is a plain [DynamicAmount.Speed] over [Player.You]; a player with no speed reads
 * as 0 (CR 702.179f), so it needs no "has speed" guard.
 */
val MomentumBreaker = card("Momentum Breaker") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Start your engines! (If you have no speed, it starts at 1. It increases once on " +
        "each of your turns when an opponent loses life. Max speed is 4.)\n" +
        "When this enchantment enters, each opponent sacrifices a creature or Vehicle of their " +
        "choice. Each opponent who can't discards a card.\n" +
        "{2}, Sacrifice this enchantment: You gain life equal to your speed."

    startYourEngines()

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachPlayer(
            players = Player.EachOpponent,
            effect = Effects.If(
                condition = Exists(
                    player = Player.You,
                    zone = Zone.BATTLEFIELD,
                    filter = GameObjectFilter.CreatureOrVehicle
                ),
                then = Effects.Sacrifice(
                    GameObjectFilter.CreatureOrVehicle,
                    target = EffectTarget.Controller
                ),
                otherwise = Patterns.Hand.discardCards(1, EffectTarget.Controller)
            )
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.SacrificeSelf)
        effect = Effects.GainLife(DynamicAmounts.speed(Player.You))
        description = "{2}, Sacrifice this enchantment: You gain life equal to your speed."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "97"
        artist = "Dmitry Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38513b53-384f-45e7-9905-80dd2c3c4918.jpg?1783907891"
        ruling(
            "2025-02-07",
            "If an effect needs to know what a player's speed is and that player doesn't have a speed, their speed is considered 0."
        )
    }
}
