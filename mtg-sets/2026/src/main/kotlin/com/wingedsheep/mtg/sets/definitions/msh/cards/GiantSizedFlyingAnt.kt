package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Giant-Sized Flying Ant
 * {3}{U}
 * Creature — Insect
 * 3/2
 *
 * Flash
 * Flying
 * When this creature enters, choose one —
 * • Tap target nonland permanent.
 * • Untap target nonland permanent.
 *
 * A modal *triggered* ability whose modes both target: per CR 603.3c the mode and its target are
 * chosen as the ability goes on the stack, which the engine's modal-trigger path already does. Each
 * mode's target is referenced by its mode-local [EffectTarget.ContextTarget] index 0.
 */
val GiantSizedFlyingAnt = card("Giant-Sized Flying Ant") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Insect"
    power = 3
    toughness = 2
    oracleText = "Flash\n" +
        "Flying\n" +
        "When this creature enters, choose one —\n" +
        "• Tap target nonland permanent.\n" +
        "• Untap target nonland permanent."

    keywords(Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            mode("Tap target nonland permanent") {
                val nonlandPermanent = target(TargetFilter.NonlandPermanent)
                effect = Effects.Tap(nonlandPermanent)
            },
            mode("Untap target nonland permanent") {
                val nonlandPermanent = target(TargetFilter.NonlandPermanent)
                effect = Effects.Untap(nonlandPermanent)
            }
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "56"
        artist = "Zoltan Boros"
        flavorText = "Ultron laughed at the swarm, so Pym upped the ante."
        imageUri = "https://cards.scryfall.io/normal/front/d/a/da529bb3-725b-41aa-ac34-b4117ff5d95b.jpg?1783902958"
    }
}
