package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Showdown of the Skalds — Kaldheim #229
 * {2}{R}{W} · Enchantment — Saga · Rare
 *
 * I — Exile the top four cards of your library. Until the end of your next turn, you may play
 *     those cards.
 * II, III — Whenever you cast a spell this turn, put a +1/+1 counter on target creature you control.
 */
val ShowdownOfTheSkalds = card("Showdown of the Skalds") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "WR"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Exile the top four cards of your library. Until the end of your next turn, you may play those cards.\n" +
        "II, III — Whenever you cast a spell this turn, put a +1/+1 counter on target creature you control."

    sagaChapter(1) {
        effect = Patterns.Exile.impulse(4, MayPlayExpiry.UntilEndOfNextTurn)
    }
    sagaChapter(2) {
        effect = skaldsCounterTrigger()
    }
    sagaChapter(3) {
        effect = skaldsCounterTrigger()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "229"
        artist = "Steven Belledin"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d9d840e-1f13-44e3-a4de-903cfa58a346.jpg?1783928190"
        ruling("2021-02-05", "The cards you exile because of the chapter I ability are exiled face up.")
        ruling("2021-02-05", "Playing a card this way follows the normal rules for playing the card. You must pay its costs, and you must follow all applicable timing rules. For example, if one of the cards is a sorcery card, you can cast it only during your main phase while the stack is empty.")
        ruling("2021-02-05", "Under normal circumstances, you can play a land from among the exiled cards only if you haven't played a land yet that turn.")
        ruling("2021-02-05", "Any cards you don't play by the end of your next turn will remain exiled.")
    }
}

/**
 * "Whenever you cast a spell this turn, put a +1/+1 counter on target creature you control." A
 * fresh delayed trigger backs each of chapters II and III; the target is chosen as each copy fires.
 */
private fun skaldsCounterTrigger() = Effects.CreateDelayedTrigger(
    trigger = Triggers.you.casts(),
    fireOnce = false,
    expiry = DelayedTriggerExpiry.EndOfTurn
) {
    val creature = target(TargetFilter.CreatureYouControl)
    effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
}
