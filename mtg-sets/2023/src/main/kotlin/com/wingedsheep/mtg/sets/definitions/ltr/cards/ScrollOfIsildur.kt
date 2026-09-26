package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Scroll of Isildur
 * {2}{U}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Gain control of up to one target artifact for as long as you control this Saga.
 *     The Ring tempts you.
 * II — Tap up to two target creatures. Put a stun counter on each of them.
 * III — Draw a card for each tapped creature target opponent controls.
 *
 * Chapter I uses [Duration.WhileYouControlSource]: a Threaten on the Saga itself ends the
 * gained-control effect immediately, per CR 611.2b — that is what "as long as you control
 * this Saga" actually means (a vanilla [Duration.WhileSourceOnBattlefield] would mis-handle
 * the steal). The Ring-tempt rider is composed with [CompositeEffect] after the optional
 * gain-control resolves; if the player passes on the gain-control target, the tempt half
 * still fires.
 *
 * Chapter II runs `Tap + AddCounters(STUN, 1)` per chosen target via [ForEachTargetEffect].
 * The stun-counter mechanic is fully wired (CR 122.1d — `untapOrConsumeStun`).
 *
 * Chapter III counts tapped creatures controlled by the targeted opponent via
 * [DynamicAmount.Count] over `Creature.tapped()` filtered to `Player.TargetOpponent`'s
 * battlefield, and draws that many cards.
 */
val ScrollOfIsildur = card("Scroll of Isildur") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Gain control of up to one target artifact for as long as you control this Saga. The Ring tempts you.\n" +
        "II — Tap up to two target creatures. Put a stun counter on each of them.\n" +
        "III — Draw a card for each tapped creature target opponent controls."

    sagaChapter(1) {
        val artifact = target(TargetFilter.Artifact, optional = true)
        effect = Effects.GainControl(artifact, Duration.WhileYouControlSource("Scroll of Isildur")) then
            Effects.TheRingTemptsYou()
    }

    sagaChapter(2) {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.Tap(EffectTarget.ContextTarget(0)),
            Effects.AddCounters(CounterType.STUN, 1, EffectTarget.ContextTarget(0))
        )
    }

    sagaChapter(3) {
        target(Targets.Opponent)
        effect = Effects.DrawCards(
            count = DynamicAmounts.count(
                Player.TargetOpponent,
                Zone.BATTLEFIELD,
                GameObjectFilter.Creature.tapped()
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "69"
        artist = "Audrey Benjaminsen"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/195821f4-ba3d-4412-930f-f3656b319dfd.jpg?1688569264"
    }
}
