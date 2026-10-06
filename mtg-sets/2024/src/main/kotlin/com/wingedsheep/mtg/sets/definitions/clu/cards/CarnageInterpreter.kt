package com.wingedsheep.mtg.sets.definitions.clu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Carnage Interpreter — Ravnica: Clue Edition #26
 * {1}{B/R}{B/R} · Creature — Devil Detective · 3/3 · Rare
 *
 * When this creature enters, discard your hand, then investigate four times.
 * As long as you have one or fewer cards in hand, this creature gets +2/+2 and has menace.
 *
 * The enters trigger is "discard, *then* investigate" with no dependency between the halves: an
 * empty hand still makes four Clues. The static is two condition-gated abilities over the source
 * (the Djeru and Hazoret shape), both re-checked on every projection, so drawing back up to two
 * cards turns off the bonus and menace together. Per the ruling, losing cards after blockers are
 * declared doesn't undo a block — menace is only checked when blocks are declared.
 */
val CarnageInterpreter = card("Carnage Interpreter") {
    manaCost = "{1}{B/R}{B/R}"
    colorIdentity = "BR"
    typeLine = "Creature — Devil Detective"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, discard your hand, then investigate four times. " +
        "(To investigate, create a Clue token. It's an artifact with \"{2}, Sacrifice this token: " +
        "Draw a card.\")\n" +
        "As long as you have one or fewer cards in hand, this creature gets +2/+2 and has menace."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Hand.discardHand() then Effects.Investigate(4)
        description = "When this creature enters, discard your hand, then investigate four times."
    }

    staticAbility {
        condition = Conditions.CardsInHandAtMost(1)
        ability = ModifyStats(powerBonus = 2, toughnessBonus = 2, filter = GroupFilter.source())
    }
    staticAbility {
        condition = Conditions.CardsInHandAtMost(1)
        ability = GrantKeyword(Keyword.MENACE, GroupFilter.source())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "26"
        artist = "Justine Cruz"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6fb576e-a4a4-496b-b553-3f81cc651210.jpg?1783912576"

        ruling(
            "2024-02-02",
            "Once Carnage Interpreter has been blocked, causing it to gain menace by removing cards " +
                "from your hand won't cause it to stop being blocked."
        )
    }
}
