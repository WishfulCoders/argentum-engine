package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.core.Step

/**
 * Muerra, Trash Tactician
 * {1}{R}{G}
 * Legendary Creature — Raccoon Warrior
 * 2/4
 *
 * At the beginning of your first main phase, add {R} or {G} for each Raccoon you control.
 * Whenever you expend 4, you gain 3 life.
 * Whenever you expend 8, exile the top two cards of your library. Until the end of your
 * next turn, you may play those cards.
 *
 * Rulings:
 * - Muerra's first ability is not a mana ability. It uses the stack and can be responded to.
 * - You choose {R} or {G} for each Raccoon you control. You aren't limited to only one color of mana.
 */
val MuerraTrashTactician = card("Muerra, Trash Tactician") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Raccoon Warrior"
    power = 2
    toughness = 4
    oracleText = "At the beginning of your first main phase, add {R} or {G} for each Raccoon you control.\n" +
        "Whenever you expend 4, you gain 3 life. (You expend 4 as you spend your fourth total mana to cast spells during a turn.)\n" +
        "Whenever you expend 8, exile the top two cards of your library. Until the end of your next turn, you may play those cards."

    // At the beginning of your first main phase, add {R} or {G} for each Raccoon you control.
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.PRECOMBAT_MAIN)
        effect = Effects.AddDynamicMana(
            amount = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Creature.withSubtype(Subtype.of("Raccoon"))
            ).count(),
            allowedColors = setOf(Color.RED, Color.GREEN)
        )
    }

    // Whenever you expend 4, you gain 3 life.
    triggeredAbility {
        trigger = Triggers.you.expends(4)
        effect = Effects.GainLife(3)
    }

    // Whenever you expend 8, exile the top two cards of your library.
    // Until the end of your next turn, you may play those cards.
    triggeredAbility {
        trigger = Triggers.you.expends(8)
        effect = Effects.Pipeline {
            val exiledCards = gather(CardSource.TopOfLibrary(2))
            exile(exiledCards)
            run(Effects.GrantMayPlayFromExile(from = exiledCards, expiry = MayPlayExpiry.UntilEndOfNextTurn))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "227"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b40e4658-fd68-46d0-9a89-25570a023d19.jpg?1721427159"
        ruling("2024-07-26", "Muerra's first ability is not a mana ability. It uses the stack and can be responded to.")
        ruling("2024-07-26", "You choose {R} or {G} for each Raccoon you control. You aren't limited to only one color of mana.")
    }
}
