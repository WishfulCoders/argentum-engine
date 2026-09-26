package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Death Pulse
 * {2}{B}{B}
 * Instant
 * Target creature gets -4/-4 until end of turn.
 * Cycling {1}{B}{B}
 * When you cycle Death Pulse, you may have target creature get -1/-1 until end of turn.
 */
val DeathPulse = card("Death Pulse") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets -4/-4 until end of turn.\nCycling {1}{B}{B}\nWhen you cycle Death Pulse, you may have target creature get -1/-1 until end of turn."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(
            power = -4,
            toughness = -4,
            target = t,
            duration = Duration.EndOfTurn
        )
    }

    keywordAbility(KeywordAbility.cycling("{1}{B}{B}"))

    triggeredAbility {
        trigger = Triggers.self.isCycled()
        val t = target(TargetFilter.Creature)
        effect = Effects.May(
            Effects.ModifyStats(
                power = -1,
                toughness = -1,
                target = t,
                duration = Duration.EndOfTurn
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "137"
        artist = "Tony Szczudlo"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/normal/front/5/2/524fd470-e535-47ea-98a0-6187e429dfe1.jpg?1562914293"
    }
}
