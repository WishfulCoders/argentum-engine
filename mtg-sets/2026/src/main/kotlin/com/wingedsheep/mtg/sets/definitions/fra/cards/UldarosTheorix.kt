package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Uldaros Theorix — Reality Fracture #159.
 *
 * - "if you cast him" is the intervening-if [Conditions.WasCast] (CR 603.4), checked on trigger
 *   and again on resolution.
 * - "up to one target nonland card of each card type from your graveyard" is one instance of
 *   "target" with the [TargetObject.onePerCardType] cross-target rule: each chosen card fills a
 *   different card-type slot it qualifies for (an artifact creature fills either the artifact or
 *   the creature slot, never both — CR 115.3).
 * - The legal targets are exiled, copied as a group (the copies are created in exile, CR 707.12),
 *   and the controller may cast any number of the copies whose mana values total 6 or less for
 *   free, during the trigger's resolution. The budget is spent per cast, so only copies that still
 *   fit are offered. Cast permanent copies become tokens and uncast copies cease to exist
 *   (CR 707.10a); the exiled originals stay in exile.
 */
val UldarosTheorix = card("Uldaros Theorix") {
    manaCost = "{3}{U}{B}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Elder Sphinx"
    power = 5
    toughness = 5
    oracleText = "Flying\n" +
        "When Uldaros Theorix enters, if you cast him, exile up to one target nonland card of each " +
        "card type from your graveyard. Copy those cards. You may cast any number of spells with " +
        "total mana value 6 or less from among the copies without paying their mana costs. " +
        "(Permanent spells cast this way become tokens.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasCast
        targets(
            TargetFilter(GameObjectFilter.Nonland.ownedByYou(), zone = Zone.GRAVEYARD),
            unlimited = true,
            onePerCardType = true,
        )
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.ChosenTargets)
            exile(exiled)
            val copies = copyCards(exiled)
            run(Effects.CastWithTotalManaValueFromCollectionWithoutPayingCost(from = copies, maxTotalManaValue = 6))
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "159"
        artist = "Alexander Mokhov"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7ad622a-42ff-48fa-ae95-12e0a5bd9387.jpg?1788878220"
        inBooster = false
    }
}
