package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fake Your Own Death
 * {1}{B}
 * Instant
 *
 * Until end of turn, target creature gets +2/+0 and gains "When this creature dies,
 * return it to the battlefield tapped under its owner's control and you create a
 * Treasure token."
 *
 * Canonical printing: Streets of New Capenna (SNC, 2022). Reprinted in Outlaws of
 * Thunder Junction (OTJ) and others.
 *
 * Modeled as a temporary +2/+0 [Effects.ModifyStats] plus a granted self dies-trigger
 * ([GrantTriggeredAbilityEffect] with `Triggers.self.dies()`). When the buffed creature dies,
 * the granted ability returns it to the battlefield tapped from the graveyard
 * ([Effects.PutOntoBattlefield] of [EffectTarget.Self], which with no controllerOverride
 * lands under its owner's control) and creates a Treasure token. Both the buff and the
 * grant expire at end of turn ([Duration.EndOfTurn]) — same shape as Commando Raid /
 * Unstoppable Slasher's self-return dies trigger.
 */
val FakeYourOwnDeath = card("Fake Your Own Death") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Until end of turn, target creature gets +2/+0 and gains \"When this creature dies, " +
        "return it to the battlefield tapped under its owner's control and you create a Treasure token.\" " +
        "(It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 0, t) then
            Effects.GrantTriggeredAbility(
                ability = TriggeredAbility.create(
                    trigger = Triggers.self.dies(),
                    effect = Effects.PutOntoBattlefield(EffectTarget.Self, tapped = true) then
                        Effects.CreateTreasure(1),
                    descriptionOverride = "When this creature dies, return it to the battlefield tapped " +
                        "under its owner's control and you create a Treasure token.",
                ),
                target = t,
                duration = Duration.EndOfTurn,
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "79"
        artist = "Kari Christensen"
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b4e117a8-3291-4f9a-ab00-c820c8e2aa00.jpg?1664410943"
    }
}
