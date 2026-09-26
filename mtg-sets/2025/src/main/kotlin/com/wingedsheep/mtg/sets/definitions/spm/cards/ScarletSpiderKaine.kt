package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mayhem
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Scarlet Spider, Kaine — Marvel's Spider-Man #143
 * {B}{R} · Legendary Creature — Spider Human Hero · 2/1
 *
 * Menace
 * When Scarlet Spider enters, you may discard a card. If you do, put a +1/+1 counter on him.
 * Mayhem {B/R}
 */
val ScarletSpiderKaine = card("Scarlet Spider, Kaine") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Spider Human Hero"
    power = 2
    toughness = 1
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\nWhen Scarlet Spider enters, you may discard a card. If you do, put a +1/+1 counter on him.\nMayhem {B/R} (You may cast this card from your graveyard for {B/R} if you discarded it this turn. Timing rules still apply.)"

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            effect = Effects.IfYouDo(
                action = Patterns.Hand.discardCards(1),
                then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
            )
        )
        description = "When Scarlet Spider enters, you may discard a card. If you do, put a " +
            "+1/+1 counter on him."
    }

    mayhem("{B/R}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "Forrest Imel"
        flavorText = "Those who cross Kaine are forever marked."
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2cb00060-8cc5-42dc-bcbf-affd9e59f8fd.jpg?1783905312"
    }
}
