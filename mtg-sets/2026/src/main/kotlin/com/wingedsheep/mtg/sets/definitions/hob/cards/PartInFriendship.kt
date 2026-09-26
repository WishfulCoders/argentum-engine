package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Part in Friendship — The Hobbit #134
 * {4}{G} · Enchantment · Rare
 *
 * Whenever a nontoken creature you control dies, reveal cards from the top of your library until
 * you reveal a creature card. If its mana value is less than or equal to the number of lands you
 * control, put it onto the battlefield. Otherwise, put it into your hand. Put the rest on the
 * bottom of your library in a random order. This ability triggers only once each turn.
 *
 * Modeling notes:
 *  - The reveal is the Spinner of Souls pipeline with a branch grafted in: gather until the first
 *    creature card, split the revealed pile into that single match and everything under it, then
 *    branch the match on mana value while the remainder always goes to the bottom in a random
 *    order. Splitting first is what keeps the match from being swept to the bottom along with the
 *    rest — [GatherUntilMatchEffect]'s revealed pile includes the card it stopped on.
 *  - The branch is a [Compare] against the *live* land count, read at resolution, so lands that
 *    entered or left since the trigger went on the stack are counted correctly.
 *  - An empty library (no creature revealed) leaves the match collection empty: both branches
 *    move nothing and every revealed card goes to the bottom, which is the printed behaviour.
 *  - "This ability triggers only once each turn" is `oncePerTurn`: the first nontoken creature to
 *    die spends the turn's use whether or not a creature card was found.
 */
val PartInFriendship = card("Part in Friendship") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Whenever a nontoken creature you control dies, reveal cards from the top of " +
        "your library until you reveal a creature card. If its mana value is less than or equal " +
        "to the number of lands you control, put it onto the battlefield. Otherwise, put it into " +
        "your hand. Put the rest on the bottom of your library in a random order. This ability " +
        "triggers only once each turn."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().nontoken()).dies()
        oncePerTurn = true
        effect = Effects.Pipeline {
            val (_, revealed) = gatherUntilMatch(GameObjectFilter.Creature)
            reveal(revealed)
            val (found, rest) = filterSplit(revealed, GameObjectFilter.Creature)
            run(Effects.If(
                condition = Conditions.CompareAmounts(
                    left = DynamicAmounts.manaValueOf(found),
                    operator = ComparisonOperator.LTE,
                    right = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Land).count()
                ),
                then = Effects.Pipeline { move(found, CardDestination.ToZone(Zone.BATTLEFIELD)) },
                otherwise = Effects.Pipeline { toHand(found, revealed = true) }
            ))
            toLibraryBottom(rest, order = CardOrder.Random)
        }
        description = "Whenever a nontoken creature you control dies, reveal cards from the top " +
            "of your library until you reveal a creature card. If its mana value is less than or " +
            "equal to the number of lands you control, put it onto the battlefield. Otherwise, " +
            "put it into your hand. Put the rest on the bottom of your library in a random order."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "134"
        artist = "Jarel Threat"
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b4ff1eac-6d97-40ab-9b7c-c2fdca0917d9.jpg?1784632164"
    }
}
