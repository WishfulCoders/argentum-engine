package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Wrenn and Six — Modern Horizons #217
 * {R}{G} · Legendary Planeswalker — Wrenn · Starting loyalty 3
 *
 * +1: Return up to one target land card from your graveyard to your hand.
 * −1: Wrenn and Six deals 1 damage to any target.
 * −7: You get an emblem with "Instant and sorcery cards in your graveyard have retrace."
 *
 * Retrace (CR 702.81a) is "you may cast this card from your graveyard by discarding a land card as
 * an additional cost to cast it" — Six's graveyard-cast grant ([MayCastFromGraveyard] owing
 * [Costs.additional.DiscardCards] of a land), here over instant and sorcery cards and owned by
 * the emblem (`ownedStaticAbilities`, the Wrenn and Realmbreaker route) so it outlives Wrenn and
 * works on either player's turn at the card's normal timing.
 */
val WrennAndSix = card("Wrenn and Six") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Planeswalker — Wrenn"
    startingLoyalty = 3
    oracleText = "+1: Return up to one target land card from your graveyard to your hand.\n" +
        "−1: Wrenn and Six deals 1 damage to any target.\n" +
        "−7: You get an emblem with \"Instant and sorcery cards in your graveyard have retrace.\" " +
        "(You may cast instant and sorcery cards from your graveyard by discarding a land card in " +
        "addition to paying their other costs.)"

    loyaltyAbility(+1) {
        val land = target(TargetFilter(GameObjectFilter.Land.ownedByYou(), zone = Zone.GRAVEYARD), optional = true)
        effect = Effects.ReturnToHand(land)
        description = "Return up to one target land card from your graveyard to your hand."
    }

    loyaltyAbility(-1) {
        val any = target(Targets.Any)
        effect = Effects.DealDamage(1, any)
        description = "Wrenn and Six deals 1 damage to any target."
    }

    loyaltyAbility(-7) {
        effect = Effects.CreatePermanentEmblem(
            ownedStaticAbilities = listOf(
                MayCastFromGraveyard(
                    filter = GameObjectFilter.InstantOrSorcery,
                    additionalCost = Costs.additional.DiscardCards(1, GameObjectFilter.Land)
                )
            ),
            emblemDescription = "Instant and sorcery cards in your graveyard have retrace."
        )
        description = "You get an emblem with \"Instant and sorcery cards in your graveyard have retrace.\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "217"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a706ecf-3277-40e3-871c-4ba4ead16e20.jpg?1783933076"

        ruling(
            "2019-06-14",
            "When a spell you cast with retrace resolves or is countered, it's put back into your graveyard. You " +
                "may use the retrace ability to cast it again."
        )
    }
}
