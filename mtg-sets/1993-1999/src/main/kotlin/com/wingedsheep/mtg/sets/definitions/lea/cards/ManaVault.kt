package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mana Vault
 * {1}
 * Artifact
 * This artifact doesn't untap during your untap step.
 * At the beginning of your upkeep, you may pay {4}. If you do, untap this artifact.
 * At the beginning of your draw step, if this artifact is tapped, it deals 1 damage to you.
 * {T}: Add {C}{C}{C}.
 *
 * "Doesn't untap" is [AbilityFlag.DOESNT_UNTAP] (as on Basalt Monolith); the upkeep untap is the
 * Brass Man [Effects.MayPay] shape. The draw-step damage is an intervening "if" — checked both when
 * the trigger would fire and again on resolution — via [Conditions.SourceIsTapped], so untapping
 * Mana Vault in response (or paying in upkeep) spares the damage.
 */
val ManaVault = card("Mana Vault") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "This artifact doesn't untap during your untap step.\n" +
        "At the beginning of your upkeep, you may pay {4}. If you do, untap this artifact.\n" +
        "At the beginning of your draw step, if this artifact is tapped, it deals 1 damage to you.\n" +
        "{T}: Add {C}{C}{C}."

    flags(AbilityFlag.DOESNT_UNTAP)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.MayPay(ManaCost.parse("{4}"), Effects.Untap(EffectTarget.Self))
        description = "At the beginning of your upkeep, you may pay {4}. If you do, untap this artifact."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.DRAW)
        interveningIf = Conditions.SourceIsTapped
        effect = Effects.DealDamage(1, EffectTarget.Controller)
        description = "At the beginning of your draw step, if this artifact is tapped, it deals 1 damage to you."
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(3)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "259"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19499cb7-eccb-4e69-af32-6002d447a160.jpg?1783948664"
    }
}
