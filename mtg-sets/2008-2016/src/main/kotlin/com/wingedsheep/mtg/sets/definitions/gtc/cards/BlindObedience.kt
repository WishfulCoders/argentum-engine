package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PermanentsEnterTapped

/**
 * Blind Obedience
 * {1}{W}
 * Enchantment
 * Extort (Whenever you cast a spell, you may pay {W/B}. If you do, each opponent loses 1 life and
 * you gain that much life.)
 * Artifacts and creatures your opponents control enter tapped.
 *
 * Extort is [Patterns.Mechanic.extort] on a cast trigger (shared with The Kingpin of Crime); the
 * second ability is Thalia, Heretic Cathar's enters-tapped replacement over artifacts and creatures.
 */
val BlindObedience = card("Blind Obedience") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Extort (Whenever you cast a spell, you may pay {W/B}. If you do, each opponent loses 1 life and you gain that much life.)\n" +
        "Artifacts and creatures your opponents control enter tapped."

    triggeredAbility {
        trigger = Triggers.you.casts()
        effect = Patterns.Mechanic.extort()
        description = "Extort (Whenever you cast a spell, you may pay {W/B}. If you do, each " +
            "opponent loses 1 life and you gain that much life.)"
    }

    replacementEffect(
        PermanentsEnterTapped(
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.CreatureOrArtifact.opponentControls(),
                to = Zone.BATTLEFIELD,
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "6"
        artist = "Seb McKinnon"
        flavorText = "\"By the time your knees have worn through your robe, you may have begun to learn your place.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07c3e78d-d917-4552-842f-feff99c059e0.jpg?1783940144"
        ruling("2024-01-12", "You may pay {W/B} a maximum of one time for each extort triggered ability. You decide whether to pay when the ability resolves.")
        ruling("2024-01-12", "The amount of life you gain from extort is based on the total amount of life lost, not necessarily the number of opponents you have. For example, if your opponent's life total can't change (perhaps because that player controls Platinum Emperion), you won't gain any life.")
        ruling("2024-01-12", "The extort ability doesn't target any player.")
        ruling("2024-01-12", "The extort ability resolves before the spell that caused it to trigger. The ability resolves even if that spell is countered.")
    }
}
