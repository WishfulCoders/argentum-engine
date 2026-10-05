package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.TargetChooser
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Diaochan, Artful Beauty
 * {3}{R}
 * Legendary Creature — Human Advisor
 * 1/1
 * {T}: Destroy target creature of your choice, then destroy target creature of an opponent's choice.
 * Activate only during your turn, before attackers are declared.
 *
 * The second target is a real target picked by an opponent ([TargetChooser.Opponent]).
 */
val DiaochanArtfulBeauty = card("Diaochan, Artful Beauty") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Advisor"
    power = 1
    toughness = 1
    oracleText = "{T}: Destroy target creature of your choice, then destroy target creature of an opponent's choice. Activate only during your turn, before attackers are declared."

    activatedAbility {
        cost = Costs.Tap
        restrictions = listOf(
            ActivationRestriction.OnlyDuringYourTurn,
            ActivationRestriction.BeforeStep(Step.DECLARE_ATTACKERS)
        )
        val mine = target(TargetFilter.Creature)
        val theirs = target(TargetFilter.Creature, chooser = TargetChooser.Opponent)
        effect = Effects.Destroy(mine) then Effects.Destroy(theirs)
        description = "{T}: Destroy target creature of your choice, then destroy target creature of an opponent's choice. Activate only during your turn, before attackers are declared."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "108"
        artist = "Miao Aili"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/6180c476-dadf-4c03-ab1e-639386bd4319.jpg?1783946108"
    }
}
