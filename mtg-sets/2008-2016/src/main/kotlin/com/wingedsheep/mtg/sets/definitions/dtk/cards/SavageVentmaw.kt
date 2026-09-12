package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ManaExpiry

/**
 * Savage Ventmaw
 * {4}{R}{G}
 * Creature — Dragon
 * 4/4
 *
 * Flying
 * Whenever this creature attacks, add {R}{R}{R}{G}{G}{G}. Until end of turn, you don't
 * lose this mana as steps and phases end.
 *
 * Pools empty as each step and phase ends, so the "until end of turn, you don't lose this mana"
 * clause is [ManaExpiry.UNTIL_END_OF_TURN] on each colour's mana.
 */
val SavageVentmaw = card("Savage Ventmaw") {
    manaCost = "{4}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever this creature attacks, add {R}{R}{R}{G}{G}{G}. Until end of turn, you " +
        "don't lose this mana as steps and phases end."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.Composite(
            Effects.AddMana(Color.RED, 3, expiry = ManaExpiry.UNTIL_END_OF_TURN),
            Effects.AddMana(Color.GREEN, 3, expiry = ManaExpiry.UNTIL_END_OF_TURN),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "231"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/690008d1-d1fe-49ad-810c-84be57cecc6c.jpg?1782712690"
    }
}
