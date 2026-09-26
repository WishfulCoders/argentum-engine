package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Eerie Gravestone
 * {2}
 * Artifact
 *
 * When this artifact enters, draw a card.
 * {1}{B}, Sacrifice this artifact: Mill four cards. You may put a creature card from among
 * them into your hand. (To mill four cards, put the top four cards of your library into your
 * graveyard.)
 *
 * Implementation:
 *  - ETB trigger = [Effects.DrawCards]`(1)`.
 *  - Activated ability cost = {1}{B} + [Costs.SacrificeSelf]; effect composes the mill+pick
 *    pipeline (same shape as Cache Grab): gather top 4 → move to graveyard, then a
 *    "you may put a creature card from among them" [SelectFromCollectionEffect] filtered to
 *    [GameObjectFilter.Creature] and moved to hand.
 */
val EerieGravestone = card("Eerie Gravestone") {
    manaCost = "{2}"
    colorIdentity = "B"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, draw a card.\n" +
        "{1}{B}, Sacrifice this artifact: Mill four cards. You may put a creature card from " +
        "among them into your hand. (To mill four cards, put the top four cards of your " +
        "library into your graveyard.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
        description = "When this artifact enters, draw a card."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.SacrificeSelf)
        effect = Effects.Pipeline {
            // Mill four: gather top 4, move to graveyard.
            val milled = gather(CardSource.TopOfLibrary(4, isMill = true))
            toGraveyard(milled)
            // You may put a creature card from among them into your hand.
            val selected = chooseUpTo(
                1,
                from = milled,
                filter = GameObjectFilter.Creature,
                showAllCards = true,
                prompt = "You may put a creature card into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Leave in graveyard"
            )
            toHand(selected)
        }
        description = "{1}{B}, Sacrifice this artifact: Mill four cards. You may put a creature " +
            "card from among them into your hand."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "163"
        artist = "Lordigan"
        flavorText = "\"You've murdered a mask, but you haven't murdered a man.\"\n—Peter Parker"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/7675e91f-dba7-4e64-a7ff-1dd56665a4cc.jpg?1783905305"
    }
}
