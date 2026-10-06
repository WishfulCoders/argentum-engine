package com.wingedsheep.mtg.sets.definitions.exo.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Recurring Nightmare
 * {2}{B}
 * Enchantment
 * Sacrifice a creature, Return this enchantment to its owner's hand: Return target creature card from
 * your graveyard to the battlefield. Activate only as a sorcery.
 *
 * The same cost shape as Chthonian Nightmare (its MH3 homage) minus the energy: both the sacrifice and
 * the bounce are costs, so the ability resolves even though Recurring Nightmare is back in hand. The
 * target is chosen before costs are paid (CR 602.2b), so the creature being sacrificed can't be the
 * one returned.
 */
val RecurringNightmare = card("Recurring Nightmare") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Sacrifice a creature, Return this enchantment to its owner's hand: Return target creature " +
        "card from your graveyard to the battlefield. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Sacrifice(GameObjectFilter.Creature),
            Costs.ReturnSelfToHand,
        )
        timing = TimingRule.SorcerySpeed
        val creature = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.PutOntoBattlefieldFromGraveyard(creature)
        description = "Sacrifice a creature, Return this enchantment to its owner's hand: Return target " +
            "creature card from your graveyard to the battlefield. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "72"
        artist = "Jeff Laubenstein"
        flavorText = "\"I am confined by sleep and defined by nightmare.\"\n—Crovax"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c8173030-1c33-417c-b8e9-79231b6a85a7.jpg?1783946516"
        ruling("2013-04-15", "If you cast this as normal during your main phase, it will enter the battlefield and you'll receive priority. If no abilities trigger because of this, you can activate its ability immediately, before any other player has a chance to remove it from the battlefield.")
    }
}
