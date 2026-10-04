package com.wingedsheep.mtg.sets.definitions.roe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Oust — Rise of the Eldrazi #40
 * {W} · Sorcery
 *
 * Put target creature into its owner's library second from the top. Its controller gains 3 life.
 *
 * The life is gained by the creature's *controller* (not its owner), so the gain is sequenced
 * before the move — while the creature is still on the battlefield and
 * [EffectTarget.TargetController] still resolves to whoever controls it — the same ordering
 * Swords to Plowshares uses. Both happen during one resolution, so nothing can observe the order.
 * Position 1 (0-indexed) is second from the top; with an empty library it becomes the only card.
 */
val Oust = card("Oust") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Put target creature into its owner's library second from the top. Its controller gains 3 life."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.GainLife(3, EffectTarget.TargetController) then
            Effects.PutIntoLibraryNthFromTop(creature, 1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "40"
        artist = "Mike Bierek"
        flavorText = "\"'Invincible' is just a word.\"\n—Gideon Jura"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07313dd3-d0dc-40ca-98a3-fa4d39e5bcae.jpg?1783942004"
        ruling("2010-06-15", "Note that the creature's controller, not its owner, is the one who gains life.")
        ruling("2010-06-15", "If the targeted creature is an illegal target by the time Oust resolves, the spell doesn't resolve. No one gains life.")
    }
}
