package com.wingedsheep.mtg.sets.definitions.mir.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction

/**
 * Lion's Eye Diamond — Mirage #307
 * {0} · Artifact
 *
 * Discard your hand, Sacrifice this artifact: Add three mana of any one color. Activate only as an
 * instant.
 *
 * A mana ability (CR 605.1 — "regardless of … what timing restrictions (such as 'Activate only as an
 * instant') [it] may have"), so it doesn't use the stack and can't be responded to (CR 605.3b). The
 * printed restriction is [ActivationRestriction.OnlyAsInstant] (CR 602.5e): it can be activated only
 * while its controller holds priority, never in the middle of casting a spell or paying a cost — the
 * other two windows CR 605.3a would otherwise give a mana ability. So the hand is discarded *before*
 * the spell the mana pays for is cast, which is the whole point of the card.
 */
val LionsEyeDiamond = card("Lion's Eye Diamond") {
    manaCost = "{0}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Discard your hand, Sacrifice this artifact: Add three mana of any one color. Activate only as an instant."

    activatedAbility {
        cost = Costs.Composite(Costs.DiscardHand, Costs.SacrificeSelf)
        effect = Effects.AddAnyColorMana(3)
        manaAbility = true
        restrictions = listOf(ActivationRestriction.OnlyAsInstant)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "307"
        artist = "Margaret Organ-Kean"
        flavorText = "Held in the lion's eye\n—Zhalfirin saying meaning \"caught in the moment of crisis\""
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63bacc32-d6ba-420c-9b49-299c08e5fb39.jpg?1783947041"
        ruling("2004-10-04", "The ability is a mana ability, so it is activated and resolves as a mana ability, but it can only be activated at times when you can cast an instant. Yes, this is a bit weird.")
    }
}
