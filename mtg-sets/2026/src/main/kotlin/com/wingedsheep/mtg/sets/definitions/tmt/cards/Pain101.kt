package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Pain 101
 * {1}{B}
 * Instant
 *
 * Until end of turn, target creature gains deathtouch and "When this
 * creature dies, return it to the battlefield tapped under its
 * owner's control."
 */
val Pain101 = card("Pain 101") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Until end of turn, target creature gains deathtouch and \"When this creature dies, return it to the battlefield tapped under its owner's control.\""

    spell {
        val creature = target(TargetFilter.Creature)

        val diesReturnTapped = TriggeredAbility.create(
            trigger = Triggers.self.dies(),
            effect = Effects.Move(
                target = EffectTarget.Self,
                destination = Zone.BATTLEFIELD,
                placement = ZonePlacement.Tapped,
                fromZone = Zone.GRAVEYARD,
            ),
            descriptionOverride = "When this creature dies, return it to the battlefield tapped under its owner's control."
        )

        effect = Effects.GrantKeyword(Keyword.DEATHTOUCH, creature, Duration.EndOfTurn) then
            Effects.GrantTriggeredAbility(
                ability = diesReturnTapped,
                target = creature,
                duration = Duration.EndOfTurn,
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "69"
        artist = "Thomas Chamberlain-Keen"
        flavorText = "Your instructor is Casey Jones!"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a1c70bf2-b2bd-4585-ba50-304f4dad8e62.jpg?1771586891"
    }
}
