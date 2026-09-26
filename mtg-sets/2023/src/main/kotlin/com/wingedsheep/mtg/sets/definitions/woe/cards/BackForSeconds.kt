package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.bargain
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Back for Seconds
 * {2}{B}
 * Sorcery
 *
 * Bargain
 * Return up to two target creature cards from your graveyard to your hand. If this spell was
 * bargained, you may put one of those cards with mana value 4 or less onto the battlefield
 * instead of putting it into your hand.
 *
 * Targets are chosen while casting, but the optional battlefield choice happens on resolution
 * after target legality is checked. Moving the chosen eligible card first and then filtering the
 * original target collection to cards still in the graveyard implements "instead"; the selected
 * card cannot subsequently be moved to hand by the final step.
 */
val BackForSeconds = card("Back for Seconds") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Bargain (You may sacrifice an artifact, enchantment, or token as you cast this " +
        "spell.)\n" +
        "Return up to two target creature cards from your graveyard to your hand. If this spell " +
        "was bargained, you may put one of those cards with mana value 4 or less onto the " +
        "battlefield instead of putting it into your hand."

    bargain()

    spell {
        targets(TargetFilter.CreatureInYourGraveyard, count = 2, optional = true)
        effect = Effects.Pipeline {
            val backForSecondsTargets = gather(CardSource.ChosenTargets)
            run(Effects.If(
                condition = Conditions.WasBargained,
                then = Effects.Pipeline {
                    val backForSecondsReanimated = chooseUpTo(
                        1,
                        from = backForSecondsTargets,
                        filter = GameObjectFilter.Creature.manaValueAtMost(4),
                        prompt = "Put up to one creature card with mana value 4 or less onto the battlefield"
                    )
                    move(
                        backForSecondsReanimated,
                        CardDestination.ToZone(Zone.BATTLEFIELD),
                        underOwnersControl = true
                    )
                    val backForSecondsToHand = filter(
                        backForSecondsTargets,
                        GameObjectFilter.Any.currentlyIn(Zone.GRAVEYARD)
                    )
                    toHand(backForSecondsToHand)
                },
                otherwise = Effects.Pipeline { toHand(backForSecondsTargets) },
            ))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "80"
        artist = "Julia Metzger"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/660845b5-96fa-4484-822b-aa0508801306.jpg?1783915111"

        ruling(
            "2023-09-01",
            "If you bargain this spell, you can still choose not to put one of the cards onto the " +
                "battlefield, even if at least one is eligible.",
        )
    }
}
