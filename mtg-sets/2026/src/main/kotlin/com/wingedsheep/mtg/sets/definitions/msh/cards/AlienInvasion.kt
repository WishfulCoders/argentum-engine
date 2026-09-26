package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step

/**
 * Alien Invasion — Marvel Super Heroes #200
 * {1}{R}{R}{G} · Enchantment
 *
 * At the beginning of combat on your turn, create a 1/1 red Alien creature token with haste and
 * "This token attacks each combat if able." Put a +1/+1 counter on it for each invasion counter on
 * this enchantment, then put an invasion counter on this enchantment.
 *
 * Modeling notes:
 *  - The token's printed "attacks each combat if able" is a [MustAttack] static scoped to the token
 *    itself ([GroupFilter.source]), granted through `CreateTokenEffect.staticAbilities`.
 *  - The freshly created token is addressed through the well-known [CREATED_TOKENS] pipeline
 *    collection (the Zanarkand / Emil idiom), so "put a +1/+1 counter on **it**" lands on this
 *    combat's Alien and nothing else.
 *  - Order matters and is exactly the printed order: the +1/+1 counters are sized by
 *    [DynamicAmounts.countersOnSelf]`(`[CounterType.INVASION]`)` read *before* the increment, so the
 *    first Alien is a 1/1, the second a 2/2, and so on.
 */
val AlienInvasion = card("Alien Invasion") {
    manaCost = "{1}{R}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Enchantment"
    oracleText = "At the beginning of combat on your turn, create a 1/1 red Alien creature token " +
        "with haste and \"This token attacks each combat if able.\" Put a +1/+1 counter on it for " +
        "each invasion counter on this enchantment, then put an invasion counter on this enchantment."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf(Subtype.ALIEN.value),
            keywords = setOf(Keyword.HASTE),
            staticAbilities = listOf(MustAttack(GroupFilter.source())),
            imageUri = "https://cards.scryfall.io/normal/front/e/c/eca87cbb-5958-4775-93ce-b1d4c7ef3a99.jpg?1783902802",
        ) then
            Effects.AddCountersToCollection(
                CREATED_TOKENS,
                CounterType.PLUS_ONE_PLUS_ONE,
                DynamicAmounts.countersOnSelf(CounterType.INVASION),
            ) then
            Effects.AddCounters(CounterType.INVASION, 1, EffectTarget.Self)
        description = "At the beginning of combat on your turn, create a 1/1 red Alien creature " +
            "token with haste and \"This token attacks each combat if able.\" Put a +1/+1 counter " +
            "on it for each invasion counter on this enchantment, then put an invasion counter on " +
            "this enchantment."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "200"
        artist = "Björn Barends"
        flavorText = "\"New York? Again? Too afraid of Asgard, I see.\"\n—Thor"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f46a7158-5807-4750-a1f3-853aabb56b99.jpg?1783902907"
    }
}
