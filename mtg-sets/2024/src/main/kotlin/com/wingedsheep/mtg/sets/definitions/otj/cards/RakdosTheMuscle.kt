package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Rakdos, the Muscle
 * {2}{B}{B}{R}
 * Legendary Creature — Demon Mercenary
 * 6/5
 *
 * Flying, trample
 * Whenever you sacrifice another creature, exile cards equal to its mana value from the top of
 * target player's library. Until your next end step, you may play those cards, and mana of any
 * type can be spent to cast those spells.
 * Sacrifice another creature: Rakdos gains indestructible until end of turn. Tap it. Activate only
 * once each turn.
 *
 * Implementation:
 * - Flying/trample keywords.
 * - The sacrifice trigger (`Triggers.you.sacrifices(filter, batch = true)` over creatures) targets a player and
 *   impulse-exiles the top X of that player's library, where X is the sacrificed creature's mana
 *   value — read via [EffectTarget.TriggeringEntity] (the sacrificed creature is the triggering
 *   entity; its `CardComponent.manaValue` survives in the graveyard). It then grants a may-play
 *   permission expiring at the controller's next end step ([MayPlayExpiry.UntilNextEndStep]) with
 *   `withAnyManaType = true` — the same impulse-play-with-any-mana primitive Laughing Jasper Flint
 *   uses for "mana of any type can be spent".
 * - The activated ability sacrifices another creature ([Costs.SacrificeAnother]), grants Rakdos
 *   indestructible until end of turn, and taps it, restricted to once each turn
 *   ([ActivationRestriction.OncePerTurn]). Activating it also satisfies the sacrifice trigger above.
 */
private val rakdosManaValueX: DynamicAmount =
    DynamicAmounts.triggeringManaValue()

val RakdosTheMuscle = card("Rakdos, the Muscle") {
    manaCost = "{2}{B}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Demon Mercenary"
    power = 6
    toughness = 5
    oracleText = "Flying, trample\n" +
        "Whenever you sacrifice another creature, exile cards equal to its mana value from the top " +
        "of target player's library. Until your next end step, you may play those cards, and mana " +
        "of any type can be spent to cast those spells.\n" +
        "Sacrifice another creature: Rakdos gains indestructible until end of turn. Tap it. " +
        "Activate only once each turn."

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.you.sacrifices(GameObjectFilter.Creature, batch = true)
        val targetPlayer = target(Targets.Player)
        effect = Effects.Pipeline {
            val rakdosExiled = gather(
                CardSource.TopOfLibrary(
                    count = rakdosManaValueX,
                    player = targetPlayer.asPlayer,
                )
            )
            exile(rakdosExiled, targetPlayer.asPlayer)
            run(Effects.GrantMayPlayFromExile(
                from = rakdosExiled,
                expiry = MayPlayExpiry.UntilNextEndStep,
                withAnyManaType = true,
            ))
        }
        description = "Whenever you sacrifice another creature, exile cards equal to its mana value " +
            "from the top of target player's library. Until your next end step, you may play those " +
            "cards, and mana of any type can be spent to cast those spells."
    }

    activatedAbility {
        cost = Costs.SacrificeAnother(GameObjectFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self, Duration.EndOfTurn) then
            Effects.Tap(EffectTarget.Self)
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        description = "Sacrifice another creature: Rakdos gains indestructible until end of turn. Tap it."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "226"
        artist = "Victor Maury"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb34babd-1b85-4a7d-a066-a8337805056e.jpg?1712356185"

        ruling("2024-04-12", "The number of cards exiled is determined by the mana value of the sacrificed creature as it last existed on the battlefield.")
        ruling("2024-04-12", "You can play the exiled cards only until your next end step. Any you don't play remain exiled.")
        ruling("2024-04-12", "Playing the exiled cards still uses up your one land play for the turn if any of them are lands.")
        ruling("2024-04-12", "Mana of any type can be spent to cast the exiled spells, but you still must pay those costs.")
    }
}
