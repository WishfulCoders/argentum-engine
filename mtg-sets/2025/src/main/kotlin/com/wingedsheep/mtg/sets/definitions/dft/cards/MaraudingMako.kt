package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Marauding Mako — Aetherdrift #138
 * {R} · Creature — Shark Pirate · 1/1
 *
 * Whenever you discard one or more cards, put that many +1/+1 counters on this creature.
 * Cycling {2}
 *
 * The payoff is batch-worded (CR 603.2c), so it uses `Triggers.you.discards(batch = true)` — one trigger
 * per discard *event* however many cards it held — and reads the batch size back through
 * [ContextPropertyKey.TRIGGER_DISCARD_COUNT] for "that many". Same shape as its set-mate Magmakin
 * Artillerist: discarding three cards to one effect adds 3 counters, three separate discards add 1
 * each.
 *
 * Cycling this card never feeds its own trigger — cycling discards it *from hand*, and the
 * battlefield-only discard trigger doesn't function from hand. Cycling anything else while the
 * Mako is on the battlefield does add a counter.
 */
val MaraudingMako = card("Marauding Mako") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Shark Pirate"
    power = 1
    toughness = 1
    oracleText = "Whenever you discard one or more cards, put that many +1/+1 counters on this " +
        "creature.\n" +
        "Cycling {2} ({2}, Discard this card: Draw a card.)"

    triggeredAbility {
        trigger = Triggers.you.discards(batch = true)
        effect = Effects.AddDynamicCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            amount = DynamicAmounts.triggerDiscardCount(),
            target = EffectTarget.Self,
        )
        description = "Whenever you discard one or more cards, put that many +1/+1 counters on " +
            "this creature."
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "138"
        artist = "Alix Branwyn"
        flavorText = "\"What a bunch of junk. I'll take the lot.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9efbfd67-e0f5-43e0-9fff-1eb4a2bed0d8.jpg?1783907879"
    }
}
