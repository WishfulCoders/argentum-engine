package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.model.Rarity

/**
 * Transcendent Archaic
 * {7}
 * Creature — Avatar
 * 6/6
 *
 * Vigilance
 * Converge — When this creature enters, you may draw X cards, where X is the number of colors of
 * mana spent to cast this spell. If you draw one or more cards this way, discard two cards.
 *
 * The {7} cost is all generic, so the colour count X is entirely a function of how the controller
 * pays — X = [DynamicAmounts.colorsOfManaSpent] (`DynamicAmount.DistinctColorsManaSpent`), read off
 * the entering creature's recorded payment (the dominant Converge shape, same as the Archaic cycle).
 *
 * Modelled as an `Triggers.self.enters()` trigger whose body is an optional [Effects.May] draw of X
 * cards, followed by a discard of two that is gated on "you drew one or more this way" — i.e. X >= 1.
 * The discard sits *inside* the `may`, so declining the draw never forces the discard; and a `yes`
 * with X = 0 (all-colourless payment) draws nothing and the `Effects.If` gate suppresses the
 * discard. (Drawing zero is not "drawing one or more.") The intervening gate is a synchronous
 * `Compare(DistinctColorsManaSpent >= 1)` rather than an `IfYouDo` action-outcome, because the draw
 * count is a known function of the cast and a draw isn't a zone-move shape `SuccessCriterion.Auto`
 * can infer.
 */
val TranscendentArchaic = card("Transcendent Archaic") {
    manaCost = "{7}"
    colorIdentity = ""
    typeLine = "Creature — Avatar"
    power = 6
    toughness = 6
    oracleText = "Vigilance\n" +
        "Converge — When this creature enters, you may draw X cards, where X is the number of " +
        "colors of mana spent to cast this spell. If you draw one or more cards this way, " +
        "discard two cards."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Effects.DrawCards(DynamicAmounts.colorsOfManaSpent()) then
                Effects.If(
                    condition = Conditions.CompareAmounts(
                        DynamicAmounts.colorsOfManaSpent(),
                        ComparisonOperator.GTE,
                        1,
                    ),
                    then = Effects.Discard(2),
                ),
        )
        description = "Converge — When this creature enters, you may draw X cards, where X is the " +
            "number of colors of mana spent to cast this spell. If you draw one or more cards this " +
            "way, discard two cards."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "5"
        artist = "Chris Rahn"
        flavorText = "Even as most archaics raged across Arcavios, others seemed to sink into contemplation."
        imageUri = "https://cards.scryfall.io/normal/front/1/6/1624c680-502b-474a-b9b2-888fe3ca008c.jpg?1775936948"
    }
}
