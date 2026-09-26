package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Heroic Feast — Marvel Super Heroes #172
 * {2}{G} · Enchantment · Rare
 *
 * When this enchantment enters, create a Food token.
 * Whenever you gain life, choose up to that many target creatures you control. Put a +1/+1
 * counter on each of them.
 *
 * The life-gain payoff is Elrond, Master of Healing's shape: a variable-count target requirement
 * whose cap is the trigger's own context value ([ContextPropertyKey.TRIGGER_LIFE_GAINED]),
 * snapshotted when the ability goes on the stack, with `optional = true` supplying the "up to"
 * floor of zero. [ForEachTargetEffect] then applies one +1/+1 counter per chosen target, so the
 * count of counters is owned by the targets rather than duplicated in the effect — and a target
 * that became illegal before resolution simply drops out (CR 608.2b) without affecting the rest.
 *
 * "Whenever you gain life" fires once per life-gain *event*, not once per point (CR 118.5), so a
 * single 3-life gain is one trigger with a cap of three.
 */
val HeroicFeast = card("Heroic Feast") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, create a Food token. (It's an artifact with " +
        "\"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n" +
        "Whenever you gain life, choose up to that many target creatures you control. Put a " +
        "+1/+1 counter on each of them."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateFood()
        description = "When this enchantment enters, create a Food token."
    }

    triggeredAbility {
        trigger = Triggers.you.gainsLife()
        targets(
            TargetFilter(GameObjectFilter.Creature.youControl()),
            optional = true,
            dynamicMaxCount = DynamicAmounts.triggerLifeGained(),
        )
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0))
        )
        description = "Whenever you gain life, choose up to that many target creatures you " +
            "control. Put a +1/+1 counter on each of them."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "172"
        artist = "Javier Charro"
        flavorText = "The Thing belched. \"I told youse guys. Yancy Street has the best sandwiches.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32d05c3d-cf03-492e-a956-2e77c36e36c4.jpg?1783902917"
    }
}
