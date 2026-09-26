package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Passenger Ferry
 * {3}
 * Artifact — Vehicle, 4/3
 *
 * Whenever this Vehicle attacks, you may pay {U}. When you do, another target attacking
 * creature can't be blocked this turn.
 * Crew 2 (Tap any number of creatures you control with total power 2 or more: This Vehicle
 * becomes an artifact creature until end of turn.)
 *
 * The attack trigger is a "When you do" reflexive: the optional {U} payment is the action, and
 * the reflexive ability — which targets another attacking creature, chosen as it goes on the
 * stack — only fires if the payment is made. `.other()` excludes the Vehicle itself so the
 * target is "another" attacking creature.
 */
val PassengerFerry = card("Passenger Ferry") {
    manaCost = "{3}"
    colorIdentity = "U"
    typeLine = "Artifact — Vehicle"
    power = 4
    toughness = 3
    oracleText = "Whenever this Vehicle attacks, you may pay {U}. When you do, another target attacking " +
        "creature can't be blocked this turn.\n" +
        "Crew 2 (Tap any number of creatures you control with total power 2 or more: This Vehicle " +
        "becomes an artifact creature until end of turn.)"

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ReflexiveTrigger(
            // "you may pay {U}"
            action = Effects.PayMana("{U}"),
            optional = true) {
            // "When you do, another target attacking creature can't be blocked this turn."
            val creature = target(TargetFilter.AttackingCreature.other())
            effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, creature)
        }
    }

    keywordAbility(KeywordAbility.crew(2))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "170"
        artist = "Leon Tukker"
        flavorText = "\"Staten Island in peril? Uh, give me an hour?\"\n—Ghost-Spider, Gwen Stacy"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/2495f477-b88c-4938-a86a-f72c3c861188.jpg?1783905304"
    }
}
