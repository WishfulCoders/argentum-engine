package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Parting Gust
 * {W}{W}
 * Instant
 *
 * Gift a tapped Fish (You may promise an opponent a gift as you cast this spell.
 * If you do, they create a tapped 1/1 blue Fish creature token before its other effects.)
 *
 * Exile target nontoken creature. If the gift wasn't promised, return that card to the
 * battlefield under its owner's control with a +1/+1 counter on it at the beginning of
 * the next end step.
 */
val PartingGust = card("Parting Gust") {
    manaCost = "{W}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Gift a tapped Fish (You may promise an opponent a gift as you cast this spell. If you do, they create a tapped 1/1 blue Fish creature token before its other effects.)\nExile target nontoken creature. If the gift wasn't promised, return that card to the battlefield under its owner's control with a +1/+1 counter on it at the beginning of the next end step."

    val nontokenCreature = TargetObject(filter = TargetFilter(GameObjectFilter.Creature.nontoken()))

    spell {
        effect = Patterns.Mechanic.giftSpell(
            // Mode 1: No gift — exile and return at end step with +1/+1 counter
            mode("Don't promise a gift — exile target nontoken creature, return it at the next end step with a +1/+1 counter") {
                val targetNontokenCreature = target(nontokenCreature)
                effect = Effects.Move(targetNontokenCreature, Zone.EXILE) then
                    Effects.CreateDelayedTrigger(
                        step = Step.END,
                        effect = Effects.Move(targetNontokenCreature, Zone.BATTLEFIELD) then
                            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, targetNontokenCreature)
                    )
            },
            // Mode 2: Gift a tapped Fish — opponent gets Fish token, exile target permanently
            mode("Promise a gift — opponent creates a tapped 1/1 blue Fish token, then exile target nontoken creature permanently") {
                val targetNontokenCreature = target(nontokenCreature)
                effect = Effects.CreateToken(
                    count = 1,
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.BLUE),
                    creatureTypes = setOf("Fish"),
                    controller = EffectTarget.PlayerRef(Player.ChosenOpponent),
                    tapped = true,
                    imageUri = "https://cards.scryfall.io/normal/front/d/e/de0d6700-49f0-4233-97ba-cef7821c30ed.jpg?1721431109"
                ) then Effects.Exile(targetNontokenCreature) then
                    Effects.GiftGiven()
            }
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "24"
        artist = "Nils Hamm"
        imageUri = "https://cards.scryfall.io/normal/front/1/0/1086e826-94b8-4398-8a38-d8eacca56a43.jpg?1721425901"
        ruling("2024-07-26", "Once the exiled permanent returns, it's considered a new object with no relation to the object that it was.")
        ruling("2024-07-26", "For instants and sorceries with gift, the gift is given to the appropriate opponent as part of the resolution of the spell. This happens before any of the spell's other effects would take place.")
        ruling("2024-07-26", "If a spell for which the gift was promised is countered, doesn't resolve, or is otherwise removed from the stack, the gift won't be given.")
    }
}
