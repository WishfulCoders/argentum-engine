package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Siren's Call — the group form of Nettling Imp.
 *
 * Every creature the active player controls is marked "attacks this turn if able". A delayed
 * trigger at the next end step (still this turn, as the spell is cast before attackers) destroys
 * the non-Wall creatures that player — still the active player — controls that didn't attack,
 * ignoring any it hasn't controlled continuously since the turn began. Tapped or otherwise
 * unable creatures are destroyed too (rulings).
 */
val SirensCall = card("Siren's Call") {
    manaCost = "{U}"
    typeLine = "Instant"
    oracleText = "Cast this spell only during an opponent's turn, before attackers are declared.\n" +
        "Creatures the active player controls attack this turn if able.\n" +
        "At the beginning of the next end step, destroy all non-Wall creatures that player controls " +
        "that didn't attack this turn. Ignore this effect for each creature the player didn't control " +
        "continuously since the beginning of the turn."

    spell {
        castOnlyIf(Conditions.All(Conditions.IsOpponentsTurn, Conditions.BeforeAttackersDeclared))
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.controlledByActivePlayer()),
            Effects.MarkMustAttackThisTurn(EffectTarget.IterationEntity),
        ) then Effects.CreateDelayedTrigger(
            step = Step.END,
            effect = Effects.DestroyAll(
                GameObjectFilter.Creature
                    .notSubtype(Subtype("Wall"))
                    .controlledByActivePlayer()
                    .didntAttackThisTurn()
                    .controlledSinceTurnBegan()
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "77"
        artist = "Anson Maddocks"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d992b336-3b6e-43e1-8662-d85664349b44.jpg?1783948702"
        ruling("2013-09-20", "If a turn has multiple combat phases, this spell can only be cast before the beginning of the Declare Attackers Step of the first combat phase in that turn.")
        ruling("2009-02-01", "This will destroy creatures that weren't able to attack because they had been previously tapped.")
        ruling("2004-10-04", "It will require creatures with Haste to attack since they are able, but it won't destroy them if they don't for some reason.")
        ruling("2004-10-04", "The creature is destroyed if it does not attack because it simply can't do so legally.")
    }
}
