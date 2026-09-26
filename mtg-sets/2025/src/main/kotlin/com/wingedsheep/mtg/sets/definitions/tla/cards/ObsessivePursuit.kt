package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Obsessive Pursuit
 * {1}{B}
 * Enchantment
 *
 * When this enchantment enters and at the beginning of your upkeep, you lose 1 life
 * and create a Clue token. (It's an artifact with "{2}, Sacrifice this token: Draw a card.")
 * Whenever you attack, put X +1/+1 counters on target attacking creature, where X is the
 * number of permanents you've sacrificed this turn. If X is three or more, that creature
 * gains lifelink until end of turn.
 */
val ObsessivePursuit = card("Obsessive Pursuit") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters and at the beginning of your upkeep, you lose 1 life " +
        "and create a Clue token. (It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")\n" +
        "Whenever you attack, put X +1/+1 counters on target attacking creature, where X is the number " +
        "of permanents you've sacrificed this turn. If X is three or more, that creature gains lifelink " +
        "until end of turn."

    // "When this enchantment enters and at the beginning of your upkeep" — one ability that triggers
    // off two events; modeled as two triggered abilities sharing the same effect.
    val loseLifeAndClue = Effects.LoseLife(1, EffectTarget.Controller) then Effects.CreateClue()

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = loseLifeAndClue
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = loseLifeAndClue
    }

    triggeredAbility {
        trigger = Triggers.you.attacks()
        val attacker = target(TargetFilter.AttackingCreature)
        effect = Effects.AddDynamicCounters(
            CounterType.PLUS_ONE_PLUS_ONE,
            DynamicAmounts.permanentsSacrificedThisTurn(),
            attacker,
        ) then
            // X is the per-controller "permanents sacrificed this turn" count; lifelink only when X >= 3.
            Effects.If(
                condition = Conditions.YouSacrificedPermanentsThisTurn(atLeast = 3),
                then = Effects.GrantKeyword(Keyword.LIFELINK, attacker),
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "112"
        artist = "Ichiko Milk Tei"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e837e29c-d241-43c8-8f45-05056e082b60.jpg?1764120772"
    }
}
