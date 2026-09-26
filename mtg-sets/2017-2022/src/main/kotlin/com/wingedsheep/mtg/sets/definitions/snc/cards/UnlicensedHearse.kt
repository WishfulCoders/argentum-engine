package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Unlicensed Hearse
 * {2}
 * Artifact — Vehicle
 * * / *
 * {T}: Exile up to two target cards from a single graveyard.
 * Unlicensed Hearse's power and toughness are each equal to the number of cards exiled with it.
 * Crew 2
 *
 * Shred Memory's single-graveyard targeting (`sameOwner`), exiling each card into the Hearse's
 * linked-exile pile, which its characteristic-defining power and toughness count
 * ([ContextPropertyKey.LINKED_EXILE_CARD_COUNT], as Veteran Survivor reads it). Crewed before
 * anything has been exiled it is a 0/0 and dies to state-based actions (ruling).
 */
val UnlicensedHearse = card("Unlicensed Hearse") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Vehicle"
    oracleText = "{T}: Exile up to two target cards from a single graveyard.\n" +
        "Unlicensed Hearse's power and toughness are each equal to the number of cards exiled with it.\n" +
        "Crew 2"

    dynamicStats(DynamicAmount.ContextProperty(ContextPropertyKey.LINKED_EXILE_CARD_COUNT))

    activatedAbility {
        cost = Costs.Tap
        target(TargetObject(
                count = 2,
                optional = true,
                filter = TargetFilter.CardInGraveyard,
                sameOwner = true,
            ),
        )
        effect = ForEachTargetEffect(
            effects = listOf(Effects.Move(EffectTarget.ContextTarget(0), Zone.EXILE, linkToSource = true))
        )
    }

    keywordAbility(KeywordAbility.crew(2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "246"
        artist = "Chris Seaman"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/93ee60f7-31dd-4bc6-b71f-57a1a0d19d20.jpg?1783923059"
        ruling("2022-04-29", "If you activated the crew ability of Unlicensed Hearse before exiling any cards with its first ability, its power and toughness will be 0/0. Unless there is another effect increasing its toughness, it will be put into its owner's graveyard as a state-based action.")
    }
}
