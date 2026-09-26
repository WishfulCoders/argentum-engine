package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantMayCastFromLinkedExile

/**
 * Null Summoner (Reality Fracture #142) — {2}{U}{B} Creature — Human Warlock 4/2.
 *
 * - The enters trigger is gated by an intervening "if you cast it" ([Conditions.WasCast]). It
 *   reveals the target opponent's hand, you choose a nonland card, and that card is exiled
 *   *linked to Null Summoner* (`linkToSource = true`), which is what "the exiled card" names.
 * - The threshold line is a [GrantMayCastFromLinkedExile] static wrapped in a
 *   seven-cards-in-your-graveyard condition, so the permission switches on and off with the
 *   graveyard count and ends for good when Null Summoner leaves the battlefield (the card stays
 *   exiled). `withAnyManaType` relaxes the colored pips of that one cast (CR 609.4b).
 */
val NullSummoner = card("Null Summoner") {
    manaCost = "{2}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Human Warlock"
    power = 4
    toughness = 2
    oracleText = "When this creature enters, if you cast it, target opponent reveals their hand. " +
        "You choose a nonland card from it. Exile that card.\n" +
        "Threshold — As long as there are seven or more cards in your graveyard, you may cast the " +
        "exiled card, and mana of any type can be spent to cast that spell."

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasCast
        val opponent = target(Targets.Opponent)
        effect = Patterns.Hand.revealHandAndExileChosen(target = opponent, linkToSource = true)
    }

    staticAbility {
        ability = GrantMayCastFromLinkedExile(withAnyManaType = true)
        condition = Conditions.CardsInGraveyardAtLeast(7)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "142"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3afdc75a-1bf5-4f2f-84eb-d82f77a095cd.jpg?1789127648"
        inBooster = false
    }
}
