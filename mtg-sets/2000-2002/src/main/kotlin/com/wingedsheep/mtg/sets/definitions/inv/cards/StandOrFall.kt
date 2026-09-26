package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step

/**
 * Stand or Fall
 * {3}{R}
 * Enchantment
 * At the beginning of combat on your turn, for each defending player, separate
 * all creatures that player controls into two piles and that player chooses one.
 * Only creatures in the chosen piles can block this turn.
 *
 * A per-defender "divvy" (CR 700.3) restriction: for each opponent you partition
 * their creatures (as the source's controller), they choose which pile can block,
 * and the other pile can't block this turn. Composed inside [ForEachPlayerEffect]
 * over the defending players — within the loop the iterated player is the controller,
 * so `Player.You`/`Chooser.Controller` is that defender while `Chooser.SourceController`
 * stays the enchantment's controller (who separates). The non-chosen pile gets a
 * snapshot can't-block restriction via ForEachInCollection + single-target
 * [Effects.CantBlock].
 */
val StandOrFall = card("Stand or Fall") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "At the beginning of combat on your turn, for each defending player, separate all creatures that player controls into two piles and that player chooses one. Only creatures in the chosen piles can block this turn."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.ForEachPlayer(
            players = Player.EachOpponent,
            Effects.Pipeline {
                // 1. Gather the creatures this defending player controls.
                val creatures = gather(
                    CardSource.ControlledPermanents(
                        player = Player.You,
                        filter = GameObjectFilter.Creature
                    )
                )
                // 2. You (the enchantment's controller) separate that player's creatures into two piles.
                val (pileA, pileB) = chooseAnyNumberSplit(
                    from = creatures,
                    chooser = Chooser.SourceController,
                    selectedLabel = "Pile 1",
                    remainderLabel = "Pile 2",
                    prompt = "Separate this player's creatures into two piles. The creatures you select form Pile 1; the rest form Pile 2.",
                    useTargetingUI = true,
                    alwaysPrompt = true
                )
                // 3. That player chooses which pile may block.
                val (_, cantBlock) = choosePile(
                    pileA,
                    pileB,
                    pileALabel = "Pile 1",
                    pileBLabel = "Pile 2",
                    chooser = Chooser.Controller,
                    prompt = "Choose which pile of your creatures can block this turn."
                )
                // 4. Only the chosen pile can block — the other pile can't block this turn.
                run(Effects.ForEachInCollection(
                    collection = cantBlock,
                    effect = Effects.CantBlock(EffectTarget.IterationEntity)
                ))
            }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "171"
        artist = "Matt Cavotta"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/60c34970-a106-490c-ac37-6156eb7f34ce.jpg?1562914611"
    }
}
