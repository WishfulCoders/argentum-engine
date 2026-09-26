package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Zoraline, Cosmos Caller
 * {1}{W}{B}
 * Legendary Creature — Bat Cleric
 * 3/3
 *
 * Flying, vigilance
 * Whenever a Bat you control attacks, you gain 1 life.
 * Whenever Zoraline enters or attacks, you may pay {W}{B} and 2 life.
 * When you do, return target nonland permanent card with mana value 3
 * or less from your graveyard to the battlefield with a finality counter on it.
 */
val ZoralineCosmosCaller = card("Zoraline, Cosmos Caller") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Bat Cleric"
    power = 3
    toughness = 3
    oracleText = "Flying, vigilance\n" +
        "Whenever a Bat you control attacks, you gain 1 life.\n" +
        "Whenever Zoraline enters or attacks, you may pay {W}{B} and 2 life. " +
        "When you do, return target nonland permanent card with mana value 3 or less " +
        "from your graveyard to the battlefield with a finality counter on it."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    // Whenever a Bat you control attacks, you gain 1 life.
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withSubtype("Bat").youControl()).attacks()
        effect = Effects.GainLife(1)
    }

    // Whenever Zoraline enters the battlefield, you may pay {W}{B} and 2 life.
    // When you do, return target nonland permanent card with MV ≤ 3 from your graveyard
    // to the battlefield with a finality counter on it.
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = zoralineReanimateEffect()
    }

    // Whenever Zoraline attacks, same effect.
    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = zoralineReanimateEffect()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "242"
        artist = "Justin Gerard"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7f99fd5-5298-4b27-923d-9d31203c931a.jpg?1721427252"

        ruling("2024-07-26", "You don't choose a target for Zoraline's last ability at the time it triggers. Rather, a second \"reflexive\" ability triggers when you pay {W}{B} and 2 life this way. You choose a target for this ability as it goes on the stack.")
        ruling("2024-07-26", "If a permanent with a finality counter on it would be put into a graveyard from the battlefield, exile it instead.")
        ruling("2024-07-26", "Finality counters don't stop permanents from going to zones other than the graveyard from the battlefield.")
        ruling("2024-07-26", "Multiple finality counters on a single permanent are redundant.")
    }
}

/**
 * Zoraline's "you may pay {W}{B} and 2 life. When you do, return target …" — the payment is the
 * [Effects.MayPay] gate (only offered when both halves are affordable), and the return is a real
 * reflexive triggered ability (CR 603.12) whose target is chosen as it goes on the stack, after the
 * payment, per the 2024-07-26 ruling. The [ReflexiveTriggerEffect]'s own action is empty because
 * the "when you do" is the payment itself (the Fire Lord Sozin shape).
 */
private fun zoralineReanimateEffect() = Effects.MayPay(
    cost = Effects.PayMana("{W}{B}") then Effects.PayLife(2),
    then = Effects.ReflexiveTrigger(
        action = Effects.Nothing,
        optional = false,
        descriptionOverride = "return target nonland permanent card with mana value 3 or less " +
            "from your graveyard to the battlefield with a finality counter on it"
    ) {
        val nonlandPermanent = target(
            TargetFilter(
                GameObjectFilter.NonlandPermanent.ownedByYou().manaValueAtMost(3),
                zone = Zone.GRAVEYARD
            ),
        )
        effect = Effects.PutOntoBattlefieldFromGraveyard(nonlandPermanent) then
            Effects.AddCounters(CounterType.FINALITY, 1, nonlandPermanent)
    }
)
