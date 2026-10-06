package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kumano Faces Kakkazan // Etching of Kumano — Kamigawa: Neon Dynasty #152
 * {R} · Enchantment — Saga // Enchantment Creature — Human Shaman 2/2 · Uncommon
 *
 * Front — Kumano Faces Kakkazan:
 *   (As this Saga enters and after your draw step, add a lore counter.)
 *   I — This Saga deals 1 damage to each opponent and each planeswalker they control.
 *   II — When you next cast a creature spell this turn, that creature enters with an additional
 *        +1/+1 counter on it.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 *
 * Back — Etching of Kumano (red color indicator):
 *   Haste
 *   If a creature dealt damage this turn by a source you controlled would die, exile it instead.
 *
 * Modeling notes:
 *  - Chapter I is untargeted group damage, the End the Festivities shape: each opponent, then each
 *    planeswalker those opponents control, with the Saga as the source.
 *  - Chapter II is the Summon: Fenrir shape — a one-shot delayed trigger for the rest of the turn on
 *    your next creature spell, putting the +1/+1 counter on the spell so it enters with it.
 *  - Chapter III is the standard transforming-Saga final chapter
 *    ([Effects.ExileAndReturnTransformed]); the returned permanent is a new object (CR 400.7).
 *  - Etching's replacement reads `wasDealtDamageBySourceYouControlledThisTurn()`: the dying
 *    creature's own record of the sources that damaged it this turn and who controlled each one
 *    *when it dealt the damage* (CR 608.2h). Any creature counts, yours included, and any death — not
 *    just death from that damage. The source may have left the battlefield since (a burn spell, a
 *    creature that traded), and Etching itself need only be on the battlefield when the creature
 *    would die. "You" is Etching's controller (CR 614.1a — a replacement, not a trigger).
 */
private val EtchingOfKumano = card("Etching of Kumano") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R"
    typeLine = "Enchantment Creature — Human Shaman"
    oracleText = "Haste\n" +
        "If a creature dealt damage this turn by a source you controlled would die, exile it instead."
    power = 2
    toughness = 2

    keywords(Keyword.HASTE)

    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.wasDealtDamageBySourceYouControlledThisTurn(),
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD,
            ),
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "152"
        artist = "Mike Bierek"
        flavorText = "Many kami came to demand a secret he did not know, and every one of them fell."
        imageUri = "https://cards.scryfall.io/normal/back/2/8/28d92cad-45b0-479d-97fc-c175f3a3f259.jpg?1783923870"
    }
}

private val KumanoFacesKakkazanFront = card("Kumano Faces Kakkazan") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I — This Saga deals 1 damage to each opponent and each planeswalker they control.\n" +
        "II — When you next cast a creature spell this turn, that creature enters with an additional " +
        "+1/+1 counter on it.\n" +
        "III — Exile this Saga, then return it to the battlefield transformed under your control."

    // I — This Saga deals 1 damage to each opponent and each planeswalker they control.
    sagaChapter(1) {
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Planeswalker.opponentControls()),
                Effects.DealDamage(1, EffectTarget.IterationEntity)
            )
    }

    // II — When you next cast a creature spell this turn, that creature enters with an additional
    // +1/+1 counter on it.
    sagaChapter(2) {
        effect = Effects.CreateDelayedTrigger(
            trigger = Triggers.you.casts(GameObjectFilter.Creature),
            fireOnce = true,
            expiry = DelayedTriggerExpiry.EndOfTurn,
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.TriggeringEntity),
        )
    }

    // III — Exile this Saga, then return it to the battlefield transformed under your control.
    sagaChapter(3) {
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "152"
        artist = "Mike Bierek"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28d92cad-45b0-479d-97fc-c175f3a3f259.jpg?1783923870"
    }
}

val KumanoFacesKakkazan: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = KumanoFacesKakkazanFront,
    backFace = EtchingOfKumano,
)
