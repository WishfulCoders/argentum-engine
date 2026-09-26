package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.Targets

/**
 * Collective Brutality — Eldritch Moon #85.
 *
 * The escalate cost is a discard rather than mana, so it rides
 * [com.wingedsheep.sdk.scripting.effects.ModalEffect.additionalCostPerExtraMode]: choosing two
 * modes discards one card, choosing three discards two.
 *
 * The third mode is *not* a drain — the 2 life gained is independent of the life actually lost, so
 * it stays a composite of a loss and a gain rather than `Effects.DrainLife`.
 */
val CollectiveBrutality = card("Collective Brutality") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Escalate—Discard a card. (Pay this cost for each mode chosen beyond the first.)\n" +
        "Choose one or more —\n" +
        "• Target opponent reveals their hand. You choose an instant or sorcery card from it. " +
        "That player discards that card.\n" +
        "• Target creature gets -2/-2 until end of turn.\n" +
        "• Target opponent loses 2 life and you gain 2 life."

    spell {
        modal(
            chooseCount = 3,
            minChooseCount = 1,
            additionalCostPerExtraMode = CostAtom.Discard(1),
        ) {
            mode("Target opponent reveals their hand. You choose an instant or sorcery card from it. That player discards that card.") {
                val opponent = target(Targets.Opponent)
                effect = Effects.Pipeline {
                    run(Effects.RevealHand(opponent))
                    val opponentHand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
                    val toDiscard = chooseExactly(
                        1,
                        from = opponentHand,
                        chooser = Chooser.Controller,
                        filter = GameObjectFilter.InstantOrSorcery,
                        prompt = "Choose an instant or sorcery card to discard",
                        alwaysPrompt = true,
                        showAllCards = true
                    )
                    discard(toDiscard, opponent.asPlayer)
                }
            }
            mode("Target creature gets -2/-2 until end of turn.") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.ModifyStats(-2, -2, creature)
            }
            mode("Target opponent loses 2 life and you gain 2 life.") {
                val opponent = target(Targets.Opponent)
                effect = Effects.LoseLife(2, opponent) then Effects.GainLife(2)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "85"
        artist = "Johann Bodin"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb94a02f-4660-45b6-8a39-941b710cf8f3.jpg?1783937489"
        ruling("2016-07-13", "You choose all of your modes at once. You can't wait to perform one mode's actions and then decide to choose more modes.")
        ruling("2016-07-13", "You can't choose any one mode multiple times.")
        ruling("2016-07-13", "If one target of an escalate spell becomes illegal, the other targets will still be affected. If all of the targets become illegal, the spell won't resolve.")
        ruling("2016-07-13", "Effects that reduce the cost of spells reduce the total cost, including any escalate costs added.")
        ruling("2016-07-13", "If an effect allows you to cast a spell that has escalate without paying its mana cost, you pay escalate costs for that spell if you choose more than one mode.")
    }
}
