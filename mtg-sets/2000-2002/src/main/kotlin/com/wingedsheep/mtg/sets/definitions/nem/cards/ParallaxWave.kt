package com.wingedsheep.mtg.sets.definitions.nem.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Parallax Wave
 * {2}{W}{W}
 * Enchantment
 *
 * Fading 5 (This enchantment enters with five fade counters on it. At the beginning of your
 * upkeep, remove a fade counter from it. If you can't, sacrifice it.)
 * Remove a fade counter from this enchantment: Exile target creature.
 * When this enchantment leaves the battlefield, each player returns to the battlefield all cards
 * they own exiled with it.
 *
 * Implementation notes:
 * - Fading is engine-live ([KeywordAbility.fading]): the five fade counters and the upkeep
 *   "remove one; if you can't, sacrifice it" come from the keyword alone. The activated ability
 *   spends the same counters as its cost, so five activations empty the Wave and the *next* upkeep
 *   sacrifices it — returning everything at once.
 * - The exile and the return are linked abilities (CR 607.2a): [Effects.ExileLinkedToSource]
 *   records each exiled card on this Wave's battlefield visit, and the leaves trigger returns
 *   exactly those, each under its owner's control ("each player returns … all cards they own").
 *   The pile is visit-scoped, so an activation that resolves *after* the Wave has left — its
 *   leaves trigger already resolved above it — exiles the creature for good, and a later Wave (or
 *   this card, re-cast) never returns cards an earlier visit exiled.
 * - Responding to the Wave's own removal by exiling your creatures with it, then letting the
 *   leaves trigger bring them back, is the classic flicker; it falls out of the stack order with
 *   no special casing.
 */
val ParallaxWave = card("Parallax Wave") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Fading 5 (This enchantment enters with five fade counters on it. At the beginning " +
        "of your upkeep, remove a fade counter from it. If you can't, sacrifice it.)\n" +
        "Remove a fade counter from this enchantment: Exile target creature.\n" +
        "When this enchantment leaves the battlefield, each player returns to the battlefield all " +
        "cards they own exiled with it."

    keywordAbility(KeywordAbility.fading(5))

    activatedAbility {
        val creature = target(TargetFilter.Creature)
        cost = Costs.RemoveCounterFromSelf(CounterType.FADE)
        effect = Effects.ExileLinkedToSource(creature)
        description = "Remove a fade counter from this enchantment: Exile target creature."
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "17"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/cef789e8-e4cc-4f61-bc15-debc2487777f.jpg?1783945841"
    }
}
