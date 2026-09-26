package com.wingedsheep.mtg.sets.definitions.lgn.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Triggers

/**
 * Goblin Assassin
 * {3}{R}{R}
 * Creature — Goblin Assassin
 * 2/2
 * Whenever Goblin Assassin or another Goblin enters, each player flips a coin.
 * Each player whose coin comes up tails sacrifices a creature.
 *
 * Ruling (2004-10-04): This coin flip has no winner or loser.
 */
val GoblinAssassin = card("Goblin Assassin") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Assassin"
    power = 2
    toughness = 2
    oracleText = "Whenever Goblin Assassin or another Goblin enters, each player flips a coin. Each player whose coin comes up tails sacrifices a creature."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withSubtype(Subtype.GOBLIN)).enters()
        effect = Effects.ForEachPlayer(
            players = Player.Each,
            effect = Effects.FlipCoin(
                lostEffect = Effects.Sacrifice(
                    filter = GameObjectFilter.Creature,
                    count = 1,
                    target = EffectTarget.Controller
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "95"
        artist = "Dave Dorman"
        flavorText = "The more victims he kills, the more likely he is to get the right one."
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57ec836f-6dcf-45f9-8e95-487762742a1e.jpg?1562912891"
    }
}
