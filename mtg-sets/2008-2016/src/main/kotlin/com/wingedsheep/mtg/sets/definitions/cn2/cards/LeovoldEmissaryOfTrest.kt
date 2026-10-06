package com.wingedsheep.mtg.sets.definitions.cn2.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RestrictDrawsPerTurn
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Leovold, Emissary of Trest
 * {B}{G}{U}
 * Legendary Creature — Elf Advisor
 * 3/3
 *
 * Each opponent can't draw more than one card each turn.
 * Whenever you or a permanent you control becomes the target of a spell or ability an opponent
 * controls, you may draw a card.
 *
 * The first line is Narset, Parter of Veils' [RestrictDrawsPerTurn] — a "can't" (CR 614.17), so a
 * draw it forbids can't be replaced by dredge or Laboratory Maniac (CR 614.17c, the fourth ruling).
 *
 * The trigger keeps the object half and the player half of "you or a permanent you control" apart:
 * the filter is "a permanent you control" and `targetPlayer = You` is "you", both behind
 * `byOpponent`. It fires on target announcement (CR 601.2c / 602.2b), once per object or player that
 * becomes a target, so you and a permanent you control targeted by the same spell draw two (the
 * last ruling). The draw is optional.
 */
val LeovoldEmissaryOfTrest = card("Leovold, Emissary of Trest") {
    manaCost = "{B}{G}{U}"
    colorIdentity = "BGU"
    typeLine = "Legendary Creature — Elf Advisor"
    power = 3
    toughness = 3
    oracleText = "Each opponent can't draw more than one card each turn.\n" +
        "Whenever you or a permanent you control becomes the target of a spell or ability an " +
        "opponent controls, you may draw a card."

    staticAbility {
        ability = RestrictDrawsPerTurn(maxPerTurn = 1, affected = Player.EachOpponent)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.youControl()).becomesTarget(
            byOpponent = true,
            includePlayerTargets = true,
            targetPlayer = Player.You,
        )
        effect = Effects.May(Effects.DrawCards(1))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "77"
        artist = "Magali Villeneuve"
        flavorText = "\"I'm sure we can come to an arrangement.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49bb0ad3-1082-41f1-82a4-52a4006cc9b6.jpg?1783937339"

        ruling("2018-12-07", "Your opponents can draw a maximum of one card each on each player's turn. Subsequent card draws during that turn are ignored.")
        ruling("2018-12-07", "If an opponent hasn't drawn any cards in a turn and a spell or ability instructs that player to draw multiple cards, that player will just draw one card. However, if the draws are optional, the player can't choose to draw, even if they could draw one card this way.")
        ruling("2018-12-07", "Leovold will \"see\" cards drawn by opponents earlier in the turn before it entered the battlefield, although Leovold can't affect cards drawn before it entered the battlefield. For example, if an opponent draws two cards, then Leovold enters the battlefield, that opponent can't draw more cards that turn, but the two drawn cards are unaffected.")
        ruling("2018-12-07", "Replacement effects (such as that of dredge or Laboratory Maniac's ability) can't be used to replace draws that Leovold disallows. However, if an opponent's first draw is replaced (by a dredge ability, for example), that draw didn't happen and Leovold won't stop the next draw.")
        ruling("2018-12-07", "If you and a permanent you control each become the target of the same spell or ability an opponent controls, Leovold's ability will trigger twice. The same is true if two permanents you control become the target of the same spell or ability an opponent controls.")
    }
}
