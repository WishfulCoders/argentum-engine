package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Agate Instigator — Bloomburrow Commander #21
 * {1}{R} · Creature — Lizard Rogue · 1/3
 *
 * Offspring {1}{R}
 * Whenever another creature you control enters, this creature deals 1 damage to each opponent.
 *
 * The offspring token copies the damage trigger, so with both on the battlefield each later
 * creature pings each opponent twice — and the original sees the token itself enter.
 */
val AgateInstigator = card("Agate Instigator") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Lizard Rogue"
    power = 1
    toughness = 3
    oracleText = "Offspring {1}{R} (You may pay an additional {1}{R} as you cast this spell. If you do, " +
        "when this creature enters, create a 1/1 token copy of it.)\n" +
        "Whenever another creature you control enters, this creature deals 1 damage to each opponent."

    keywordAbility(KeywordAbility.offspring("{1}{R}"))

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasKicked
        effect = Effects.CreateTokenCopyOfSelf(overridePower = 1, overrideToughness = 1)
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "21"
        artist = "Quintin Gleim"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f838603-f7cd-497d-8d2b-d4cca03c1af7.jpg?1783910732"
    }
}
