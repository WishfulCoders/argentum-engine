package com.wingedsheep.mtg.sets.definitions.rav.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Thoughtpicker Witch — Ravnica: City of Guilds #109
 * {B} · Creature — Human Wizard · 1/1
 *
 * {1}, Sacrifice a creature: Look at the top two cards of target opponent's library, then exile
 * one of them.
 *
 * Modelling notes:
 * - `Costs.Sacrifice(GameObjectFilter.Creature)` is "a creature", not "another creature" — the
 *   Witch is a legal sacrifice for her own ability, and the ability still resolves after she is
 *   gone because the cost is paid on activation.
 * - The exile is mandatory ("then exile one of them"), so the selection is `ChooseExactly(1)`
 *   rather than the "you may" `ChooseUpTo` of a peek. The card that isn't exiled is moved back to
 *   the top of that opponent's library, which is where it already was.
 * - The look and the choice belong to this card's controller; the targeted opponent only supplies
 *   the library.
 */
val ThoughtpickerWitch = card("Thoughtpicker Witch") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 1
    oracleText = "{1}, Sacrifice a creature: Look at the top two cards of target opponent's " +
        "library, then exile one of them."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Sacrifice(GameObjectFilter.Creature))
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            val peeked = gather(CardSource.TopOfLibrary(2, opponent.asPlayer))
            val (toExile, toTop) = chooseExactlySplit(
                1,
                from = peeked,
                selectedLabel = "Exile",
                remainderLabel = "Leave on top of that player's library"
            )
            exile(toExile, opponent.asPlayer)
            toLibraryTop(toTop, opponent.asPlayer, order = CardOrder.Preserve)
        }
        description = "{1}, Sacrifice a creature: Look at the top two cards of target opponent's " +
            "library, then exile one of them."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "109"
        artist = "Pete Venters"
        flavorText = "\"Once the brew gets the brains nice and pickled, they're a lot easier to " +
            "pick through.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b6f8df33-a955-403c-aafc-85e5589c5041.jpg?1783943661"
    }
}
