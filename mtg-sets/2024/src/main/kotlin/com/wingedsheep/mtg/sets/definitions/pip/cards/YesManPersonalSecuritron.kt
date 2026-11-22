package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val YesManPersonalSecuritron = card("Yes Man, Personal Securitron") {
    manaCost = "{2}{W}"
    typeLine = "Legendary Artifact Creature — Robot"
    power = 2
    toughness = 2
    oracleText = "{T}: Target opponent gains control of Yes Man. When they do, you draw two cards and put a quest counter on Yes Man. Activate only during your turn.\nWild Card — When Yes Man leaves the battlefield, its owner creates a tapped 1/1 white Soldier creature token for each quest counter on it."

    activatedAbility {
        cost = Costs.Tap
        restrictions = listOf(ActivationRestriction.OnlyDuringYourTurn)
        val opponent = target(Targets.Opponent)
        effect = Effects.IfYouDo(
            action = Effects.GiveControl(EffectTarget.Self, opponent),
            successCriterion = SuccessCriterion.ControlChanged,
            then = Effects.ReflexiveTrigger(
                action = Effects.Nothing,
                optional = false,
                reflexiveEffect = Effects.DrawCards(2) then
                    Effects.AddCounters(CounterType.QUEST, 1, EffectTarget.Self),
                descriptionOverride = "Draw two cards and put a quest counter on Yes Man",
            ),
        )
        description = "Target opponent gains control of Yes Man. When they do, you draw two cards and put a quest counter on Yes Man."
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.CreateToken(
            count = DynamicAmounts.lastKnownSourceCounters(CounterType.QUEST),
            power = 1, toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
            tapped = true,
            controller = EffectTarget.PlayerRef(Player.OwnerOfSource),
            imageUri = "https://cards.scryfall.io/normal/front/a/f/af191656-5e62-440f-b63a-705cfed314e7.jpg?1783912045",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "29"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8e2fac8c-a574-4414-ac68-632fc822ddbb.jpg?1783912412"
        ruling("2024-03-08", "If Yes Man isn't on the battlefield as its first ability resolves, or if the target opponent is an illegal target as the ability tries to resolve, nothing will happen. You won't draw two cards, because that effect is part of a reflexive triggered ability that triggers only if the target opponent gains control of Yes Man.")
        ruling("2024-03-08", "When Yes Man's controller leaves the game, if that player wasn't its owner, the effect giving that player control of Yes Man ends. Yes Man will return to the control of the player still in the game who most recently controlled it, ignoring any control effects whose durations have already expired.")
        ruling("2024-03-08", "When Yes Man's owner leaves the game, Yes Man leaves the game along with that player. It's just doing what it's told.")
    }
}
