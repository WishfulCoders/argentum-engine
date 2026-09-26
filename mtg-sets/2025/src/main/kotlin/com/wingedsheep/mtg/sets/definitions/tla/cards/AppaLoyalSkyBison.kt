package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetOther
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Appa, Loyal Sky Bison — {4}{W}{W} Legendary Creature — Bison Ally — 4/4
 *
 * Flying
 * Whenever Appa enters or attacks, choose one —
 * • Target creature you control gains flying until end of turn.
 * • Airbend another target nonland permanent you control. (Exile it. While it's exiled, its owner
 *   may cast it for {2} rather than its mana cost.)
 *
 * "Enters or attacks" is two triggered abilities (ETB + Attacks) sharing one
 * [ModalEffect.chooseOne] — the immutable modal data is reused by both (HeiBai / Raven Eagle
 * pattern). The airbend mode uses target-agnostic [Effects.Airbend]; its mode target ("another
 * nonland permanent you control") feeds it via `CardSource.ChosenTargets`.
 */
val AppaLoyalSkyBison = card("Appa, Loyal Sky Bison") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Bison Ally"
    oracleText = "Flying\n" +
        "Whenever Appa enters or attacks, choose one —\n" +
        "• Target creature you control gains flying until end of turn.\n" +
        "• Airbend another target nonland permanent you control. (Exile it. While it's exiled, its owner may cast it for {2} rather than its mana cost.)"
    power = 4
    toughness = 4

    keywords(Keyword.FLYING)

    val choice = ModalEffect.chooseOne(
        mode("Target creature you control gains flying until end of turn") {
            val creatureYouControl = target(TargetFilter.CreatureYouControl)
            effect = Effects.GrantKeyword(Keyword.FLYING, creatureYouControl)
        },
        Mode(
            effect = Effects.Airbend(),
            targetRequirements = listOf(
                TargetOther(baseRequirement = TargetObject(filter = TargetFilter.NonlandPermanent.youControl()))
            ),
            description = "Airbend another target nonland permanent you control"
        )
    )

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = choice
        description = "Whenever Appa enters or attacks, choose one — Target creature you control gains flying until end of turn; or airbend another target nonland permanent you control."
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = choice
        description = "Whenever Appa enters or attacks, choose one — Target creature you control gains flying until end of turn; or airbend another target nonland permanent you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "9"
        artist = "Tomoyo Asatani"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9b2a843-c6fe-4d19-801e-1538e4381ab0.jpg?1764119928"
    }
}
