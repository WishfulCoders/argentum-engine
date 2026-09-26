package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Momo, Playful Pet
 * {W}
 * Legendary Creature — Lemur Bat Ally
 * 1/1
 *
 * Flying, vigilance
 * When Momo leaves the battlefield, choose one —
 * • Create a Food token. (It's an artifact with "{2}, {T}, Sacrifice this token: You gain 3 life.")
 * • Put a +1/+1 counter on target creature you control.
 * • Scry 2.
 */
val MomoPlayfulPet = card("Momo, Playful Pet") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Lemur Bat Ally"
    oracleText = "Flying, vigilance\n" +
        "When Momo leaves the battlefield, choose one —\n" +
        "• Create a Food token. (It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n" +
        "• Put a +1/+1 counter on target creature you control.\n" +
        "• Scry 2."
    power = 1
    toughness = 1

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.CreateFood(),
                "Create a Food token."
            ),
            mode("Put a +1/+1 counter on target creature you control.") {
                val creature = target(TargetFilter.Creature.youControl())
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
            },
            Mode.noTarget(
                Patterns.Library.scry(2),
                "Scry 2."
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "30"
        artist = "Awanqi (Angela Wang)"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/9350bf4a-fa12-4867-b31f-1f1394d99571.jpg?1764120089"
    }
}
