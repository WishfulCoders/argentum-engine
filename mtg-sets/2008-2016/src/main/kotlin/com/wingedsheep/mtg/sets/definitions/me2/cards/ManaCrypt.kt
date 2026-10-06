package com.wingedsheep.mtg.sets.definitions.me2.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mana Crypt
 * {0}
 * Artifact
 * At the beginning of your upkeep, flip a coin. If you lose the flip, this artifact deals 3 damage
 * to you.
 * {T}: Add {C}{C}.
 *
 * The flip and the damage happen inside one resolution of the trigger ([Effects.FlipCoin]'s lost
 * branch), so no player can act between the flip and the damage (2020-08-07 ruling).
 */
val ManaCrypt = card("Mana Crypt") {
    manaCost = "{0}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "At the beginning of your upkeep, flip a coin. If you lose the flip, this artifact " +
        "deals 3 damage to you.\n{T}: Add {C}{C}."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.FlipCoin(
            wonEffect = null,
            lostEffect = Effects.DealDamage(3, EffectTarget.PlayerRef(Player.You)),
        )
        description = "At the beginning of your upkeep, flip a coin. If you lose the flip, this " +
            "artifact deals 3 damage to you."
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(2)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "214"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28ececd4-2a6d-49dd-bc52-4fdb1e0b1dba.jpg?1783942593"
        ruling(
            "2020-08-07",
            "No player may choose to take actions between determining the result of the flip " +
                "and damage being dealt if you lost the flip.",
        )
    }
}
