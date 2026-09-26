package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Bristlebane Battler
 * {1}{G}
 * Creature — Kithkin Soldier
 * 6/6
 *
 * Trample, ward {2}
 * This creature enters with five -1/-1 counters on it.
 * Whenever another creature you control enters while this creature has a -1/-1 counter on it,
 * remove a -1/-1 counter from this creature.
 */
val BristlebaneBattler = card("Bristlebane Battler") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Kithkin Soldier"
    power = 6
    toughness = 6
    oracleText = "Trample, ward {2}\n" +
        "This creature enters with five -1/-1 counters on it.\n" +
        "Whenever another creature you control enters while this creature has a -1/-1 counter on it, " +
        "remove a -1/-1 counter from this creature."

    keywords(Keyword.TRAMPLE, Keyword.WARD)
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.MINUS_ONE_MINUS_ONE,
            count = 5,
            selfOnly = true,
        )
    )

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        triggerRestriction = Conditions.SourceHasCounter(CounterType.MINUS_ONE_MINUS_ONE)
        effect = Effects.RemoveCounters(CounterType.MINUS_ONE_MINUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "168"
        artist = "Steve Ellis"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c857fa32-1b5d-4139-8809-b4d0df44b472.jpg?1767732790"
    }
}
