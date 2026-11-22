package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * White Glove Gourmand — Fallout #124
 * {2}{W}{B} · Creature — Human Noble · 2/2
 *
 * When this creature enters, create two 1/1 white Human Soldier creature tokens.
 * At the beginning of your end step, if another Human died under your control this turn, create
 * a Food token.
 *
 * The intervening-if reads the per-turn death history scoped to you, with each death recorded by
 * its last-known subtypes — so one dead Human satisfies both the subtype and the "under your
 * control" halves, and the Gourmand needn't have been around when it died. "Another" adds nothing
 * the history can't already see: the Gourmand can only trigger while it is on the battlefield,
 * and an earlier Gourmand object that died is a different object (CR 400.7).
 */
val WhiteGloveGourmand = card("White Glove Gourmand") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Human Noble"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, create two 1/1 white Human Soldier creature tokens.\n" +
        "At the beginning of your end step, if another Human died under your control this turn, create a Food token."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human", "Soldier"),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/7/3/737741e7-b2b2-4c41-ab7c-4eeaf9f37b71.jpg?1783912046",
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.SubtypeCreatureDiedThisTurn(Subtype.HUMAN, player = Player.You)
        effect = Effects.CreateFood()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "124"
        artist = "Wonchun Choi"
        flavorText = "\"Here's to us! It's time we move beyond beef!\""
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c5bf1f6c-4653-4734-9bd7-af689a1b679d.jpg?1783912382"
        ruling("2024-03-08", "White Glove Gourmand doesn't need to have been on the battlefield when the Human died. For example, if a Human dies during combat on your turn and you cast White Glove Gourmand during your second main phase, its last ability will trigger at the beginning of your end step.")
        ruling("2024-03-08", "White Glove Gourmand's last ability will trigger only once during your end step, no matter how many Humans died under your control this turn. However, if no Humans have died under your control so far this turn as your end step begins, the ability won't trigger at all. It's not possible to cause a Human to die under your control during the end step in time to have the ability trigger.")
    }
}
