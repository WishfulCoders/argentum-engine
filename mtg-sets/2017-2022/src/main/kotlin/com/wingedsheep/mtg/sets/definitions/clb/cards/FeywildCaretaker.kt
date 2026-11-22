package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Feywild Caretaker
 * {4}{U}
 * Creature — Orc Wizard
 * 3/4
 * When this creature enters, you take the initiative.
 * At the beginning of your end step, if you have the initiative, create a 1/1 blue Faerie Dragon
 * creature token with flying.
 */
val FeywildCaretaker = card("Feywild Caretaker") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Orc Wizard"
    power = 3
    toughness = 4
    oracleText = "When this creature enters, you take the initiative.\n" +
        "At the beginning of your end step, if you have the initiative, create a 1/1 blue Faerie Dragon " +
        "creature token with flying."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.TakeTheInitiative()
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.YouHaveTheInitiative
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Faerie", "Dragon"),
            keywords = setOf(Keyword.FLYING)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "69"
        artist = "Borja Pindado"
        flavorText = "\"Hello, my fluttering friends! Isn't it a beautiful day?\""
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c959ab97-0727-4589-bfd9-d78dd8ebd86b.jpg?1783922789"
    }
}
