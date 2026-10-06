package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.divRoundedUp

/**
 * Personal Incarnation
 * {3}{W}{W}{W}
 * Creature — Avatar Incarnation
 * 6/6
 * {0}: The next 1 damage that would be dealt to this creature this turn is dealt to its owner
 * instead. Only this creature's owner may activate this ability.
 * When this creature dies, its owner loses half their life, rounded up.
 *
 * Both abilities name the *owner*, not the controller, so they part ways once the creature is
 * stolen. "Only its owner may activate" is `AnyPlayerMay` (opens the ability past its controller)
 * narrowed by an `OnlyIfCondition` that the source is owned by the activating player. The dies
 * trigger is controlled by whoever controlled it when it died, but the owner loses the life —
 * `Player.OwnerOfSource` for both the payer and the life total read.
 *
 * Ruling (2007-02-01): halving a negative life total halves 0, so the owner loses nothing — the
 * life total is floored at 0 before it is halved.
 */
val PersonalIncarnation = card("Personal Incarnation") {
    manaCost = "{3}{W}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Avatar Incarnation"
    power = 6
    toughness = 6
    oracleText = "{0}: The next 1 damage that would be dealt to this creature this turn is dealt to its owner instead. " +
        "Only this creature's owner may activate this ability.\n" +
        "When this creature dies, its owner loses half their life, rounded up."

    activatedAbility {
        cost = Costs.Free
        restrictions = listOf(
            ActivationRestriction.All(
                ActivationRestriction.AnyPlayerMay,
                ActivationRestriction.OnlyIfCondition(
                    Conditions.SourceMatches(GameObjectFilter.Any.ownedByYou())
                )
            )
        )
        effect = Effects.RedirectNextDamage(
            protectedTargets = listOf(EffectTarget.Self),
            // Only the owner may activate, and the activating player controls the ability, so the
            // ability's controller *is* the owner — even while someone else controls the creature.
            redirectTo = EffectTarget.Controller,
            amount = 1
        )
        description = "{0}: The next 1 damage that would be dealt to this creature this turn is dealt to its owner instead. Only this creature's owner may activate this ability."
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.LoseLife(
            DynamicAmounts.nonNegative(DynamicAmounts.lifeTotal(Player.OwnerOfSource)) divRoundedUp 2,
            EffectTarget.PlayerRef(Player.OwnerOfSource)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "31"
        artist = "Kev Brockschmidt"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/caf9cef4-0f2d-478a-b119-fe1967687f74.jpg?1783948711"
        ruling("2007-02-01", "If you attempt to halve a negative life total, you halve 0. This means that the life total stays the same. A life total of -10 would remain -10.")
        ruling("2004-10-04", "You do not lose life if this card is exiled or sent someplace without going to the graveyard first.")
        ruling("2004-10-04", "The owner of the Incarnation loses life when it is destroyed, not the controller. So if you control your opponent's Incarnation you can let it die to make them lose life.")
    }
}
