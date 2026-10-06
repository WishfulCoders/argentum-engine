package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Dress Down
 * {1}{U}
 * Enchantment
 * Flash
 * When this enchantment enters, draw a card.
 * Creatures lose all abilities.
 * At the beginning of the end step, sacrifice this enchantment.
 *
 * "Creatures lose all abilities" is a Layer 6 continuous effect over every creature, applied in
 * timestamp order: an ability granted to a creature after Dress Down entered survives, while
 * everything a creature already had is removed (2021-06-18 ruling). The end-step sacrifice fires
 * at the beginning of every end step, not only its controller's.
 */
val DressDown = card("Dress Down") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Flash\n" +
        "When this enchantment enters, draw a card.\n" +
        "Creatures lose all abilities.\n" +
        "At the beginning of the end step, sacrifice this enchantment."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    staticAbility {
        ability = LoseAllAbilities(filter = GroupFilter.AllCreatures)
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        effect = SacrificeSelfEffect
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "39"
        artist = "Iain McCaig"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04f9f061-67b8-4427-9fcb-b3ccfee8fc5d.jpg?1783926882"
        ruling(
            "2021-06-18",
            "If an effect grants a creature an ability after Dress Down has entered the battlefield, it " +
                "won't lose that ability. For example, if a land becomes a creature while Dress Down is " +
                "on the battlefield, it will still gain any abilities given to it by the effect that " +
                "animated it. It will, however, lose any abilities it already had."
        )
        ruling(
            "2021-06-18",
            "If an effect causes Dress Down to become a creature, it will lose its own abilities along " +
                "with all other creatures. This includes the triggered ability that causes it to be " +
                "sacrificed at the beginning of the end step."
        )
    }
}
