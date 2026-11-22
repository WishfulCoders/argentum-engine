package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Psychic Pickpocket (SNC #54)
 * {4}{U}  Creature — Octopus Rogue  3/2
 *
 * When this creature enters, it connives. When it connives this way, return up to one target
 * nonland permanent to its owner's hand.
 *
 * Modeling: "When it connives this way" is a reflexive trigger whose action is the connive itself,
 * so the "up to one target nonland permanent" is chosen after the draw and discard (per the
 * 2022-04-29 ruling). Conniving always happens — even if the Pickpocket has left the battlefield —
 * so the reflexive trigger is mandatory (`optional = false`); the target itself is "up to one".
 */
val PsychicPickpocket = card("Psychic Pickpocket") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Octopus Rogue"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, it connives. When it connives this way, return up to one " +
        "target nonland permanent to its owner's hand. (To have a creature connive, draw a card, then " +
        "discard a card. If you discarded a nonland card, put a +1/+1 counter on that creature.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ReflexiveTrigger(action = Effects.Connive(), optional = false) {
            val permanent = target(TargetFilter.NonlandPermanent, optional = true)
            effect = Effects.ReturnToHand(permanent)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "54"
        artist = "Will Gist"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/956b90ca-1012-4132-bf2f-bc3825ed16ac.jpg?1783923141"
        ruling("2022-04-29", "The target permanent to return is chosen after Psychic Pickpocket connives.")
        ruling(
            "2022-04-29",
            "If a resolving spell or ability instructs a specific creature to connive but that " +
                "creature has left the battlefield, the creature still connives. Abilities that " +
                "trigger \"when [that creature] connives,\" such as that of Psychic Pickpocket, will trigger."
        )
    }
}
