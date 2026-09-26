package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Inti, Seneschal of the Sun
 * {1}{R}
 * Legendary Creature — Human Knight
 * 2/2
 *
 * Whenever you attack, you may discard a card. When you do, put a +1/+1 counter on
 * target attacking creature. It gains trample until end of turn.
 * Whenever you discard one or more cards, exile the top card of your library. You may
 * play that card until your next end step.
 *
 * Ability 1 is a "When you do" reflexive: the optional discard is the action, and the
 * reflexive ability — which targets an attacking creature chosen as it goes on the stack —
 * only fires if a card is actually discarded ([ReflexiveTriggerEffect]).
 *
 * Ability 2 uses `Triggers.you.discards(batch = true)` — the batch discard trigger (CR 603.2c):
 * discarding several cards in one discard event fires it once, matching the printed "one or
 * more cards" wording (a per-card trigger would impulse-exile once per discarded card). The
 * impulse-exile grants play permission until the controller's next end step
 * ([MayPlayExpiry.UntilNextEndStep] — this turn's end step counts on your own turn).
 */
val IntiSeneschalOfTheSun = card("Inti, Seneschal of the Sun") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Knight"
    power = 2
    toughness = 2
    oracleText = "Whenever you attack, you may discard a card. When you do, put a +1/+1 counter on target " +
        "attacking creature. It gains trample until end of turn.\n" +
        "Whenever you discard one or more cards, exile the top card of your library. You may play that " +
        "card until your next end step."

    // "Whenever you attack, you may discard a card. When you do, put a +1/+1 counter on
    // target attacking creature. It gains trample until end of turn."
    triggeredAbility {
        trigger = Triggers.you.attacks()
        effect = Effects.ReflexiveTrigger(
            action = Effects.Discard(1),
            optional = true) {
            val attackingCreature = target(TargetFilter.AttackingCreature)
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, attackingCreature) then
                Effects.GrantKeyword(Keyword.TRAMPLE, attackingCreature, Duration.EndOfTurn)
        }
    }

    // "Whenever you discard one or more cards, exile the top card of your library. You may
    // play that card until your next end step."
    triggeredAbility {
        trigger = Triggers.you.discards(batch = true)
        effect = Effects.Pipeline {
            val intiExiled = gather(CardSource.TopOfLibrary(1))
            exile(intiExiled)
            run(Effects.GrantMayPlayFromExile(intiExiled, MayPlayExpiry.UntilNextEndStep))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "156"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa7a55aa-ae61-4933-b7a4-dcc55dac6fcd.jpg?1782694483"
    }
}
