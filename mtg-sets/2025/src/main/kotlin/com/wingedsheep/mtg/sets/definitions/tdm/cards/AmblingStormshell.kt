package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Ambling Stormshell — Tarkir: Dragonstorm #37
 * {3}{U}{U} · Creature — Turtle · 5/9
 *
 * Ward {2}
 * Whenever this creature attacks, put three stun counters on it and draw three cards.
 * Whenever you cast a Turtle spell, untap this creature.
 *
 * Composed entirely from existing primitives: [WardCost.Mana] for the ward cost,
 * [Effects.AddCounters] + [Effects.DrawCards] on the attack trigger, and an untap-self
 * effect keyed to the `Triggers.you.casts(…Turtle…)` Turtle-spell trigger. The three stun
 * counters keep the shell tapped after attacking (CR 122.1c stun-counter replacement);
 * casting a Turtle spell untaps it so it can attack again.
 */
val AmblingStormshell = card("Ambling Stormshell") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Turtle"
    power = 5
    toughness = 9
    oracleText = "Ward {2}\n" +
        "Whenever this creature attacks, put three stun counters on it and draw three cards. " +
        "(If a permanent with a stun counter would become untapped, remove one from it instead.)\n" +
        "Whenever you cast a Turtle spell, untap this creature."

    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.AddCounters(CounterType.STUN, 3, EffectTarget.Self) then Effects.DrawCards(3)
        description = "Whenever this creature attacks, put three stun counters on it and draw three cards."
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withSubtype(Subtype.TURTLE))
        effect = Effects.Untap(EffectTarget.Self)
        description = "Whenever you cast a Turtle spell, untap this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "37"
        artist = "Carlos Palma Cruchaga"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c74d4a57-0f66-4965-9ed7-f88a08aa1d15.jpg?1743204109"
    }
}
