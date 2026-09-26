package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.Triggers

/**
 * Jackdaw Savior
 * {2}{W}
 * Creature — Bird Cleric
 * 3/1
 *
 * Flying
 *
 * Whenever this creature or another creature you control with flying dies,
 * return another target creature card with lesser mana value from your
 * graveyard to the battlefield.
 *
 * The return is a real target, chosen as the trigger is put on the stack (CR 603.3d): a
 * creature card in your graveyard whose mana value is below the dying creature's. The cap is
 * read off the triggering entity, which is the dead creature's card in the graveyard — the same
 * mana value it had as it last existed on the battlefield, per the 2024-07-26 ruling. "Another"
 * needs no separate exclusion: the dying creature's own card never has a mana value lower than
 * itself. If the targeted card leaves the graveyard before the trigger resolves, the trigger has
 * no legal target and does nothing (CR 608.2b).
 */
val JackdawSavior = card("Jackdaw Savior") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird Cleric"
    power = 3
    toughness = 1
    oracleText = "Flying\nWhenever this creature or another creature you control with flying dies, return another target creature card with lesser mana value from your graveyard to the battlefield."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().withKeyword(Keyword.FLYING)).dies()
        val creatureCard = target(
            TargetFilter.CreatureInYourGraveyard.manaValueAtMostDynamic(
                DynamicAmounts.triggeringManaValue() - 1
            ),
        )
        effect = Effects.PutOntoBattlefieldFromGraveyard(creatureCard)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "18"
        artist = "Alessandra Pisano"
        imageUri = "https://cards.scryfall.io/normal/front/1/2/121af600-6143-450a-9f87-12ce4833f1ec.jpg?1721425865"

        ruling("2024-07-26", "If Jackdaw Savior and another creature you control with flying die at the same time, Jackdaw Savior's last ability triggers for each of them.")
        ruling("2024-07-26", "The target creature card must have a lesser mana value than the creature that caused Jackdaw Savior's last ability to trigger. Use the mana value of that creature as it last existed on the battlefield to determine which creature cards in your graveyard are legal targets for that ability.")
    }
}
