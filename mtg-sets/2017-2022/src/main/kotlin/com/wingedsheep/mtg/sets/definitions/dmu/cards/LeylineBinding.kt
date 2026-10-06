package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Leyline Binding
 * {5}{W}
 * Enchantment
 *
 * Flash
 * Domain — This spell costs {1} less to cast for each basic land type among lands you control.
 * When this enchantment enters, exile target nonland permanent an opponent controls until this
 * enchantment leaves the battlefield.
 *
 * The domain discount is a self cost reduction (CR 601.2f) priced from the basic land types among
 * the caster's lands; it never changes the mana value, which stays 6. The exile is one ability
 * with a duration (CR 610.3), not Oblivion Ring's two triggers: if Leyline Binding has already left
 * the battlefield when the trigger resolves, nothing is exiled ([Effects.MoveUntilSourceLeaves]).
 */
val LeylineBinding = card("Leyline Binding") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Flash\n" +
        "Domain — This spell costs {1} less to cast for each basic land type among lands you control.\n" +
        "When this enchantment enters, exile target nonland permanent an opponent controls until this " +
        "enchantment leaves the battlefield."

    keywords(Keyword.FLASH)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(DynamicAmounts.domain()),
            ),
        )
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(TargetFilter.NonlandPermanentOpponentControls)
        effect = Effects.MoveUntilSourceLeaves(permanent, Zone.EXILE)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "24"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c3ac3dd-35db-447f-8674-37b4680a1ef7.jpg?1783921363"
        ruling("2024-04-12", "Leyline Binding's domain ability doesn't change its mana value, which is always 6.")
        ruling("2022-09-09", "If Leyline Binding leaves the battlefield before its enters-the-battlefield ability resolves, the target permanent won't be exiled.")
        ruling("2022-09-09", "If a token is exiled, it ceases to exist. It won't be returned to the battlefield.")
        ruling("2022-09-09", "Auras attached to the exiled permanent will be put into their owners' graveyards. Equipment attached to an exiled creature will become unattached and remain on the battlefield. Any counters on the exiled permanent will cease to exist.")
        ruling("2022-09-09", "Leyline Binding's last ability is a single ability that creates two one-shot effects: one that exiles the permanent when the ability resolves, and another that returns the exiled card to the battlefield immediately after Leyline Binding leaves the battlefield.")
    }
}
