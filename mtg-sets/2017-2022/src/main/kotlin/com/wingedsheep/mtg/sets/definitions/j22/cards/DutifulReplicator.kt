package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dutiful Replicator — Jumpstart 2022 #48
 * {3} · Artifact Creature — Assembly-Worker · 3/2
 *
 * When this creature enters, you may pay {1}. When you do, create a token that's a copy of
 * target token you control not named Dutiful Replicator.
 *
 * The {1} is paid as the enters trigger resolves; only then does the reflexive "when you do"
 * ability (CR 603.12) go on the stack and choose its target.
 */
val DutifulReplicator = card("Dutiful Replicator") {
    manaCost = "{3}"
    typeLine = "Artifact Creature — Assembly-Worker"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, you may pay {1}. When you do, create a token that's a copy " +
        "of target token you control not named Dutiful Replicator."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ReflexiveTrigger(
            action = Effects.PayMana("{1}"),
            descriptionOverride = "You may pay {1}. When you do, create a token that's a copy of " +
                "target token you control not named Dutiful Replicator."
        ) {
            val token = target(
                TargetFilter(GameObjectFilter.Token.youControl().notNamed("Dutiful Replicator"))
            )
            effect = Effects.CreateTokenCopyOfTarget(target = token)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "48"
        artist = "Alexander Forssberg"
        flavorText = "\"Careful. Leave it running for too long and it'll turn the whole building into angels.\"\n—Tawnos"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0de6fc60-89ad-4c6f-a734-5344e5b954fa.jpg?1783919177"
    }
}
