package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Skyclave Apparition — Zendikar Rising #39
 * {1}{W}{W} · Creature — Kor Spirit · 2/2
 *
 * When this creature enters, exile up to one target nonland, nontoken permanent you don't control
 * with mana value 4 or less.
 * When this creature leaves the battlefield, the exiled card's owner creates an X/X blue Illusion
 * creature token, where X is the mana value of the exiled card.
 *
 * - The exile is permanent (nothing returns), linked to this creature with
 *   [Effects.ExileLinkedToSource] so the leaves trigger can find the card. "You don't control" is
 *   the two-player `opponentControls()` spelling the corpus uses throughout.
 * - The leaves trigger is Severance Priest's pipeline: gather the linked-exile pile, create
 *   `count` tokens (0 when nothing was exiled — no token, per the first ruling) sized by the
 *   exiled card's mana value and controlled by that card's owner.
 */
val SkyclaveApparition = card("Skyclave Apparition") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Kor Spirit"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, exile up to one target nonland, nontoken permanent you don't control with mana value 4 or less.\n" +
        "When this creature leaves the battlefield, the exiled card's owner creates an X/X blue Illusion creature token, where X is the mana value of the exiled card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(
            TargetFilter(
                GameObjectFilter.NonlandPermanent
                    .nontoken()
                    .opponentControls()
                    .manaValueAtMost(4)
            ),
            optional = true,
        )
        effect = Effects.ExileLinkedToSource(t)
        description = "When this creature enters, exile up to one target nonland, nontoken permanent you don't control with mana value 4 or less."
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.Pipeline {
            val exiledCard = gather(CardSource.FromLinkedExile())
            run(Effects.CreateToken(
                count = exiledCard.count,
                power = 0,
                toughness = 0,
                colors = setOf(Color.BLUE),
                creatureTypes = setOf("Illusion"),
                dynamicPower = DynamicAmounts.manaValueOf(exiledCard),
                dynamicToughness = DynamicAmounts.manaValueOf(exiledCard),
                controller = exiledCard.controllerOf(),
                imageUri = "https://cards.scryfall.io/normal/front/2/3/2300635e-7771-4676-a5a5-29a9d8f49f1a.jpg?1783929499"
            ))
        }
        description = "When this creature leaves the battlefield, the exiled card's owner creates an X/X blue Illusion creature token, where X is the mana value of the exiled card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "39"
        artist = "Donato Giancola"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b83cfbaa-7890-4f6f-878b-4edb45677371.jpg?1783929406"
        ruling("2020-09-25", "If there's no exiled card when Skyclave Apparition leaves the battlefield (most likely because its first ability hasn't resolved yet), no player creates a token.")
        ruling("2020-09-25", "If a creature on the battlefield has {X} in its mana cost, X is considered to be 0.")
        ruling("2020-09-25", "If Skyclave Apparition leaves the battlefield before its first ability resolves, the ability still exiles the target permanent.")
    }
}
