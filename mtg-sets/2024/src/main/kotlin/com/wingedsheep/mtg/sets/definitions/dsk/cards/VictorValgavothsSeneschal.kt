package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.IncrementAbilityResolutionCountEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Victor, Valgavoth's Seneschal — Duskmourn: House of Horror #238
 * {1}{W}{B} · Legendary Creature — Human Warlock · 3/3
 *
 * Eerie — Whenever an enchantment you control enters and whenever you fully unlock a Room, surveil 2
 * if this is the first time this ability has resolved this turn. If it's the second time, each
 * opponent discards a card. If it's the third time, put a creature card from a graveyard onto the
 * battlefield under your control.
 *
 * One Eerie ability with two trigger conditions (an enchantment you control entering, and fully
 * unlocking a Room) and an escalating payoff keyed off how many times *this ability* has resolved
 * this turn. Modeled as the two standard Eerie triggers (cf. Ghostly Dancers), each running the same
 * [eerieEscalation] effect: it bumps the source's per-turn resolution counter
 * ([IncrementAbilityResolutionCountEffect]) and then branches on the exact count via
 * [Conditions.SourceAbilityResolvedNTimes] (== n). Because the counter lives on the *source
 * permanent* (not per-ability), both triggers share it, so a Room unlock followed by an enchantment
 * ETB escalate as one sequence — surveil 2, then each opponent discards, then reanimate; the 4th+
 * resolution does nothing.
 *
 * The reanimation gathers creature cards from *every* graveyard (`Player.Each` fans the gather across
 * all players' graveyards — "a graveyard"), lets you pick one, and puts it onto the battlefield under
 * your control (the default control for a battlefield move by the resolving controller).
 */
val VictorValgavothsSeneschal = card("Victor, Valgavoth's Seneschal") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Human Warlock"
    power = 3
    toughness = 3
    oracleText = "Eerie — Whenever an enchantment you control enters and whenever you fully unlock " +
        "a Room, surveil 2 if this is the first time this ability has resolved this turn. If it's " +
        "the second time, each opponent discards a card. If it's the third time, put a creature " +
        "card from a graveyard onto the battlefield under your control."

    keywords(Keyword.EERIE)

    // Eerie — part 1: whenever an enchantment you control enters.
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Enchantment.youControl()).enters()
        effect = eerieEscalation()
        description = "Eerie — Whenever an enchantment you control enters, surveil 2 if this is the " +
            "first time this ability has resolved this turn. If it's the second time, each opponent " +
            "discards a card. If it's the third time, put a creature card from a graveyard onto the " +
            "battlefield under your control."
    }

    // Eerie — part 2: whenever you fully unlock a Room.
    triggeredAbility {
        trigger = Triggers.you.fullyUnlocksARoom()
        effect = eerieEscalation()
        description = "Eerie — Whenever you fully unlock a Room, surveil 2 if this is the first time " +
            "this ability has resolved this turn. If it's the second time, each opponent discards a " +
            "card. If it's the third time, put a creature card from a graveyard onto the battlefield " +
            "under your control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "238"
        artist = "Jeremy Wilson"
        imageUri = "https://cards.scryfall.io/normal/front/5/1/51392ece-c9f5-46b0-9dce-0a1a0343a536.jpg?1726286757"
    }
}

/**
 * The shared Eerie payoff: increment the source's per-turn resolution count, then run exactly one
 * tier based on whether this is the 1st / 2nd / 3rd resolution this turn.
 */
private fun eerieEscalation(): Effect = IncrementAbilityResolutionCountEffect then
    // 1st time — surveil 2.
    Effects.If(
        condition = Conditions.SourceAbilityResolvedNTimes(1),
        then = Patterns.Library.surveil(2),
    ) then
    // 2nd time — each opponent discards a card.
    Effects.If(
        condition = Conditions.SourceAbilityResolvedNTimes(2),
        then = Effects.EachOpponentDiscards(1),
    ) then
    // 3rd time — put a creature card from a graveyard onto the battlefield under your control.
    Effects.If(
        condition = Conditions.SourceAbilityResolvedNTimes(3),
        then = Effects.Pipeline {
            val victorReanimatable = gather(
                CardSource.FromZone(Zone.GRAVEYARD, Player.Each, GameObjectFilter.Creature)
            )
            val victorReanimated = chooseExactly(
                1,
                from = victorReanimatable,
                showAllCards = true,
                prompt = "Put a creature card from a graveyard onto the battlefield under your control"
            )
            move(victorReanimated, CardDestination.ToZone(Zone.BATTLEFIELD))
        },
    )
