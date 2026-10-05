package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.LookAudience
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Robber of the Rich
 * {1}{R} — Creature — Human Archer Rogue 2/2 (Mythic) — Throne of Eldraine #138
 * Artist: Paul Scott Canavan
 *
 * Reach, haste
 * Whenever this creature attacks, if defending player has more cards in hand than you, exile the
 * top card of their library. During any turn you attacked with a Rogue, you may cast that card and
 * you may spend mana as though it were mana of any color to cast that spell.
 *
 * - The hand-size comparison is an intervening "if" ([interveningIf]): checked when the attack
 *   trigger would fire and again on resolution (ruling 2019-10-04).
 * - The exiled card gets a permanent cast permission (it outlives Robber — ruling 2019-10-04),
 *   gated by "you attacked with a Rogue this turn", re-evaluated whenever the permission is
 *   checked. `nonLandOnly` because "cast" never lets you play a land; normal timing still applies.
 * - "Spend mana as though it were mana of any color" is `withAnyManaType`, as on
 *   Curse of Hospitality.
 */
val RobberOfTheRich = card("Robber of the Rich") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Archer Rogue"
    power = 2
    toughness = 2
    oracleText = "Reach, haste\n" +
        "Whenever this creature attacks, if defending player has more cards in hand than you, exile " +
        "the top card of their library. During any turn you attacked with a Rogue, you may cast that " +
        "card and you may spend mana as though it were mana of any color to cast that spell."

    keywords(Keyword.REACH, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.zone(Player.DefendingPlayer, Zone.HAND).count(),
            ComparisonOperator.GT,
            DynamicAmounts.zone(Player.You, Zone.HAND).count()
        )
        effect = Effects.Pipeline {
            val robbed = gather(
                CardSource.TopOfLibrary(count = 1, player = Player.DefendingPlayer),
                lookAudience = LookAudience.None
            )
            exile(robbed, Player.DefendingPlayer)
            run(
                Effects.GrantMayPlayFromExile(
                    from = robbed,
                    expiry = MayPlayExpiry.Permanent,
                    withAnyManaType = true,
                    nonLandOnly = true,
                    condition = Conditions.YouAttackedWithCreaturesThisTurn(
                        GameObjectFilter.Creature.withSubtype("Rogue"),
                        atLeast = 1
                    )
                )
            )
        }
        description = "Whenever this creature attacks, if defending player has more cards in hand " +
            "than you, exile the top card of their library. During any turn you attacked with a " +
            "Rogue, you may cast that card and you may spend mana as though it were mana of any " +
            "color to cast that spell."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "138"
        artist = "Paul Scott Canavan"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0ecbe097-ba51-42e5-957c-382eb66c08f0.jpg?1783932619"
        ruling("2019-10-04", "If the defending player doesn't have more cards in hand than you immediately after Robber of the Rich attacks, its ability doesn't trigger at all. If that player doesn't have more cards in hand as the ability resolves, the ability does nothing.")
        ruling("2019-10-04", "If Robber of the Rich leaves the battlefield, the effect allowing you to cast the exiled card if you attacked with a Rogue continues to apply, even during later turns—as long as you attack with a Rogue during those later turns.")
        ruling("2019-10-04", "An effect that instructs you to \"cast\" a card doesn't allow you to play lands.")
        ruling("2019-10-04", "Casting an exiled card causes it to leave exile. You can't cast it multiple times.")
    }
}
