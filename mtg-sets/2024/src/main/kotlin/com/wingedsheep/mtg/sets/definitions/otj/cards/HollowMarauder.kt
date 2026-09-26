package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetOpponent

/**
 * Hollow Marauder
 * {6}{B}
 * Creature — Specter Rogue
 * 4/2
 *
 * This spell costs {1} less to cast for each creature card in your graveyard.
 * Flying
 * When this creature enters, any number of target opponents each discard a card. For each of those
 * opponents who didn't discard a card with mana value 4 or greater, draw a card.
 *
 * Cost reduction is a self-cast [ModifySpellCost] / [CostReductionSource.CardsInGraveyardMatchingFilter]
 * (mana value is unaffected — always 7, per the ruling). The ETB iterates the chosen opponents via
 * [ForEachTargetEffect]: per opponent, the opponent discards a card (Gather→Select→Move), then the
 * controller draws *unless* the discarded card had mana value 4 or greater — modeled by
 * [ConditionalOnCollectionEffect] restricting the discarded collection to MV≥4 and drawing on its
 * `ifEmpty` branch. The `ifEmpty` branch also covers an empty-handed opponent (discarded nothing →
 * didn't discard a MV≥4 card → controller draws).
 */
val HollowMarauder = card("Hollow Marauder") {
    manaCost = "{6}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Specter Rogue"
    power = 4
    toughness = 2
    oracleText = "This spell costs {1} less to cast for each creature card in your graveyard.\n" +
        "Flying\n" +
        "When this creature enters, any number of target opponents each discard a card. For each " +
        "of those opponents who didn't discard a card with mana value 4 or greater, draw a card."

    keywords(Keyword.FLYING)

    // This spell costs {1} less to cast for each creature card in your graveyard.
    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.CardsInGraveyardMatchingFilter(
                    filter = GameObjectFilter.Creature,
                    amountPerCard = 1
                )
            )
        )
    }

    // When this creature enters, any number of target opponents each discard a card. For each of
    // those opponents who didn't discard a card with mana value 4 or greater, draw a card.
    triggeredAbility {
        trigger = Triggers.self.enters()
        target(TargetOpponent(unlimited = true))
        effect = Effects.ForEachTarget(
            Effects.Pipeline {
            // The current target opponent discards a card.
            val hmHand = gather(CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)))
            val hmDiscarded = chooseExactly(
                1,
                from = hmHand,
                chooser = Chooser.TargetPlayer,
                prompt = "Choose a card to discard"
            )
            discard(hmDiscarded, Player.ContextPlayer(0))
            // Draw a card unless the discarded card had mana value 4 or greater.
            ifNotEmpty(hmDiscarded, filter = GameObjectFilter.Any.manaValueAtLeast(4)) {
                run(Effects.Nothing)
            } orElse {
                run(Effects.DrawCards(count = 1, target = EffectTarget.Controller))
            }
        }
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "90"
        artist = "Wero Gallo"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df2913d5-57c7-4f9b-bc96-8a46beef2563.jpg?1712355599"
        ruling("2024-04-12", "Hollow Marauder's first ability doesn't change its mana value, which is always 7.")
        ruling(
            "2024-04-12",
            "When the triggered ability resolves, the next target opponent in turn order chooses a " +
                "card in hand without revealing it, then each other target opponent does the same. " +
                "All chosen cards are then discarded at the same time."
        )
    }
}
