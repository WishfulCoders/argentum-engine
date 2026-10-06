package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectDamage
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Veteran Bodyguard
 * {3}{W}{W}
 * Creature — Human
 * 2/5
 * As long as this creature is untapped, all damage that would be dealt to you by unblocked
 * creatures is dealt to this creature instead.
 *
 * The Martyrs of Korlis shape with an unblocked-creature source filter: a static
 * [RedirectDamage] to itself, gated on [Conditions.SourceIsUntapped]. The source filter reads
 * the durable unblocked combat status (`StatePredicate.IsUnblocked`), so trample damage from a
 * *blocked* attacker is not redirected (2004-10-04 ruling), and the Bodyguard still takes the
 * damage while it is blocking, as long as it is untapped.
 */
val VeteranBodyguard = card("Veteran Bodyguard") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human"
    power = 2
    toughness = 5

    oracleText = "As long as this creature is untapped, all damage that would be dealt to you by " +
        "unblocked creatures is dealt to this creature instead."

    replacementEffect(
        RedirectDamage(
            redirectTo = EffectTarget.Self,
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.You,
                source = GameObjectFilter.Creature.unblocked(),
            ),
            condition = Conditions.SourceIsUntapped,
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "41"
        artist = "Douglas Shuler"
        flavorText = "Good bodyguards are hard to find, mainly because they don't live long."
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cbd9ab01-a833-4fa4-8dee-151bd9800835.jpg?1783948709"
        ruling(
            "2004-10-04",
            "If a creature is blocked but Trample damage is still done to a player, this damage can't be " +
                "redirected to the Bodyguard because the Bodyguard only takes damage from unblocked creatures."
        )
        ruling("2004-10-04", "Damage goes to the Bodyguard as long as he is untapped. This works even if he is blocking.")
        ruling(
            "2004-10-04",
            "If you have multiple Veteran Bodyguards, you can decide which one receives the redirected damage " +
                "each time damage would be dealt to you."
        )
    }
}
