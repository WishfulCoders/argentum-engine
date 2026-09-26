package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Galadriel, Gift-Giver
 * {3}{G}{G}
 * Legendary Creature — Elf Noble
 * 4/4
 *
 * Whenever Galadriel enters or attacks, choose one —
 * • Put a +1/+1 counter on another target creature.
 * • Create a Food token.
 * • Create a Treasure token.
 */
val GaladrielGiftGiver = card("Galadriel, Gift-Giver") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Elf Noble"
    power = 4
    toughness = 4
    oracleText = "Whenever Galadriel enters or attacks, choose one —\n" +
        "• Put a +1/+1 counter on another target creature.\n" +
        "• Create a Food token. (It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n" +
        "• Create a Treasure token. (It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    val galadrielModal = ModalEffect.chooseOne(
        mode("Put a +1/+1 counter on another target creature") {
            val otherCreature = target(TargetFilter.OtherCreature)
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, otherCreature)
        },
        Mode.noTarget(
            Effects.CreateFood(),
            "Create a Food token"
        ),
        Mode.noTarget(
            Effects.CreateTreasure(),
            "Create a Treasure token"
        )
    )

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = galadrielModal
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = galadrielModal
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "296"
        artist = "Alexander Mokhov"
        imageUri = "https://cards.scryfall.io/normal/front/8/2/8229264e-af6c-4f1f-b86a-257889ace9ce.jpg?1687424896"
    }
}
