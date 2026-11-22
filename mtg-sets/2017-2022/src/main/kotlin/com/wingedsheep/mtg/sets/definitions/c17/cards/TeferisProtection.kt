package com.wingedsheep.mtg.sets.definitions.c17.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Teferi's Protection — Commander 2017 #8 (canonical printing)
 * {2}{W} · Instant
 *
 * Until your next turn, your life total can't change and you gain protection from everything.
 * All permanents you control phase out. (While they're phased out, they're treated as though they
 * don't exist. They phase in before you untap during your untap step.)
 * Exile Teferi's Protection.
 *
 * - "Your life total can't change" is both player locks (CR 119.7–8, Flare of Fortitude), here
 *   lasting until your next turn.
 * - "Protection from everything" for a player is The One Ring's `GrantPlayerProtection`.
 * - Each permanent you control phases out directly; Auras and Equipment attached to them that you
 *   don't control phase out indirectly with them (CR 702.26g, handled by the phase-out executor).
 *   Everything phases back in before your next untap step.
 * - "Exile Teferi's Protection" replaces the graveyard as its destination on resolution.
 */
val TeferisProtection = card("Teferi's Protection") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Until your next turn, your life total can't change and you gain protection from everything. " +
        "All permanents you control phase out. (While they're phased out, they're treated as though they don't exist. " +
        "They phase in before you untap during your untap step.)\nExile Teferi's Protection."

    spell {
        effect = Effects.LockLifeGain(EffectTarget.Controller, Duration.UntilYourNextTurn) then
            Effects.LockLifeLoss(EffectTarget.Controller, Duration.UntilYourNextTurn) then
            Effects.GrantPlayerProtection(
                target = EffectTarget.Controller,
                scope = ProtectionScope.Everything,
                duration = Duration.UntilYourNextTurn
            ) then
            Effects.ForEachInGroup(Filters.Group.permanentsYouControl, Effects.PhaseOut(EffectTarget.IterationEntity))
        selfExile()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "8"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/7/7/77f130c7-0138-4a1a-9f67-62d2c302dc48.jpg?1783935950"
        ruling("2017-08-25", "Gaining protection from everything causes a spell or ability on the stack to have an illegal target if it targets you.")
        ruling("2017-08-25", "Spells and abilities that would normally cause you to gain or lose life still resolve while your life total can't change, but the life-gain or life-loss part simply has no effect.")
        ruling("2017-08-25", "You can't pay a cost that includes the payment of any amount of life other than 0 life.")
        ruling("2017-08-25", "Phasing out doesn't cause any \"leaves the battlefield\" abilities to trigger. Similarly, phasing in won't cause any \"enters\" abilities to trigger.")
        ruling("2017-08-25", "If a token is phased out, it will phase in as your next untap step begins. This is a change from previous rules.")
    }
}
