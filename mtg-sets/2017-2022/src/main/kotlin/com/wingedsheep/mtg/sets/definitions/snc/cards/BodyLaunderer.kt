package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Body Launderer
 * {2}{B}{B}
 * Creature — Ogre Rogue
 * 3/3
 * Deathtouch
 * Whenever another nontoken creature you control dies, this creature connives.
 * When this creature dies, return another target non-Rogue creature card with equal or lesser
 * power from your graveyard to the battlefield.
 *
 * "Equal or lesser power" compares against Body Launderer's power as it last existed on the
 * battlefield: `powerAtMostEntity(EffectTarget.Self)` reads the source through the
 * last-known-information path once it has left the battlefield.
 */
val BodyLaunderer = card("Body Launderer") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Ogre Rogue"
    oracleText = "Deathtouch\n" +
        "Whenever another nontoken creature you control dies, this creature connives. " +
        "(Draw a card, then discard a card. If you discarded a nonland card, put a +1/+1 counter on this creature.)\n" +
        "When this creature dies, return another target non-Rogue creature card with equal or lesser power " +
        "from your graveyard to the battlefield."
    power = 3
    toughness = 3

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl().nontoken()).dies()
        effect = Effects.Connive()
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        val creature = target(
            TargetFilter(
                GameObjectFilter.Creature
                    .notSubtype(Subtype.ROGUE)
                    .ownedByYou()
                    .powerAtMostEntity(EffectTarget.Self),
                zone = Zone.GRAVEYARD,
            ).other()
        )
        effect = Effects.PutOntoBattlefieldFromGraveyard(creature)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "68"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/24d7d77a-35b6-451a-af44-ecaf38facccb.jpg?1783923136"
        ruling("2022-04-29", "Use Body Launderer's power as it last existed on the battlefield to determine legal targets for its last ability.")
        ruling("2022-04-29", "If a resolving spell or ability instructs a specific creature to connive but that creature has left the battlefield, the creature still connives. If you discard a nonland card this way, you won't put a +1/+1 counter on anything. Abilities that trigger “when [that creature] connives” will trigger.")
    }
}
