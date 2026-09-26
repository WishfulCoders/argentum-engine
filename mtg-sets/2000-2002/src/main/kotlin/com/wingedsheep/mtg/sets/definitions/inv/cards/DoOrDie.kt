package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets

/**
 * Do or Die
 * {1}{B}
 * Sorcery
 * Separate all creatures target player controls into two piles. Destroy all
 * creatures in the pile of that player's choice. They can't be regenerated.
 *
 * A "divvy" (CR 700.3) variant on the battlefield: the spell's controller does
 * the partitioning, then the *target* player chooses which pile dies. Composed
 * from the same pile primitives as Fact or Fiction
 * ([com.wingedsheep.mtg.sets.definitions.inv.cards.FactOrFiction]):
 * Gather → SelectFromCollection (split) → ChoosePile → MoveCollection (Destroy).
 */
val DoOrDie = card("Do or Die") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Separate all creatures target player controls into two piles. Destroy all creatures in the pile of that player's choice. They can't be regenerated."

    spell {
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            // 1. Gather every creature the target player controls (projected).
            val creatures = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Creature,
                    player = player.asPlayer
                )
            )
            // 2. You partition them into two piles by clicking creatures in
            //    play. The creatures you select form Pile 1; the rest, Pile 2.
            val (pileA, pileB) = chooseAnyNumberSplit(
                from = creatures,
                chooser = Chooser.Controller,
                selectedLabel = "Pile 1",
                remainderLabel = "Pile 2",
                prompt = "Separate the target player's creatures into two piles. The creatures you select form Pile 1; the rest form Pile 2.",
                useTargetingUI = true,
                alwaysPrompt = true
            )
            // 3. The target player chooses which pile is destroyed.
            val (doomed, _) = choosePile(
                pileA,
                pileB,
                pileALabel = "Pile 1",
                pileBLabel = "Pile 2",
                chooser = Chooser.TargetPlayer,
                prompt = "Choose which pile of your creatures is destroyed."
            )
            // 4. Destroy the chosen pile; it can't be regenerated.
            destroy(doomed, noRegenerate = true)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "102"
        artist = "Christopher Moeller"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/05f63cd9-e82b-4cf8-b8ce-f0aa0157692b.jpg?1562896148"
    }
}
