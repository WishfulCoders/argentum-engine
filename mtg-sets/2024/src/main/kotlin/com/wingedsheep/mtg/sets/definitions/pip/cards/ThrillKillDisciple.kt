package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mechanics.squad
import com.wingedsheep.sdk.model.Rarity

/**
 * Thrill-Kill Disciple — Fallout #68
 * {2}{R} · Creature — Human Mercenary · 3/2
 *
 * Squad—{1}, Discard a card.
 * When this creature dies, create a Junk token.
 *
 * The squad cost has a mana and a discard half; each payment charges both ({1} and one discard
 * per copy), the shape RepeatableAdditionalCostTest pins.
 */
val ThrillKillDisciple = card("Thrill-Kill Disciple") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Human Mercenary"
    power = 3
    toughness = 2
    oracleText = "Squad—{1}, Discard a card. (As an additional cost to cast this spell, you may pay its squad cost any number of times. When this creature enters, create that many tokens that are copies of it.)\nWhen this creature dies, create a Junk token."
    squad("{1}", Costs.additional.DiscardCards(1))
    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateJunk()
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "68"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/5/6/5628e781-9f12-49ed-a575-6b467d39510b.jpg?1783912400"
        ruling("2024-03-08", "You can’t sacrifice a Junk token to pay multiple costs. For example, you can’t sacrifice a Junk token to activate its own ability and also to activate the second ability of Junk Jet.")
        ruling("2024-03-08", "You may pay the squad cost any number of times. You will create a token that is a copy of that permanent for each time you paid the squad cost. If you paid the squad cost multiple times, the tokens will all enter the battlefield simultaneously.")
        ruling("2024-03-08", "If the spell is countered, the squad ability will not trigger, and no tokens will be created.")
        ruling("2024-03-08", "If the spell resolves but the creature with squad leaves the battlefield before its squad ability resolves, you’ll still create the token copies.")
        ruling("2024-03-08", "The tokens created by the squad ability aren’t “cast,” so any abilities that trigger when a spell is cast won’t trigger for the copies.")
        ruling("2024-03-08", "If, for some reason, the creature doesn’t have the squad ability when it’s on the battlefield, the ability won’t trigger, even if you’ve paid the squad cost one or more times.")
    }
}
