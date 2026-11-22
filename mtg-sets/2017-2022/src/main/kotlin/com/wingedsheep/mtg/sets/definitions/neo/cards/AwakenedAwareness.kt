package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Awakened Awareness — Kamigawa: Neon Dynasty #47
 * {X}{U}{U} · Enchantment — Aura
 *
 * Enchant artifact or creature
 * When this Aura enters, put X +1/+1 counters on enchanted permanent.
 * As long as enchanted permanent is a creature, it has base power and toughness 1/1.
 *
 * The enters trigger reads X as [DynamicAmounts.castX] — the spell is gone by the time the
 * trigger resolves, so the X rides onto the Aura permanent's cast-choices bag. The base-P/T static
 * (layer 7b) is gated on the host's *current* type, so a noncreature artifact (or an uncrewed
 * Vehicle) is untouched until it becomes a creature; counters still apply on top of the 1/1.
 */
val AwakenedAwareness = card("Awakened Awareness") {
    manaCost = "{X}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant artifact or creature\n" +
        "When this Aura enters, put X +1/+1 counters on enchanted permanent.\n" +
        "As long as enchanted permanent is a creature, it has base power and toughness 1/1."

    auraTarget = TargetObject(filter = TargetFilter.CreatureOrArtifact)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.AddDynamicCounters(
            CounterType.PLUS_ONE_PLUS_ONE,
            DynamicAmounts.castX(),
            EffectTarget.EnchantedPermanent
        )
        description = "When this Aura enters, put X +1/+1 counters on enchanted permanent."
    }

    staticAbility {
        condition = Conditions.EnchantedPermanentMatches(GameObjectFilter.Creature)
        ability = SetBasePowerToughnessStatic(1, 1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "47"
        artist = "Tran Nguyen"
        imageUri = "https://cards.scryfall.io/normal/front/5/1/51378e10-2f17-445b-97e9-5c373234ba52.jpg?1783923906"

        ruling(
            "2022-02-18",
            "Awakened Awareness will overwrite any previous effects that set the enchanted creature's " +
                "power and toughness to specific numbers. Effects that otherwise modify the enchanted " +
                "creature's power and toughness will still apply no matter when they took effect. The " +
                "same is true for +1/+1 counters."
        )
        ruling(
            "2022-02-18",
            "The enchanted creature's toughness becomes 1 before the triggered ability that puts " +
                "counters on it goes on the stack. If it has damage equal to or greater than its " +
                "toughness at that point, it will be destroyed before the triggered ability that puts " +
                "counters on it resolves."
        )
        ruling(
            "2022-02-18",
            "If Awakened Awareness is enchanting a Vehicle that is currently a creature or it becomes " +
                "a creature later, its base power and toughness will be 1/1, not the power and " +
                "toughness printed on the Vehicle."
        )
    }
}
