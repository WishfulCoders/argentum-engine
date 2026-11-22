package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Chaos Orb
 * {2}
 * Artifact
 * {1}, {T}: If this artifact is on the battlefield, flip it onto the battlefield from a height of
 * at least one foot. If this artifact turns over completely at least once during the flip, destroy
 * all nontoken permanents it touches. Then destroy this artifact.
 *
 * The flip is a dexterity mini-game: the controller aims over a table laid out from the
 * battlefield and times the release; the engine rolls where it lands
 * ([com.wingedsheep.sdk.scripting.effects.FlipOntoBattlefieldEffect]). The touched collection is
 * empty unless the Orb turned over, so the destroy step needs no gate of its own.
 */
val ChaosOrb = card("Chaos Orb") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{1}, {T}: If this artifact is on the battlefield, flip it onto the battlefield from a height of " +
        "at least one foot. If this artifact turns over completely at least once during the flip, destroy all " +
        "nontoken permanents it touches. Then destroy this artifact."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.If(
            Conditions.SourceInZone(Zone.BATTLEFIELD),
            Effects.Pipeline {
                destroy(flipOntoBattlefield(GameObjectFilter.Permanent.nontoken()))
            },
        ) then Effects.Destroy(EffectTarget.Self)
        description = "{1}, {T}: If this artifact is on the battlefield, flip it onto the battlefield from a height of " +
            "at least one foot. If this artifact turns over completely at least once during the flip, destroy all " +
            "nontoken permanents it touches. Then destroy this artifact."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "235"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92274971-7c4a-4326-b0fe-75e2d124f718.jpg?1783948669"
        ruling(
            "2004-10-04",
            "Chaos Orb can only affect permanents. Cards that are in the game but not on the battlefield, such " +
                "as those in the Library and Graveyard, can't be affected."
        )
        ruling("2004-10-04", "It must flip 360 degrees (that's what \"flip\" means). And this flip must be in the air and not in your hand.")
        ruling("2004-10-04", "This is a not a targeted ability.")
    }
}
