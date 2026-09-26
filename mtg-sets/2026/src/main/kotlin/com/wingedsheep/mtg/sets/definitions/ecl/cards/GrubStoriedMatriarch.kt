package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step

/**
 * Grub, Storied Matriarch // Grub, Notorious Auntie
 * {2}{B}
 * Legendary Creature — Goblin Warlock // Legendary Creature — Goblin Warrior
 * 2/1
 */
private val GrubNotoriousAuntie = card("Grub, Notorious Auntie") {
    manaCost = ""
    typeLine = "Legendary Creature — Goblin Warrior"
    power = 2
    toughness = 1
    oracleText = "Menace\n" +
        "Whenever Grub attacks, you may blight 1. If you do, create a tapped and attacking token " +
        "that's a copy of the blighted creature, except it has \"At the beginning of the end step, " +
        "sacrifice this token.\"\n" +
        "At the beginning of your first main phase, you may pay {B}. If you do, transform Grub."

    keywords(Keyword.MENACE)

    val sacrificeAtEndStep = TriggeredAbility.create(
        trigger = Triggers.anyPlayer.beginningOf(Step.END),
        effect = Effects.SacrificeTarget(EffectTarget.Self)
    )

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.May(
            effect = Effects.Pipeline {
                val blightTargets = gather(CardSource.ControlledPermanents(Player.You, GameObjectFilter.Creature))
                val blighted = chooseUpTo(
                    1,
                    from = blightTargets,
                    chooser = Chooser.Controller,
                    prompt = "Blight 1 — choose a creature you control (or cancel)",
                    useTargetingUI = true,
                    alwaysPrompt = true
                )
                run(Effects.AddCountersToCollection(blighted, CounterType.MINUS_ONE_MINUS_ONE, 1))
                ifNotEmpty(blighted) {
                    run(Effects.CreateTokenCopyOfTarget(
                        target = blighted.asTarget,
                        tapped = true,
                        attacking = true,
                        triggeredAbilities = listOf(sacrificeAtEndStep)
                    ))
                }
            },
            descriptionOverride = "You may blight 1. If you do, create a tapped and attacking token that's a copy of the blighted creature."
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.PRECOMBAT_MAIN)
        effect = Effects.MayPay(
            cost = ManaCost.parse("{B}"),
            then = Effects.Transform(EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "105"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/back/1/f/1f51adf8-8234-4dae-aedf-7633310d5111.jpg?1767734188"
    }
}

private val GrubStoriedMatriarchFrontFace = card("Grub, Storied Matriarch") {
    manaCost = "{2}{B}"
    typeLine = "Legendary Creature — Goblin Warlock"
    power = 2
    toughness = 1
    oracleText = "Menace\n" +
        "Whenever this creature enters or transforms into Grub, Storied Matriarch, return up to one target " +
        "Goblin card from your graveyard to your hand.\n" +
        "At the beginning of your first main phase, you may pay {R}. If you do, transform Grub."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val goblin = target(TargetFilter.CardInGraveyard.ownedByYou().withSubtype(Subtype.GOBLIN), optional = true)
        effect = Effects.ReturnToHand(goblin)
    }

    triggeredAbility {
        trigger = Triggers.self.transforms(false)
        val goblin = target(TargetFilter.CardInGraveyard.ownedByYou().withSubtype(Subtype.GOBLIN), optional = true)
        effect = Effects.ReturnToHand(goblin)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.PRECOMBAT_MAIN)
        effect = Effects.MayPay(
            cost = ManaCost.parse("{R}"),
            then = Effects.Transform(EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "105"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f51adf8-8234-4dae-aedf-7633310d5111.jpg?1767734188"
    }
}

val GrubStoriedMatriarch: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = GrubStoriedMatriarchFrontFace,
    backFace = GrubNotoriousAuntie
)
