package com.wingedsheep.mtg.sets.definitions.big.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Step

/**
 * Generous Plunderer
 * {1}{R}
 * Creature — Human Rogue
 * 2/2
 *
 * Menace
 * At the beginning of your upkeep, you may create a Treasure token. When you do,
 * target opponent creates a tapped Treasure token.
 * Whenever this creature attacks, it deals damage to defending player equal to
 * the number of artifacts they control.
 */
val GenerousPlunderer = card("Generous Plunderer") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Rogue"
    power = 2
    toughness = 2
    oracleText = "Menace\n" +
        "At the beginning of your upkeep, you may create a Treasure token. When you do, target opponent creates a tapped Treasure token.\n" +
        "Whenever this creature attacks, it deals damage to defending player equal to the number of artifacts they control."

    keywords(Keyword.MENACE)

    // "you may create a Treasure token. When you do, target opponent creates a tapped Treasure token."
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.ReflexiveTrigger(
            action = Effects.CreateTreasure(1),
            optional = true,
        ) {
            val opponent = target(Targets.Opponent)
            effect = Effects.CreateTreasure(controller = opponent, tapped = true)
        }
    }

    // "Whenever this creature attacks, it deals damage to defending player equal
    // to the number of artifacts they control."
    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.DealDamage(
            amount = DynamicAmounts.battlefield(
                Player.DefendingPlayer,
                GameObjectFilter.Artifact,
            ).count(),
            target = EffectTarget.PlayerRef(Player.DefendingPlayer),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "11"
        artist = "Alexander Mokhov"
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4c6cf93a-d073-48ac-88db-c46bf3e10beb.jpg?1739804185"
    }
}
