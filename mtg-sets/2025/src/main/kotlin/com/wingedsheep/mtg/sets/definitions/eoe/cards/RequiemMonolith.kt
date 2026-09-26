package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Requiem Monolith
 * {2}{B}
 * Artifact
 * {T}: Until end of turn, target creature gains "Whenever this creature is dealt damage,
 * you draw that many cards and lose that much life." That creature's controller may have
 * this artifact deal 1 damage to it. Activate only as a sorcery.
 */
val RequiemMonolith = card("Requiem Monolith") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Artifact"
    oracleText = "{T}: Until end of turn, target creature gains \"Whenever this creature is dealt damage, you draw that many cards and lose that much life.\" That creature's controller may have this artifact deal 1 damage to it. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Tap
        timing = TimingRule.SorcerySpeed
        val creature = target(TargetFilter.Creature)

        val damage = DynamicAmounts.triggerDamageAmount()
        val grantedAbility = TriggeredAbility.create(
            trigger = Triggers.self.isDealtDamage(),
            effect = Effects.DrawCards(damage, EffectTarget.Controller) then
                Effects.LoseLife(damage, EffectTarget.Controller),
            descriptionOverride = "Whenever this creature is dealt damage, you draw that many cards and lose that much life"
        )

        effect = Effects.GrantTriggeredAbility(ability = grantedAbility, target = creature) then
            Effects.May(
                effect = Effects.DealDamage(amount = 1, target = creature, damageSource = EffectTarget.Self),
                descriptionOverride = "Have Requiem Monolith deal 1 damage to that creature?",
                decisionMaker = EffectTarget.TargetController
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "113"
        artist = "Warren Mahy"
        imageUri = "https://cards.scryfall.io/normal/front/8/3/837d710a-652f-4c60-a52d-d786231160a4.jpg?1752947010"
        ruling("2025-07-25", "If lethal damage is dealt to a creature that has been granted the listed ability by Requiem Monolith's ability, the granted ability will still trigger.")
        ruling("2025-07-25", "If multiple sources deal damage to a creature that has been granted the listed ability by Requiem Monolith's ability (probably because multiple creatures blocked that creature), the granted ability triggers only once. When it resolves, the creature's controller draws cards and loses life equal to the total amount of damage dealt to that creature by those sources.")
    }
}
