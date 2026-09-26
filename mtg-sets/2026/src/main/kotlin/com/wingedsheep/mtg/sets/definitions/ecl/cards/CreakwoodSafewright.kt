package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step

/**
 * Creakwood Safewright
 * {1}{B}
 * Creature — Elf Warrior
 * 5/5
 *
 * This creature enters with three -1/-1 counters on it.
 * At the beginning of your end step, if there is an Elf card in your graveyard and this
 * creature has a -1/-1 counter on it, remove a -1/-1 counter from this creature.
 */
val CreakwoodSafewright = card("Creakwood Safewright") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Elf Warrior"
    power = 5
    toughness = 5
    oracleText = "This creature enters with three -1/-1 counters on it.\n" +
        "At the beginning of your end step, if there is an Elf card in your graveyard and this " +
        "creature has a -1/-1 counter on it, remove a -1/-1 counter from this creature."

    replacementEffect(EntersWithCounters(
        counterType = CounterType.MINUS_ONE_MINUS_ONE,
        count = 3,
        selfOnly = true
    ))

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.All(
            Conditions.GraveyardContainsSubtype(Subtype.ELF),
            Conditions.SourceHasCounter(CounterType.MINUS_ONE_MINUS_ONE)
        )
        effect = Effects.RemoveCounters(CounterType.MINUS_ONE_MINUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "96"
        artist = "Heather Hudson"
        flavorText = "\"I will endure grasping vine and prickling bramble to preserve even one flower.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3bcc24cf-776a-4182-bf77-a611ad90b28f.jpg?1767957099"
    }
}
