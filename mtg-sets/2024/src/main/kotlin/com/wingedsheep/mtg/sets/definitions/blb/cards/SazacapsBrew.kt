package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sazacap's Brew
 * {1}{R}
 * Instant
 *
 * Gift a tapped Fish (You may promise an opponent a gift as you cast this spell.
 * If you do, they create a tapped 1/1 blue Fish creature token before its other effects.)
 *
 * As an additional cost to cast this spell, discard a card.
 *
 * Target player draws two cards. If the gift was promised, target creature you
 * control gets +2/+0 until end of turn.
 *
 * Note: Gift is modeled as a modal choice. Mode 1 = no gift, Mode 2 = gift.
 */
val SazacapsBrew = card("Sazacap's Brew") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Gift a tapped Fish (You may promise an opponent a gift as you cast this spell. If you do, they create a tapped 1/1 blue Fish creature token before its other effects.)\nAs an additional cost to cast this spell, discard a card.\nTarget player draws two cards. If the gift was promised, target creature you control gets +2/+0 until end of turn."

    additionalCost(Costs.additional.DiscardCards())

    spell {
        effect = Patterns.Mechanic.giftSpell(
            // Mode 1: No gift — target player draws 2
            mode("Don't promise a gift — target player draws two cards") {
                val player = target(Targets.Player)
                effect = Effects.DrawCards(2, player)
            },
            // Mode 2: Gift a tapped Fish — opponent gets Fish token, target player draws 2,
            // target creature you control gets +2/+0 until end of turn
            mode("Promise a gift — opponent creates a tapped 1/1 blue Fish token, target player draws two cards, target creature you control gets +2/+0 until end of turn") {
                val player = target(Targets.Player)
                val creatureYouControl = target(TargetFilter.CreatureYouControl)
                effect = Effects.CreateToken(
                    count = 1,
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.BLUE),
                    creatureTypes = setOf("Fish"),
                    tapped = true,
                    controller = EffectTarget.PlayerRef(Player.ChosenOpponent),
                    imageUri = "https://cards.scryfall.io/normal/front/d/e/de0d6700-49f0-4233-97ba-cef7821c30ed.jpg?1721431109"
                ) then
                    Effects.DrawCards(2, player) then
                    Effects.ModifyStats(
                        power = 2,
                        toughness = 0,
                        target = creatureYouControl,
                        duration = Duration.EndOfTurn
                    ) then
                    Effects.GiftGiven()
            }
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "151"
        artist = "Sam Guay"
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6d963080-b3ec-467d-82f7-39db6ecd6bbc.jpg?1721426699"
    }
}
