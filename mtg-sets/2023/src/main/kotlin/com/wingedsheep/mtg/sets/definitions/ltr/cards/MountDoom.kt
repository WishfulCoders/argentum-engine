package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Mount Doom
 * Legendary Land
 *
 * {T}, Pay 1 life: Add {B} or {R}.
 * {1}{B}{R}, {T}: Mount Doom deals 1 damage to each opponent.
 * {5}{B}{R}, {T}, Sacrifice Mount Doom and a legendary artifact: Choose up to two creatures, then
 * destroy the rest. Activate only as a sorcery.
 *
 * Composable: the "choose up to two, destroy the rest" wrath uses the Duneblast pipeline
 * (Gather → Select ChooseUpTo(2) with storeRemainder → Move Destroy).
 */
val MountDoom = card("Mount Doom") {
    typeLine = "Legendary Land"
    colorIdentity = "BR"
    oracleText = "{T}, Pay 1 life: Add {B} or {R}.\n" +
        "{1}{B}{R}, {T}: Mount Doom deals 1 damage to each opponent.\n" +
        "{5}{B}{R}, {T}, Sacrifice Mount Doom and a legendary artifact: Choose up to two creatures, " +
        "then destroy the rest. Activate only as a sorcery."

    // {T}, Pay 1 life: Add {B} or {R}.
    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(1))
        effect = Effects.AddManaOfChoice(ManaColorSet.Specific(setOf(Color.BLACK, Color.RED)))
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    // {1}{B}{R}, {T}: deal 1 damage to each opponent.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}{R}"), Costs.Tap)
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    // {5}{B}{R}, {T}, Sacrifice Mount Doom and a legendary artifact: choose up to two creatures,
    // destroy the rest.
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{5}{B}{R}"),
            Costs.Tap,
            Costs.SacrificeSelf,
            Costs.Sacrifice(GameObjectFilter.Artifact.legendary())
        )
        effect = Effects.Pipeline {
            val allCreatures = gather(CardSource.BattlefieldMatching(filter = GameObjectFilter.Creature))
            val (_, toDestroy) = chooseUpToSplit(
                2,
                from = allCreatures,
                prompt = "Choose up to two creatures to save",
                selectedLabel = "Save",
                remainderLabel = "Destroy",
                useTargetingUI = true
            )
            destroy(toDestroy)
        }
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "258"
        artist = "Jonas De Ro"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5bc71a1-2344-4bc6-aa60-658cec19d0d6.jpg?1686970382"
    }
}
