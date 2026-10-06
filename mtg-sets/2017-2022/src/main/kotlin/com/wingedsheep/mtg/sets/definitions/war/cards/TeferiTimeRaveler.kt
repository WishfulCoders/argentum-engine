package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Teferi, Time Raveler
 * {1}{W}{U}
 * Legendary Planeswalker — Teferi
 * Starting Loyalty: 4
 *
 * Each opponent can cast spells only any time they could cast a sorcery.
 * +1: Until your next turn, you may cast sorcery spells as though they had flash.
 * −3: Return up to one target artifact, creature, or enchantment to its owner's hand. Draw a card.
 *
 * The static is a [PlayersCantCastSpells] lock gated on `Not(CouldCastSorcery())` and read from the
 * caster's seat: an opponent can cast a spell only in a main phase of their own turn with the stack
 * empty (CR 307.1) — which also stops a spell cast during a resolution (cascade, discover), since the
 * resolving object is still on the stack (CR 608.2). It's a "can't", so it beats any flash
 * permission the opponent has (CR 101.2, the first ruling).
 *
 * The +1 is a duration-bounded flash grant to sorcery spells; the −3's target is optional, and an
 * illegal chosen target means the whole ability doesn't resolve and no card is drawn (CR 608.2b,
 * the second ruling).
 */
val TeferiTimeRaveler = card("Teferi, Time Raveler") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Planeswalker — Teferi"
    startingLoyalty = 4
    oracleText = "Each opponent can cast spells only any time they could cast a sorcery.\n" +
        "+1: Until your next turn, you may cast sorcery spells as though they had flash.\n" +
        "−3: Return up to one target artifact, creature, or enchantment to its owner's hand. Draw a card."

    staticAbility {
        ability = PlayersCantCastSpells(
            affected = Player.EachOpponent,
            condition = Conditions.Not(Conditions.CouldCastSorcery()),
            conditionFromCaster = true,
        )
    }

    loyaltyAbility(+1) {
        effect = Effects.GrantFlashToSpells(
            spellFilter = GameObjectFilter.Sorcery,
            duration = Duration.UntilYourNextTurn,
        )
    }

    loyaltyAbility(-3) {
        val permanent = target(TargetFilter.ArtifactCreatureOrEnchantment, optional = true)
        effect = Effects.ReturnToHand(permanent) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "221"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5cb76266-ae50-4bbc-8f96-d98f309b02d3.jpg?1783933384"

        ruling("2024-01-12", "If an effect allows opponents to cast a spell as though it had flash (for example, if your opponent also controls a Teferi, Time Raveler and activates his +1 loyalty ability), the restriction of Teferi's first ability takes precedence over that permission.")
        ruling("2024-01-12", "You may activate Teferi's last ability without choosing any target. You'll just draw a card. However, if you do choose a target and the target permanent is an illegal target by the time Teferi's last ability tries to resolve, the ability doesn't resolve. You don't draw a card.")
    }
}
