package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Syr Konrad, the Grim
 * {3}{B}{B}
 * Legendary Creature — Human Knight
 * 5/4
 * Whenever another creature dies, or a creature card is put into a graveyard from anywhere other
 * than the battlefield, or a creature card leaves your graveyard, Syr Konrad deals 1 damage to each
 * opponent.
 * {1}{B}: Each player mills a card.
 *
 * One printed ability with three trigger events, authored as three triggered abilities with the
 * same effect — the events are disjoint, so no single event fires it twice:
 *  - "another creature dies" is an OTHER-bound battlefield-to-graveyard trigger, so it uses the
 *    leaves-the-battlefield look-back and fires for each creature that dies at the same time as
 *    Syr Konrad (ruling), tokens included;
 *  - "a creature card is put into a graveyard from anywhere other than the battlefield" is
 *    `to = GRAVEYARD, excludeFrom = BATTLEFIELD` over any graveyard — a discard, a mill, a
 *    countered creature spell;
 *  - "a creature card leaves your graveyard" is `from = GRAVEYARD` over creature cards you own,
 *    per card rather than batched.
 */
val SyrKonradTheGrim = card("Syr Konrad, the Grim") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Knight"
    power = 5
    toughness = 4
    oracleText = "Whenever another creature dies, or a creature card is put into a graveyard from anywhere other than the battlefield, or a creature card leaves your graveyard, Syr Konrad deals 1 damage to each opponent.\n" +
        "{1}{B}: Each player mills a card. (They each put the top card of their library into their graveyard.)"

    val pingEachOpponent = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
    val abilityText = "Whenever another creature dies, or a creature card is put into a graveyard from anywhere " +
        "other than the battlefield, or a creature card leaves your graveyard, Syr Konrad deals 1 damage to each opponent."

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Creature,
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.OTHER,
        )
        effect = pingEachOpponent
        description = abilityText
    }

    triggeredAbility {
        trigger = TriggerSpec(
            event = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature,
                to = Zone.GRAVEYARD,
                excludeFrom = Zone.BATTLEFIELD,
            ),
            binding = TriggerBinding.ANY,
        )
        effect = pingEachOpponent
        description = abilityText
    }

    triggeredAbility {
        trigger = TriggerSpec(
            event = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.ownedByYou(),
                from = Zone.GRAVEYARD,
            ),
            binding = TriggerBinding.ANY,
        )
        effect = pingEachOpponent
        description = abilityText
    }

    activatedAbility {
        cost = Costs.Mana("{1}{B}")
        effect = Patterns.Library.mill(1, EffectTarget.PlayerRef(Player.Each))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "107"
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a808868f-aea8-4651-9357-85a4d7b4f290.jpg?1783932632"
        ruling("2019-10-04", "If one or more creatures die at the same time as Syr Konrad, its first ability triggers for each of those creatures.")
        ruling("2019-10-04", "In a Two-Headed Giant game, Syr Konrad's first ability causes it to deal 1 damage twice.")
    }
}
