package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hangarback Walker
 * {X}{X}
 * Artifact Creature — Construct
 * 0/0
 * This creature enters with X +1/+1 counters on it.
 * When this creature dies, create a 1/1 colorless Thopter artifact creature token with flying for
 * each +1/+1 counter on this creature.
 * {1}, {T}: Put a +1/+1 counter on this creature.
 *
 * Walking Ballista's X-counter entry plus Hooded Hydra's dies trigger, which counts the +1/+1
 * counters from last-known information (the creature is already in the graveyard on resolution).
 */
val HangarbackWalker = card("Hangarback Walker") {
    manaCost = "{X}{X}"
    typeLine = "Artifact Creature — Construct"
    power = 0
    toughness = 0
    oracleText = "This creature enters with X +1/+1 counters on it.\n" +
        "When this creature dies, create a 1/1 colorless Thopter artifact creature token with flying " +
        "for each +1/+1 counter on this creature.\n" +
        "{1}, {T}: Put a +1/+1 counter on this creature."

    replacementEffect(EntersWithDynamicCounters(count = DynamicAmounts.xValue()))

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            count = DynamicAmounts.lastKnownPlusOneCounters(),
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/7/5/75fa5fab-1076-443b-ba27-46d10770883d.jpg?1783938293",
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "229"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/7/9/791c21fb-fc78-4106-9a42-abc73f41ab8b.jpg?1783938311"
        ruling(
            "2015-06-22",
            "The value of each X in Hangarback Walker's mana cost must be equal. For example, if X is 2, " +
                "you'll pay {4} to cast Hangarback Walker and it will enter the battlefield with two +1/+1 counters on it.",
        )
        ruling(
            "2015-06-22",
            "If enough -1/-1 counters are put on Hangarback Walker at the same time to make its toughness 0 or " +
                "less, the number of +1/+1 counters on it before it got any -1/-1 counters will be used to determine " +
                "how many Thopter tokens you get.",
        )
    }
}
