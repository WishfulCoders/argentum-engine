package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Waterbender Ascension
 * {1}{U}
 * Enchantment
 *
 * Whenever a creature you control deals combat damage to a player, put a quest counter on this
 * enchantment. Then if it has four or more quest counters on it, draw a card.
 * Waterbend {4}: Target creature can't be blocked this turn. (While paying a waterbend cost, you
 * can tap your artifacts and creatures to help. Each one pays for {1}.)
 *
 * Modeling notes:
 *  - The combat-damage trigger fires for any creature you control (binding ANY, source-filtered)
 *    via `Triggers.<subject>.dealsDamage(to, damageType, requireExcess, batch, requires)` — same shape as Impostor Syndrome.
 *  - Intervening-"if" payoff (CR 603.4): putting the quest counter is mandatory; only if the
 *    enchantment then has four or more quest counters does the draw happen. The counter add is
 *    sequenced first, then [Effects.If] gates the draw on the live count
 *    (`SourceCounterCountAtLeast`) — mirrors Earthbender Ascension's quest-counter pattern.
 *  - Waterbend is a keyword cost ({4}, payable by tapping your artifacts/creatures) modeled with
 *    `hasWaterbend = true` like Geyser Leaper; the can't-be-blocked grant defaults to end of turn.
 */
val WaterbenderAscension = card("Waterbender Ascension") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control deals combat damage to a player, put a quest counter on this enchantment. Then if it has four or more quest counters on it, draw a card.\n" +
        "Waterbend {4}: Target creature can't be blocked this turn. (While paying a waterbend cost, you can tap your artifacts and creatures to help. Each one pays for {1}.)"

    // Whenever a creature you control deals combat damage to a player, put a quest counter on this
    // enchantment. Then if it has four or more quest counters on it, draw a card.
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.AddCounters(CounterType.QUEST, 1, EffectTarget.Self) then
            Effects.If(
                condition = Conditions.SourceCounterCountAtLeast(CounterType.QUEST, 4),
                then = Effects.DrawCards(1)
            )
        description = "Whenever a creature you control deals combat damage to a player, put a quest counter on this enchantment. Then if it has four or more quest counters on it, draw a card."
    }

    // Waterbend {4}: Target creature can't be blocked this turn.
    activatedAbility {
        cost = Costs.Mana("{4}")
        hasWaterbend = true
        val t = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, t)
        description = "Waterbend {4}: Target creature can't be blocked this turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "79"
        artist = "Takeuchi Moto"
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3f57e0f9-e232-489c-b991-d0d23f75d8dd.jpg?1764120533"
    }
}
