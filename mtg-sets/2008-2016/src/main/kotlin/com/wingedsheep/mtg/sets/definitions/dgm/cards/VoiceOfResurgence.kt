package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessDynamicStatic
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Voice of Resurgence
 * {G}{W}
 * Creature — Elemental
 * 2/2
 *
 * Whenever an opponent casts a spell during your turn and when this creature dies, create a green
 * and white Elemental creature token with "This token's power and toughness are each equal to the
 * number of creatures you control."
 *
 * "Whenever … and when …" is two triggered abilities sharing one effect. The token's P/T is a
 * characteristic-defining ability — a Layer 7b static on the token itself, re-read on every
 * projection — not a snapshot taken at creation, so it grows and shrinks with the board and counts
 * itself (it is always at least 1/1).
 */
private val elementalToken = Effects.CreateToken(
    power = 0,
    toughness = 0,
    colors = setOf(Color.GREEN, Color.WHITE),
    creatureTypes = setOf("Elemental"),
    staticAbilities = listOf(
        SetBasePowerToughnessDynamicStatic(
            power = DynamicAmounts.creaturesYouControl(),
            toughness = DynamicAmounts.creaturesYouControl(),
            filter = GroupFilter.source(),
        )
    ),
)

val VoiceOfResurgence = card("Voice of Resurgence") {
    manaCost = "{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Elemental"
    power = 2
    toughness = 2
    oracleText = "Whenever an opponent casts a spell during your turn and when this creature dies, " +
        "create a green and white Elemental creature token with \"This token's power and toughness " +
        "are each equal to the number of creatures you control.\""

    triggeredAbility {
        trigger = Triggers.anOpponent.casts()
        triggerRestriction = Conditions.IsYourTurn
        effect = elementalToken
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = elementalToken
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "114"
        artist = "Winona Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07246783-d475-4f61-99ac-e2b574072349.jpg?1783940019"
        ruling(
            "2020-08-07",
            "An ability that triggers when a player casts a spell resolves before the spell that caused it to trigger. It resolves even if that spell is countered."
        )
        ruling(
            "2020-08-07",
            "The power and toughness of the token change as the number of creatures you control changes. The token's ability counts itself, so it'll be at least 1/1."
        )
    }
}
