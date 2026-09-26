package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetOther
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Graceful Takedown
 * {1}{G}
 * Sorcery
 *
 * Any number of target enchanted creatures you control and up to one other target creature you
 * control each deal damage equal to their power to target creature you don't control.
 *
 * **Three target requirements, and the *dealers* are a subset of them** — so the damage loop can't
 * be `IterationSpace.Targets` (that iterates *every* target, which would make the victim deal damage
 * to itself). The pipeline expresses "iterate one slice of the targets" out of steps that already
 * exist: gather [CardSource.ChosenTargets], `filter` it down to the creatures you control, and run
 * the damage body once per member with `EffectTarget.IterationEntity` bound to it. Each dealer is
 * its own damage source (`damageSource = IterationEntity`), so lifelink/deathtouch and "dealt damage by a creature" reactions
 * see the creature, not the sorcery.
 *
 * **Requirement order is load-bearing.** The victim is declared *first* even though it is printed
 * last: target↔requirement alignment is positional (`TargetValidator`), so an unbounded requirement
 * has to come last or every requirement after it loses its slice. Declaring the victim first also
 * pins it to `ContextTarget(0)`/its own bound name regardless of how many creatures the unbounded
 * slot swallowed. `TargetOther` on the two "you control" requirements keeps the same creature from
 * being chosen twice and so dealing damage twice.
 *
 * Partial legality follows from where each half reads its targets (CR 608.2b, and the printed
 * rulings): the gather sees only the still-legal targets, so dealers that became illegal drop out and
 * the rest still deal damage; the victim is referenced by name, so if *it* is illegal the reference
 * resolves to nothing and no creature deals or is dealt damage.
 */
val GracefulTakedown = card("Graceful Takedown") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Any number of target enchanted creatures you control and up to one other target " +
        "creature you control each deal damage equal to their power to target creature you don't " +
        "control."

    spell {
        val victim = target(TargetFilter.CreatureOpponentControls)
        target(TargetOther(TargetObject(filter = TargetFilter.CreatureYouControl, optional = true)))
        target(TargetOther(
                TargetObject(filter = TargetFilter.CreatureYouControl.enchanted(), unlimited = true)
            ))

        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            val dealers = filter(chosen, GameObjectFilter.Creature.youControl())
            run(
                Effects.ForEachInCollection(
                    collection = dealers,
                    effect = Effects.DealDamage(
                        amount = DynamicAmounts.powerOf(EffectTarget.IterationEntity),
                        target = victim,
                        damageSource = EffectTarget.IterationEntity
                    )
                )
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "171"
        artist = "Sidharth Chaturvedi"
        flavorText = "The fauns of the Tanglespan are adept at using the precarious environment " +
            "against larger opponents."
        imageUri = "https://cards.scryfall.io/normal/front/8/3/83edf626-ed34-417f-818d-597ecf439167.jpg?1783915083"
        ruling(
            "2023-09-01",
            "If one of the target creatures you control is an illegal target as Graceful Takedown " +
                "resolves (perhaps because it's no longer enchanted or is no longer on the " +
                "battlefield), the remaining legal target creatures you control will still deal " +
                "damage equal to their power."
        )
        ruling(
            "2023-09-01",
            "If the last target creature is an illegal target as Graceful Takedown resolves, or if " +
                "all of the target creatures you control are illegal targets, no creature deals or " +
                "is dealt damage."
        )
    }
}
