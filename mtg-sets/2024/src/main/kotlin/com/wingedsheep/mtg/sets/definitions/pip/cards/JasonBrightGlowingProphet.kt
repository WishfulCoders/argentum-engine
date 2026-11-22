package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

/**
 * Jason Bright, Glowing Prophet — Fallout #32.
 *
 * "If its power was different from its base power" reads the dead creature as it last existed on
 * the battlefield (CR 608.2h): its last-known power against its last-known base power, so a copy or
 * a "base power is N" effect it died under counts as its base, and a +1/+1 counter or an anthem
 * doesn't.
 */
val JasonBrightGlowingProphet = card("Jason Bright, Glowing Prophet") {
    manaCost = "{2}{U}"
    typeLine = "Legendary Creature — Zombie Mutant Advisor"
    power = 2
    toughness = 3
    oracleText = "Whenever a Zombie or Mutant you control dies, if its power was different from its base power, draw a card.\n" +
        "Come Fly With Me — {2}, Sacrifice a creature: Put a +1/+1 counter on target creature you control. It gains flying until end of turn."
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withAnySubtype("Zombie", "Mutant").youControl()).dies()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.propertyOf(EffectTarget.TriggeringEntity, EntityNumericProperty.Power),
            ComparisonOperator.NEQ,
            DynamicAmounts.propertyOf(EffectTarget.TriggeringEntity, EntityNumericProperty.BasePower),
        )
        effect = Effects.DrawCards(1)
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Sacrifice(GameObjectFilter.Creature))
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.GrantKeyword(Keyword.FLYING, creature)
        description = "Come Fly With Me — Put a +1/+1 counter on target creature you control. It gains flying until end of turn."
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "32"
        artist = "Tomas Duchek"
        imageUri = "https://cards.scryfall.io/normal/front/4/8/48313997-fd94-486a-bf30-e9143a155814.jpg?1783912413"
        ruling("2024-03-08", "Normally, a creature’s base power is the power printed on the card, or, for a token, the power set by the effect that created it. If another effect sets a creature’s power to a specific number or value, that becomes its base power. If an effect modifies a creature’s power without setting it, that is not included when determining its base power.")
        ruling("2024-03-08", "If a creature has a characteristic-defining ability that sets its power, indicated with a * or similar in the power and toughness box, that ability is taken into account when determining its base power.")
        ruling("2024-03-08", "Some creatures have base power 0 and an ability that gives them a bonus based on some criteria. Those are not characteristic-defining abilities, and that ability doesn’t change its base power. Such a creature will have power different from its base power if that ability causes its power to be anything other than 0.")
    }
}
