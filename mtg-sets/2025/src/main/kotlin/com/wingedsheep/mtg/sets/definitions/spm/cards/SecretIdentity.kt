package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Secret Identity
 * {U}
 * Instant
 *
 * Choose one —
 * • Conceal — Until end of turn, target creature you control becomes a Citizen with base power
 *   and toughness 1/1 and gains hexproof.
 * • Reveal — Until end of turn, target creature you control becomes a Hero with base power and
 *   toughness 3/4 and gains flying and vigilance.
 *
 * Each mode is the one-shot, end-of-turn counterpart of Spider-Man No More's become-creature
 * static stack: it replaces the creature's subtypes ([Effects.SetCreatureSubtypes], Layer 4),
 * sets its base P/T ([Effects.SetBasePowerAndToughness], Layer 7b), and grants keyword(s)
 * ([Effects.GrantKeyword], Layer 6) — all with [Duration.EndOfTurn] so the whole transform
 * reverts at cleanup. The card type stays CREATURE (subtypes are merely replaced) and colors are
 * left unchanged, matching the oracle wording ("becomes a Citizen/Hero", not "becomes a blue …").
 */
val SecretIdentity = card("Secret Identity") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Conceal — Until end of turn, target creature you control becomes a Citizen with base " +
        "power and toughness 1/1 and gains hexproof.\n" +
        "• Reveal — Until end of turn, target creature you control becomes a Hero with base power " +
        "and toughness 3/4 and gains flying and vigilance."

    spell {
        effect = Effects.Modal(
            modes = listOf(
                mode("Conceal — Until end of turn, target creature you control " +
                    "becomes a Citizen with base power and toughness 1/1 and gains hexproof.") {
                    val creatureYouControl = target(TargetFilter.CreatureYouControl)
                    effect = Effects.SetCreatureSubtypes(
                        subtypes = setOf("Citizen"),
                        target = creatureYouControl,
                        duration = Duration.EndOfTurn
                    ) then
                        Effects.SetBasePowerAndToughness(
                            power = 1,
                            toughness = 1,
                            target = creatureYouControl,
                            duration = Duration.EndOfTurn
                        ) then
                        Effects.GrantKeyword(
                            keyword = Keyword.HEXPROOF,
                            target = creatureYouControl,
                            duration = Duration.EndOfTurn
                        )
                },
                mode("Reveal — Until end of turn, target creature you control becomes " +
                    "a Hero with base power and toughness 3/4 and gains flying and vigilance.") {
                    val creatureYouControl = target(TargetFilter.CreatureYouControl)
                    effect = Effects.SetCreatureSubtypes(
                        subtypes = setOf("Hero"),
                        target = creatureYouControl,
                        duration = Duration.EndOfTurn
                    ) then
                        Effects.SetBasePowerAndToughness(
                            power = 3,
                            toughness = 4,
                            target = creatureYouControl,
                            duration = Duration.EndOfTurn
                        ) then
                        Effects.GrantKeyword(
                            keyword = Keyword.FLYING,
                            target = creatureYouControl,
                            duration = Duration.EndOfTurn
                        ) then
                        Effects.GrantKeyword(
                            keyword = Keyword.VIGILANCE,
                            target = creatureYouControl,
                            duration = Duration.EndOfTurn
                        )
                }
            ),
            chooseCount = 1,
            minChooseCount = 1
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "43"
        artist = "rk post"
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37a31d84-e87b-406e-9249-fae1b5e23e72.jpg?1783905350"
    }
}
