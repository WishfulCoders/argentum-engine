package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GraveyardCardsHaveEscape
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect

/**
 * Underworld Breach
 * {1}{R}
 * Enchantment
 * Each nonland card in your graveyard has escape. The escape cost is equal to the card's mana cost
 * plus exile three other cards from your graveyard. (You may cast cards from your graveyard for
 * their escape cost.)
 * At the beginning of the end step, sacrifice this enchantment.
 *
 * The first ability is a whole-graveyard escape grant ([GraveyardCardsHaveEscape], CR 702.138a):
 * each nonland card in its controller's graveyard may be cast for its own mana cost plus exiling
 * three other graveyard cards, at its normal timing, and is not exiled on resolution — an escaped
 * instant or sorcery goes back to the graveyard and can escape again. A card with no mana cost has
 * an unpayable escape cost (CR 118.6). A card that also prints escape (Uro) offers both, and its
 * caster chooses which to apply.
 *
 * "At the beginning of the end step" triggers in every player's end step, not just its controller's.
 */
val UnderworldBreach = card("Underworld Breach") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Each nonland card in your graveyard has escape. The escape cost is equal to the card's " +
        "mana cost plus exile three other cards from your graveyard. (You may cast cards from your " +
        "graveyard for their escape cost.)\n" +
        "At the beginning of the end step, sacrifice this enchantment."

    staticAbility {
        ability = GraveyardCardsHaveEscape(
            filter = GameObjectFilter.Nonland,
            additionalCost = Costs.additional.ExileOtherCards(3)
        )
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        effect = SacrificeSelfEffect
        description = "At the beginning of the end step, sacrifice this enchantment."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "161"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e51d796-7279-4c06-87f0-37adbdaa41df.jpg?1783931543"

        ruling(
            "2020-01-24",
            "If a card has multiple abilities giving you permission to cast it, such as two escape abilities " +
                "or an escape ability and a flashback ability, you choose which one to apply. The others have no effect."
        )
        ruling(
            "2020-01-24",
            "If a card has no mana cost, its escape cost is an unpayable cost, so you can't cast it for that cost."
        )
        ruling(
            "2020-01-24",
            "After an escaped spell resolves, it returns to its owner's graveyard if it's not a permanent spell. " +
                "If it is a permanent spell, it enters the battlefield and will return to its owner's graveyard if " +
                "it dies later."
        )
    }
}
