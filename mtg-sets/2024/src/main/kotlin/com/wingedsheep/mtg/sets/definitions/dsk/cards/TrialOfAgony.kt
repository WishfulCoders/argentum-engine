package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.model.Rarity

/**
 * Trial of Agony
 * {R}
 * Sorcery
 * Choose two target creatures controlled by the same opponent. That player chooses one of
 * those creatures. Trial of Agony deals 5 damage to that creature, and the other can't block
 * this turn.
 *
 * Same Gather/Select/split shape as Barrin's Spite, with two differences: the two targets must
 * be controlled by the *same opponent* (TargetCreature with `sameController` + the
 * opponent-controls filter), and the split effects are "deal 5 damage to the chosen one" plus
 * "the other can't block this turn" instead of sacrifice/bounce.
 *
 *   Gather(ChosenTargets) → SelectFromCollection (the targets' controller — the opponent — picks
 *   one, the remainder is "the other") → DealDamage 5 to the chosen → CantBlock the other.
 *
 * Fizzle handling falls out of the pipeline for free, as in Barrin's Spite: the stack resolver
 * strips illegal targets before the effect runs, so if only one of the two targets survives,
 * the gathered collection holds just that creature. ChooseExactly(1) auto-picks it for the 5
 * damage and the (empty) remainder applies can't-block to nothing.
 */
val TrialOfAgony = card("Trial of Agony") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Choose two target creatures controlled by the same opponent. That player " +
        "chooses one of those creatures. Trial of Agony deals 5 damage to that creature, and " +
        "the other can't block this turn."

    spell {
        targets(TargetFilter.CreatureOpponentControls, count = 2, sameController = true)
        effect = Effects.Pipeline {
            // 1. Reference the two targeted creatures.
            val trialCreatures = gather(CardSource.ChosenTargets)
            // 2. Their controller (the opponent) chooses one; the other is the remainder.
            val (trialChosen, trialOther) = chooseExactlySplit(
                1,
                from = trialCreatures,
                chooser = Chooser.ControllerOfSelection,
                useTargetingUI = true,
                prompt = "Choose one of the two creatures to take 5 damage"
            )
            // 3. Trial of Agony deals 5 damage to the chosen creature.
            run(Effects.DealDamage(5, trialChosen.asTarget))
            // 4. The other can't block this turn.
            run(Effects.ForEachInCollection(
                collection = trialOther,
                effect = Effects.CantBlock(EffectTarget.IterationEntity)
            ))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "159"
        artist = "Mike Sass"
        flavorText = "All it took to shatter their lifelong bond was a scrap of hope."
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa62f67a-d20f-4d99-b0a2-327634299c9f.jpg?1726286448"
    }
}
