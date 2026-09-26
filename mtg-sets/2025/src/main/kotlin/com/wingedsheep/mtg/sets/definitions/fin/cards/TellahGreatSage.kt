package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter


/**
 * Tellah, Great Sage
 * {3}{U}{R}
 * Legendary Creature — Human Wizard
 * 3/3
 * Whenever you cast a noncreature spell, create a 1/1 colorless Hero creature token. If four or
 * more mana was spent to cast that spell, draw two cards. If eight or more mana was spent to cast
 * that spell, sacrifice Tellah and it deals that much damage to each opponent.
 */
val TellahGreatSage = card("Tellah, Great Sage") {
    manaCost = "{3}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Human Wizard"
    oracleText = "Whenever you cast a noncreature spell, create a 1/1 colorless Hero creature token. If four or more mana was spent to cast that spell, draw two cards. If eight or more mana was spent to cast that spell, sacrifice Tellah and it deals that much damage to each opponent."
    power = 3
    toughness = 3
    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Hero"),
            imageUri = "https://cards.scryfall.io/normal/front/d/0/d0657ce1-bf75-4007-ac1b-0623eb263357.jpg?1748704030",
        ) then
            Effects.If(
                condition = Conditions.CompareAmounts(
                    DynamicAmounts.manaSpentOnTriggeringSpell(),
                    ComparisonOperator.GTE,
                    4
                ),
                then = Effects.DrawCards(2)
            ) then
            Effects.If(
                condition = Conditions.CompareAmounts(
                    DynamicAmounts.manaSpentOnTriggeringSpell(),
                    ComparisonOperator.GTE,
                    8
                ),
                then = SacrificeSelfEffect then
                    Effects.DealDamage(
                        DynamicAmounts.manaSpentOnTriggeringSpell(),
                        EffectTarget.PlayerRef(Player.EachOpponent)
                    )
            )
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "244"
        artist = "Yumi Yaoshida"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a67793ef-ef80-4434-9c54-e3fd8a270bbe.jpg?1748706698"
    }
}
