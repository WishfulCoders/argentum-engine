package com.wingedsheep.mtg.sets.definitions.cns.cards

import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dack Fayden — Conspiracy #42
 * {1}{U}{R} · Legendary Planeswalker — Dack · Loyalty 3
 *
 * +1: Target player draws two cards, then discards two cards.
 * −2: Gain control of target artifact.
 * −6: You get an emblem with "Whenever you cast a spell that targets one or more permanents, gain
 * control of those permanents."
 *
 * Both control changes last indefinitely, even after Dack leaves the battlefield (ruling). The
 * emblem is a global triggered ability that captures the spell's permanent targets when it
 * triggers (`SpellCastPredicate.TargetsMatching` + [CollectionSlot.TriggerCaptured]), so its
 * ability — which resolves before the spell — takes them even if the spell is countered or its
 * targets change in response (CR 113.7a).
 */
val DackFayden = card("Dack Fayden") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Planeswalker — Dack"
    startingLoyalty = 3
    oracleText = "+1: Target player draws two cards, then discards two cards.\n" +
        "−2: Gain control of target artifact.\n" +
        "−6: You get an emblem with \"Whenever you cast a spell that targets one or more permanents, " +
        "gain control of those permanents.\""

    loyaltyAbility(+1) {
        val player = target(Targets.Player)
        effect = Effects.DrawCards(2, player) then Patterns.Hand.discardCards(2, player)
    }

    loyaltyAbility(-2) {
        val artifact = target(TargetFilter.Artifact)
        effect = Effects.GainControl(artifact)
    }

    loyaltyAbility(-6) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.you.casts(
                    requires = setOf(SpellCastPredicate.TargetsMatching(GameObjectFilter.Permanent))
                )
                effect = Effects.ForEachInCollection(
                    collection = CollectionSlot.TriggerCaptured,
                    effect = Effects.GainControl(EffectTarget.IterationEntity)
                )
            },
            descriptionOverride = "Whenever you cast a spell that targets one or more permanents, " +
                "gain control of those permanents."
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "42"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3fcb7810-1054-4001-855c-6e17939b3d3f.jpg?1783939372"

        ruling(
            "2016-06-08",
            "The targeted player draws two cards and discards two cards all while Dack's first ability is " +
                "resolving. Nothing can happen between the two, and no player may choose to take actions."
        )
        ruling("2016-06-08", "The ability of Dack's emblem will resolve before the spell that caused it to trigger.")
        ruling(
            "2016-06-08",
            "The effect of Dack's second ability and the effect of the emblem's ability last indefinitely. " +
                "You won't lose control of the permanents if Dack leaves the battlefield."
        )
    }
}
