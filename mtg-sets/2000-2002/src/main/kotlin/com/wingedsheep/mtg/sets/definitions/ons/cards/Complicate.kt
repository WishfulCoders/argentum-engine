package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Complicate
 * {2}{U}
 * Instant
 * Counter target spell unless its controller pays {3}.
 * Cycling {2}{U}
 * When you cycle Complicate, you may counter target spell unless its controller pays {1}.
 */
val Complicate = card("Complicate") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell unless its controller pays {3}.\nCycling {2}{U}\nWhen you cycle Complicate, you may counter target spell unless its controller pays {1}."

    spell {
        val spell = target(TargetFilter.SpellOnStack)
        effect = Effects.CounterUnlessPays("{3}")
    }

    keywordAbility(KeywordAbility.cycling("{2}{U}"))

    triggeredAbility {
        val spell = target(TargetFilter.SpellOnStack)
        trigger = Triggers.self.isCycled()
        effect = Effects.May(Effects.CounterUnlessPays("{1}"))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "76"
        artist = "Scott M. Fischer"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/33f69670-e494-42b8-9148-fe105ec61aa0.jpg?1562907165"
    }
}
