package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mechanics.squad
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Powder Ganger — Fallout #65
 * {2}{R} · Creature — Human Rogue · 2/2
 *
 * Squad {2}
 * When this creature enters, destroy up to one target artifact.
 *
 * Each squad token is a copy, so each copy's own enters trigger destroys up to one more artifact.
 */
val PowderGanger = card("Powder Ganger") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Human Rogue"
    power = 2
    toughness = 2
    oracleText = "Squad {2} (As an additional cost to cast this spell, you may pay {2} any number of times. When this creature enters, create that many tokens that are copies of it.)\nWhen this creature enters, destroy up to one target artifact."
    squad("{2}")
    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Artifact, optional = true)
        effect = Effects.Destroy(t)
        description = "When this creature enters, destroy up to one target artifact."
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "65"
        artist = "Fesbra"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7bb57c73-a454-450d-9db9-67501bf4559d.jpg?1783912402"
        ruling("2024-03-08", "You may pay the squad cost any number of times. You will create a token that is a copy of that permanent for each time you paid the squad cost. If you paid the squad cost multiple times, the tokens will all enter the battlefield simultaneously.")
        ruling("2024-03-08", "If the spell is countered, the squad ability will not trigger, and no tokens will be created.")
        ruling("2024-03-08", "If the spell resolves but the creature with squad leaves the battlefield before its squad ability resolves, you’ll still create the token copies.")
        ruling("2024-03-08", "The tokens created by the squad ability aren’t “cast,” so any abilities that trigger when a spell is cast won’t trigger for the copies.")
        ruling("2024-03-08", "If, for some reason, the creature doesn’t have the squad ability when it’s on the battlefield, the ability won’t trigger, even if you’ve paid the squad cost one or more times.")
    }
}
