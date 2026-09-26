package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Glacial Dragonhunt — Tarkir: Dragonstorm #188
 * {U}{R} · Sorcery · Uncommon
 *
 * Draw a card, then you may discard a card. When you discard a nonland card this way,
 * Glacial Dragonhunt deals 3 damage to target creature.
 * Harmonize {4}{U}{R}
 *
 * The "when you discard a nonland card this way" clause is a reflexive triggered ability
 * (CR 603.12): its target is chosen only after — and only if — a nonland card is actually
 * discarded, so it is not a cast-time spell target:
 *   1. draw a card;
 *   2. gather the hand, let the controller discard UP TO one card (the printed "you may"), and
 *      move the choice to the graveyard as a discard;
 *   3. only when the discarded collection contains a [GameObjectFilter.Nonland] card, a
 *      [ReflexiveTriggerEffect] puts the damage ability on the stack, targeting a creature as it
 *      goes there — so hexproof and shroud apply, opponents can respond, and it does nothing if
 *      the creature is gone by the time it resolves (CR 608.2b).
 * If no card is discarded, or a land is discarded, nothing triggers.
 */
val GlacialDragonhunt = card("Glacial Dragonhunt") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Sorcery"
    oracleText = "Draw a card, then you may discard a card. When you discard a nonland card this " +
        "way, Glacial Dragonhunt deals 3 damage to target creature.\n" +
        "Harmonize {4}{U}{R} (You may cast this card from your graveyard for its harmonize cost. " +
        "You may tap a creature you control to reduce that cost by {X}, where X is its power. " +
        "Then exile this spell.)"

    spell {
        effect = Effects.Pipeline {
            run(Effects.DrawCards(1))
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
            val discarded = chooseUpTo(
                1,
                from = hand,
                chooser = Chooser.Controller,
                prompt = "You may discard a card"
            )
            discard(discarded)
            // "When you discard a nonland card this way" is a reflexive triggered ability
            // (CR 603.12): it exists only when the discarded card is nonland, and its target
            // creature is chosen as it goes on the stack, not picked during resolution.
            ifNotEmpty(discarded, filter = GameObjectFilter.Nonland) {
                run(Effects.ReflexiveTrigger(
                    action = Effects.Nothing,
                    optional = false,
                    descriptionOverride = "Glacial Dragonhunt deals 3 damage to target creature"
                ) {
                    // No `damageSource = Self`: by the time the reflexive ability resolves the
                    // spell has left the stack, so its source is the ability's recorded source
                    // (the default), not the new graveyard object.
                    val creature = target(TargetFilter.Creature)
                    effect = Effects.DealDamage(3, creature)
                })
            }
        }
    }

    keywordAbility(KeywordAbility.harmonize("{4}{U}{R}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "188"
        artist = "Igor Grechanyi"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95994c88-e404-4a4f-8be6-b99d703d4609.jpg?1743204732"
    }
}
