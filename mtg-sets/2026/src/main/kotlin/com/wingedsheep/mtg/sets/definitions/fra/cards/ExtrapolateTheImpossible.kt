package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.references.Player

private const val REVEALED = "extrapolateRevealed"

/**
 * Extrapolate the Impossible — Reality Fracture #55
 * {1}{B} · Sorcery
 *
 * You may reveal exactly two cards you own with different names from outside the game. An
 * opponent chooses one of them. You put that card into your hand.
 *
 * "Outside the game" is the owner's private `Zone.SIDEBOARD` (the wish model — see
 * `Patterns.Sideboard.wish`): an explicit sideboard in constructed, `pool − maindeck` in Limited.
 *
 * - "You may reveal **exactly two** … with different names" is all-or-nothing: an `Effects.May` over a
 *   `Gate.DoAction` whose bar is two revealed cards (`CollectionNonEmpty(min = 2)`), with the pick
 *   constrained by [SelectionRestriction.OnePerCardName]. When the sideboard doesn't hold two
 *   differently named cards the option can't be taken (CR 608.2d), so the gated-effect executor
 *   skips the yes/no and the spell simply resolves with no effect — there is no one-card fallback.
 * - "An opponent chooses one of them" is a `Chooser.Opponent` selection over the two revealed cards
 *   (in multiplayer the caster first picks which opponent decides). The chosen card goes to the
 *   caster's hand; the other stays outside the game.
 */
val ExtrapolateTheImpossible = card("Extrapolate the Impossible") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "You may reveal exactly two cards you own with different names from outside the game. " +
        "An opponent chooses one of them. You put that card into your hand."

    spell {
        effect = Effects.May(
            effect = Effects.IfYouDo(
                action = Effects.Pipeline {
                    val outside = gather(CardSource.FromZone(Zone.SIDEBOARD, Player.You))
                    val two = chooseExactly(
                        count = 2,
                        from = outside,
                        restrictions = listOf(SelectionRestriction.OnePerCardName),
                        prompt = "Reveal two cards with different names from outside the game",
                        name = REVEALED
                    )
                    reveal(two, fromZone = Zone.SIDEBOARD)
                },
                then = Effects.Pipeline {
                    val revealed = gather(CardSource.FromVariable(REVEALED))
                    val chosen = chooseExactly(
                        count = 1,
                        from = revealed,
                        chooser = Chooser.Opponent,
                        prompt = "Choose the card your opponent puts into their hand",
                        selectedLabel = "Into their hand",
                        remainderLabel = "Stays outside the game"
                    )
                    toHand(chosen)
                },
                successCriterion = SuccessCriterion.CollectionNonEmpty(REVEALED, min = 2),
            ),
            descriptionOverride = "You may reveal exactly two cards you own with different names from " +
                "outside the game. An opponent chooses one of them. You put that card into your hand.",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "55"
        artist = "Volkan Baǵa"
        flavorText = "Jace believed that if he could successfully merge the two Multiverses, he would " +
            "remove the suffering of so many."
        imageUri = "https://cards.scryfall.io/normal/front/1/7/17fb6538-493c-41aa-ad13-3e63d3ad3317.jpg?1789729624"
    }
}
