package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.BudgetMode
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Season of the Bold {3}{R}{R}
 * Sorcery
 *
 * Choose up to five {P} worth of modes. You may choose the same mode more than once.
 * {P} — Create a tapped Treasure token.
 * {P}{P} — Exile the top two cards of your library. Until the end of your next turn,
 *           you may play them.
 * {P}{P}{P} — Until the end of your next turn, whenever you cast a spell, Season of the
 *             Bold deals 2 damage to up to one target creature.
 */
val SeasonOfTheBold = card("Season of the Bold") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Choose up to five {P} worth of modes. You may choose the same mode more than once.\n" +
        "{P} — Create a tapped Treasure token.\n" +
        "{P}{P} — Exile the top two cards of your library. Until the end of your next turn, you may play them.\n" +
        "{P}{P}{P} — Until the end of your next turn, whenever you cast a spell, Season of the Bold deals 2 damage to up to one target creature."

    spell {
        effect = Effects.BudgetModal(
            budget = 5,
            modes = listOf(
                // {P} — Create a tapped Treasure token
                BudgetMode(
                    cost = 1,
                    effect = Effects.CreateTreasure(tapped = true),
                    description = "Create a tapped Treasure token"
                ),
                // {P}{P} — Exile top 2 and play until end of next turn
                BudgetMode(
                    cost = 2,
                    effect = Effects.Pipeline {
                        val exiledCards = gather(CardSource.TopOfLibrary(2))
                        exile(exiledCards)
                        run(Effects.GrantMayPlayFromExile(exiledCards, MayPlayExpiry.UntilEndOfNextTurn))
                    },
                    description = "Exile the top two cards of your library. Until the end of your next turn, you may play them"
                ),
                // {P}{P}{P} — Until end of your next turn, whenever you cast a spell,
                // Season of the Bold deals 2 damage to up to one target creature
                BudgetMode(
                    cost = 3,
                    effect = Effects.CreateGlobalTriggeredAbility(
                        ability = grantedTriggeredAbility {
                            trigger = Triggers.you.casts()
                            val creature = target(TargetFilter.Creature, optional = true)
                            effect = Effects.DealDamage(
                                amount = 2,
                                target = creature
                            )
                        },
                        duration = Duration.UntilYourNextTurn
                    ),
                    description = "Until the end of your next turn, whenever you cast a spell, Season of the Bold deals 2 damage to up to one target creature"
                )
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "152"
        artist = "Eli Minaya"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84352565-558b-4f9b-a411-532147806a78.jpg?1721426701"
    }
}
