package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step

/**
 * Betor, Kin to All — Tarkir: Dragonstorm #172
 * {2}{W}{B}{G} · Legendary Creature — Spirit Dragon · 5/7
 *
 * Flying
 * At the beginning of your end step, if creatures you control have total toughness 10 or
 * greater, draw a card. Then if creatures you control have total toughness 20 or greater,
 * untap each creature you control. Then if creatures you control have total toughness 40
 * or greater, each opponent loses half their life, rounded up.
 */
val BetorKinToAll = card("Betor, Kin to All") {
    manaCost = "{2}{W}{B}{G}"
    colorIdentity = "WBG"
    typeLine = "Legendary Creature — Spirit Dragon"
    power = 5
    toughness = 7
    oracleText = "Flying\n" +
        "At the beginning of your end step, if creatures you control have total toughness 10 or " +
        "greater, draw a card. Then if creatures you control have total toughness 20 or greater, " +
        "untap each creature you control. Then if creatures you control have total toughness 40 " +
        "or greater, each opponent loses half their life, rounded up."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        // Intervening "if" (CR 603.4): the 10-toughness gate is checked both as the trigger
        // would go on the stack and again on resolution.
        interveningIf = totalToughnessAtLeast(10)
        effect = Effects.DrawCards(1) then
            // "Then if ... 20 or greater, untap each creature you control."
            Effects.If(
                condition = totalToughnessAtLeast(20),
                then = Effects.ForEachInGroup(
                    filter = GroupFilter.AllCreaturesYouControl,
                    effect = Effects.Untap(EffectTarget.IterationEntity)
                )
            ) then
            // "Then if ... 40 or greater, each opponent loses half their life, rounded up."
            // Iterated per opponent so each loses half of *their own* life total — the
            // loop rebinds the controller, so the LoseHalfLife defaults (target =
            // Controller, lifePlayer = You) read the iterated opponent.
            Effects.If(
                condition = totalToughnessAtLeast(40),
                then = Effects.ForEachPlayer(
                    players = Player.EachOpponent,
                    effect = Effects.LoseHalfLife(roundUp = true)
                )
            )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "172"
        artist = "Alexander Ostrowski"
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b475b071-5545-483e-a397-89451f258602.jpg?1743204665"
    }
}

/** "Creatures you control have total toughness [threshold] or greater." */
private fun totalToughnessAtLeast(threshold: Int): Condition = Conditions.CompareAmounts(
    DynamicAmounts.battlefield(
        Player.You,
        GameObjectFilter.Creature
    ).sumToughness(),
    ComparisonOperator.GTE,
    threshold
)
