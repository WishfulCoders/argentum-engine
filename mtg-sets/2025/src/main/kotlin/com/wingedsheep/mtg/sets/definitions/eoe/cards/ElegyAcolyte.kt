package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step

/**
 * Elegy Acolyte
 * {2}{B}{B}
 * Creature — Human Cleric
 * 4/4
 * Lifelink
 * Whenever one or more creatures you control deal combat damage to a player, you draw a card
 * and lose 1 life.
 * Void — At the beginning of your end step, if a nonland permanent left the battlefield this
 * turn or a spell was warped this turn, create a 2/2 colorless Robot artifact creature token.
 */
val ElegyAcolyte = card("Elegy Acolyte") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Cleric"
    power = 4
    toughness = 4
    oracleText = "Lifelink\n" +
        "Whenever one or more creatures you control deal combat damage to a player, you draw a card and lose 1 life.\n" +
        "Void — At the beginning of your end step, if a nonland permanent left the battlefield this turn or a spell was warped this turn, create a 2/2 colorless Robot artifact creature token."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature.youControl()).dealCombatDamageToAPlayer()
        effect = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller)
        description = "Whenever one or more creatures you control deal combat damage to a player, you draw a card and lose 1 life."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.Void
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(),
            creatureTypes = setOf("Robot"),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/c/4/c46f9a07-005c-44b7-8057-b2f00b274dd6.jpg?1756281130"
        )
        description = "At the beginning of your end step, if a nonland permanent left the battlefield this turn or a spell was warped this turn, create a 2/2 colorless Robot artifact creature token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "97"
        artist = "Diana Franco"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c69ed7c7-1f49-4299-b3e6-75150258ac59.jpg?1752946946"
    }
}
