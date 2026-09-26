package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Triggers

/**
 * Belligerent Yearling
 * {1}{R}
 * Creature — Dinosaur
 * 3/2
 * Trample
 * Whenever another Dinosaur you control enters, you may have this creature's base power become
 * equal to that creature's power until end of turn.
 *
 * The trigger uses TriggerBinding.OTHER scoped to Creature.withSubtype("Dinosaur").youControl()
 * entering the battlefield — "another Dinosaur you control". The payoff is a Effects.May wrapping
 * SetBasePower(Self, EntityProperty(TriggeringEntity, Power), EndOfTurn): Layer 7b set-base-power that
 * reads the entering Dinosaur's projected power at resolution time, lasting until cleanup (EndOfTurn).
 */
val BelligerentYearling = card("Belligerent Yearling") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dinosaur"
    power = 3
    toughness = 2
    oracleText = "Trample\nWhenever another Dinosaur you control enters, you may have this creature's base power become equal to that creature's power until end of turn."

    keywords(Keyword.TRAMPLE)

    // "Whenever another Dinosaur you control enters, you may have this creature's base power
    // become equal to that creature's power until end of turn."
    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.withSubtype(Subtype.DINOSAUR).youControl()).enters()
        effect = Effects.May(
            effect = Effects.SetBasePower(
                target = EffectTarget.Self,
                power = DynamicAmounts.triggeringPower(),
                duration = Duration.EndOfTurn
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "133"
        artist = "Maxime Minard"
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b2debca-8535-4cf6-a461-c268faaacaae.jpg?1782694502"
    }
}
