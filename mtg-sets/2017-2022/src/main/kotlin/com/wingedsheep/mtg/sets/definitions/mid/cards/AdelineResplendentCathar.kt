package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Adeline, Resplendent Cathar
 * {1}{W}{W}
 * Legendary Creature — Human Knight
 * * /4
 * Vigilance
 * Adeline's power is equal to the number of creatures you control.
 * Whenever you attack, for each opponent, create a 1/1 white Human creature token that's tapped
 * and attacking that player or a planeswalker they control.
 *
 * - The printed `*` power is a characteristic-defining ability (CR 604.3) that works in every zone:
 *   `dynamicPower(creaturesYouControl())`, counting Adeline herself while she's a creature you
 *   control (first ruling).
 * - "Whenever you attack" (`Triggers.you.attacks()`) fires whenever you attack with any creatures,
 *   Adeline among them or not (second ruling).
 * - The tokens are `CreateToken(attackingEach = Player.EachOpponent)`: one per opponent — attacked
 *   or not (fifth ruling) — each attacking that opponent or, if you choose as it enters, a
 *   planeswalker that opponent controls (third ruling, CR 508.4). They were never declared as
 *   attackers, so "whenever a creature attacks" doesn't see them (fourth ruling, CR 508.3a).
 */
val AdelineResplendentCathar = card("Adeline, Resplendent Cathar") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Knight"
    toughness = 4
    dynamicPower(DynamicAmounts.creaturesYouControl())
    oracleText = "Vigilance\n" +
        "Adeline's power is equal to the number of creatures you control.\n" +
        "Whenever you attack, for each opponent, create a 1/1 white Human creature token that's tapped " +
        "and attacking that player or a planeswalker they control."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.you.attacks()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human"),
            tapped = true,
            attacking = true,
            attackingEach = Player.EachOpponent,
            imageUri = "https://cards.scryfall.io/normal/front/b/7/b7667345-e11b-4cad-ac4c-84eb1c5656c5.jpg?1783925225",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "1"
        artist = "Bryan Sola"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18092f68-b96e-4084-9eba-b240d2195d81.jpg?1783925668"
        ruling("2021-09-24", "The ability that defines Adeline's power works in all zones, not just the battlefield. As long as Adeline is on the battlefield (and still a creature), that ability will count Adeline itself.")
        ruling("2021-09-24", "Attacking with any creatures will cause Adeline's last ability to trigger. Adeline doesn't have to be among them.")
        ruling("2021-09-24", "You choose whether each token is attacking that opponent or a planeswalker they control as those tokens enter the battlefield.")
        ruling("2021-09-24", "Although the Human tokens created by the triggered ability are attacking, they were never declared as attacking creatures (for the purposes of abilities that trigger whenever a creature attacks, for example).")
        ruling("2021-09-24", "Tokens will be created for each of your opponents, not just opponents that you attacked.")
    }
}
