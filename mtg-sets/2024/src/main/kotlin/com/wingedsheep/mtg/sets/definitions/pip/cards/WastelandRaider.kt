package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mechanics.squad
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wasteland Raider — Fallout #52
 * {2}{B}{B} · Creature — Human Mercenary · 4/3
 *
 * Squad {2}
 * When this creature enters, each player sacrifices a creature of their choice.
 *
 * The Fleshbag Marauder edict: "each player" includes you, and the Raider (or any squad copy) is a
 * legal choice for its controller. Each copy's own enters trigger is a separate edict.
 */
val WastelandRaider = card("Wasteland Raider") {
    manaCost = "{2}{B}{B}"
    typeLine = "Creature — Human Mercenary"
    power = 4
    toughness = 3
    oracleText = "Squad {2} (As an additional cost to cast this spell, you may pay {2} any number of times. When this creature enters, create that many tokens that are copies of it.)\nWhen this creature enters, each player sacrifices a creature of their choice."
    squad("{2}")
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.PlayerRef(Player.Each))
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "52"
        artist = "Yigit Koroglu"
        flavorText = "Honor means nothing. Survival means everything."
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9eae2596-013a-4e9b-982b-e0fc8d1e78bf.jpg?1783912406"
        ruling("2024-03-08", "You may pay the squad cost any number of times. You will create a token that is a copy of that permanent for each time you paid the squad cost. If you paid the squad cost multiple times, the tokens will all enter the battlefield simultaneously.")
        ruling("2024-03-08", "If the spell is countered, the squad ability will not trigger, and no tokens will be created.")
        ruling("2024-03-08", "If the spell resolves but the creature with squad leaves the battlefield before its squad ability resolves, you’ll still create the token copies.")
        ruling("2024-03-08", "The tokens created by the squad ability aren’t “cast,” so any abilities that trigger when a spell is cast won’t trigger for the copies.")
        ruling("2024-03-08", "If, for some reason, the creature doesn’t have the squad ability when it’s on the battlefield, the ability won’t trigger, even if you’ve paid the squad cost one or more times.")
    }
}
