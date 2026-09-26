package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Triggers

/**
 * Soulcipher Board // Cipherbound Spirit (Innistrad: Crimson Vow)
 * {1}{U}
 * Artifact // Creature — Spirit
 *
 * Front — Soulcipher Board
 *   This artifact enters with three omen counters on it.
 *   {1}{U}, {T}: Look at the top two cards of your library. Put one of them into your graveyard.
 *   Whenever a creature card is put into your graveyard from anywhere, remove an omen counter from
 *   this artifact. Then if it has no omen counters on it, transform it.
 *
 * Back — Cipherbound Spirit (3/2)
 *   Flying
 *   This creature can block only creatures with flying.
 *   {3}{U}: Draw two cards, then discard a card.
 *
 * Built from existing primitives. The countdown is the new passive [CounterType.OMEN] counter placed
 * by a self-only [EntersWithCounters]. The tap ability is
 * [Patterns.Library.lookAtTopAndKeep] with the *kept* card going to the graveyard and the
 * remainder back on top of the library — "look at the top two, put one of them into your graveyard"
 * leaves the other where it was.
 *
 * The countdown trigger is a per-card `ZoneChangeEvent(to = GRAVEYARD)` over creature cards you
 * own, with `TriggerBinding.ANY` — deliberately **not** the batching
 * `Triggers.oneOrMore(filter).putIntoYourGraveyard()`: two creature cards hitting the graveyard at once remove two
 * counters, not one. "From anywhere" is expressed by leaving `from` unset. The follow-up
 * "Then if it has no omen counters on it" is an intervening check at resolution, so it is a
 * [Effects.If] over the *current* counter count rather than a second trigger.
 *
 * `nontoken()` is the printed word "**card**": a dying creature *token* is not a card and never
 * counts down the board (the card's second Gatherer ruling says so outright). The engine's LKI
 * matcher answers it off the death event's `wasToken` snapshot, since 704.5d has already swept the
 * token by the time the trigger is matched.
 *
 * Two triggers can be waiting on the stack together — two creature cards hitting the graveyard at
 * once — and the second one finds no counter to remove and a counter count of zero, so its "then
 * if" check passes. It does *not* flip the permanent back: CR 701.28f ignores an ability's
 * instruction to transform the permanent it's on once that permanent has transformed since the
 * ability went on the stack, which the engine enforces in `TransformEffectExecutor`.
 */

private val SoulcipherBoardFront = card("Soulcipher Board") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "This artifact enters with three omen counters on it.\n" +
        "{1}{U}, {T}: Look at the top two cards of your library. Put one of them into your graveyard.\n" +
        "Whenever a creature card is put into your graveyard from anywhere, remove an omen counter " +
        "from this artifact. Then if it has no omen counters on it, transform it."

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.OMEN,
            count = 3,
            selfOnly = true,
        )
    )

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{U}"), Costs.Tap)
        effect = Patterns.Library.lookAtTopAndKeep(
            count = 2,
            keepCount = 1,
            keepDestination = CardDestination.ToZone(Zone.GRAVEYARD),
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top),
        )
        description = "Look at the top two cards of your library. Put one of them into your graveyard."
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.ownedByYou().nontoken()).changesZone(to = Zone.GRAVEYARD)
        effect = Effects.RemoveCounters(CounterType.OMEN, 1, EffectTarget.Self) then
            Effects.If(
                condition = Conditions.CompareAmounts(
                    DynamicAmounts.countersOnSelf(CounterType.OMEN),
                    ComparisonOperator.EQ,
                    0,
                ),
                then = Effects.Transform(EffectTarget.Self),
            )
        description = "Whenever a creature card is put into your graveyard from anywhere, remove " +
            "an omen counter from this artifact. Then if it has no omen counters on it, transform it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "79"
        artist = "Caio Monteiro"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c0fae23-1278-499f-9df7-4a29691726b1.jpg?1783924889"
    }
}

private val CipherboundSpirit = card("Cipherbound Spirit") {
    manaCost = ""
    colorIdentity = "U"
    colorIndicator = "U" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Spirit"
    power = 3
    toughness = 2
    oracleText = "Flying\n" +
        "This creature can block only creatures with flying.\n" +
        "{3}{U}: Draw two cards, then discard a card."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = CanOnlyBlockCreaturesWith(
            blockerFilter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING)
        )
    }

    activatedAbility {
        cost = Costs.Mana("{3}{U}")
        effect = Effects.DrawCards(2) then Patterns.Hand.discardCards(1, EffectTarget.Controller)
        description = "Draw two cards, then discard a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "79"
        artist = "Caio Monteiro"
        imageUri = "https://cards.scryfall.io/normal/back/3/c/3c0fae23-1278-499f-9df7-4a29691726b1.jpg?1783924889"
    }
}

val SoulcipherBoard: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = SoulcipherBoardFront,
    backFace = CipherboundSpirit,
)
