package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Moira Brown, Guide Author
 * {1}{R}{W}
 * Legendary Creature — Human Citizen
 * 2/3
 * When Moira Brown enters, create a colorless Book Equipment artifact token named Wasteland
 * Survival Guide with "Equipped creature gets +1/+1 for each quest counter among permanents you
 * control" and equip {1}.
 * Whenever you attack, put a quest counter on target nonland permanent you control.
 *
 * The token is a named predefined token (`PredefinedTokens.WastelandSurvivalGuide`) — its static
 * and equip ability live once in the registry, the Sword / Sturdy Shield shape. "Whenever you
 * attack" fires once per declaration of attackers, not per attacking creature.
 */
val MoiraBrownGuideAuthor = card("Moira Brown, Guide Author") {
    manaCost = "{1}{R}{W}"
    typeLine = "Legendary Creature — Human Citizen"
    power = 2
    toughness = 3
    oracleText = "When Moira Brown enters, create a colorless Book Equipment artifact token named " +
        "Wasteland Survival Guide with \"Equipped creature gets +1/+1 for each quest counter among " +
        "permanents you control\" and equip {1}.\n" +
        "Whenever you attack, put a quest counter on target nonland permanent you control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreatePredefinedToken("Wasteland Survival Guide")
        description = "When Moira Brown enters, create a colorless Book Equipment artifact token " +
            "named Wasteland Survival Guide with \"Equipped creature gets +1/+1 for each quest " +
            "counter among permanents you control\" and equip {1}."
    }

    triggeredAbility {
        trigger = Triggers.you.attacks()
        val permanent = target(TargetFilter(GameObjectFilter.NonlandPermanent.youControl()))
        effect = Effects.AddCounters(CounterType.QUEST, 1, permanent)
        description = "Whenever you attack, put a quest counter on target nonland permanent you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "110"
        artist = "Campbell White"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/418ce6f6-b483-4695-aaf5-e4f7eeb9eb16.jpg?1783912387"
    }
}
