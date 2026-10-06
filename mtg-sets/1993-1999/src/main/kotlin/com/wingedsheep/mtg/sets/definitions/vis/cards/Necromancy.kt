package com.wingedsheep.mtg.sets.definitions.vis.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Necromancy
 * {2}{B}
 * Enchantment
 * You may cast this spell as though it had flash. If you cast it any time a sorcery couldn't have
 * been cast, the controller of the permanent it becomes sacrifices it at the beginning of the next
 * cleanup step.
 * When this enchantment enters, if it's on the battlefield, it becomes an Aura with "enchant
 * creature put onto the battlefield with Necromancy." Put target creature card from a graveyard
 * onto the battlefield under your control and attach this enchantment to it. When this enchantment
 * leaves the battlefield, that creature's controller sacrifices it.
 *
 * The first paragraph is `flashWithCleanupSacrifice`. Necromancy is cast as a plain enchantment —
 * it targets nothing until its enters trigger does. As that trigger resolves it becomes an Aura (a
 * layer-4 subtype addition lasting as long as this object is on the battlefield) whose only enchant
 * ability is "enchant creature put onto the battlefield with Necromancy" (`Effects.EnchantPutOntoBattlefield`),
 * returns the target and attaches to it; a creature it can't enchant leaves it an unattached Aura,
 * which the state-based action puts into the graveyard. The leaves-the-battlefield sacrifice is a
 * delayed trigger that remembers the returned creature, as on Animate Dead.
 */
val Necromancy = card("Necromancy") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "You may cast this spell as though it had flash. If you cast it any time a sorcery couldn't " +
        "have been cast, the controller of the permanent it becomes sacrifices it at the beginning of the next " +
        "cleanup step.\n" +
        "When this enchantment enters, if it's on the battlefield, it becomes an Aura with \"enchant creature " +
        "put onto the battlefield with Necromancy.\" Put target creature card from a graveyard onto the " +
        "battlefield under your control and attach this enchantment to it. When this enchantment leaves the " +
        "battlefield, that creature's controller sacrifices it."

    flashWithCleanupSacrifice = true

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.SourceInZone(Zone.BATTLEFIELD)
        target(TargetFilter.CreatureInGraveyard)
        effect = Effects.Pipeline {
            run(Effects.AddSubtype("Aura", EffectTarget.Self, Duration.Permanent))
            val returned = gather(CardSource.ChosenTargets)
            move(returned, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
            run(Effects.EnchantPutOntoBattlefield(returned))
            run(
                Effects.CreateDelayedTrigger(
                    trigger = Triggers.self.leaves(),
                    watchedTarget = EffectTarget.Self,
                    fireOnce = true,
                    expiry = DelayedTriggerExpiry.Never,
                    carryCollections = listOf(returned.key),
                    effect = Effects.SacrificeTarget(returned.asTarget, sacrificedByItsController = true)
                )
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "Pete Venters"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/311a6257-dd77-4bb6-81cb-c8e7862350f3.jpg?1783946992"
        ruling(
            "2022-12-08",
            "The sacrifice occurs only if you cast it using its own ability. If you cast it using some other " +
                "effect (for example, if another effect allowed you to cast it as though it had flash), then it " +
                "won't be sacrificed."
        )
        ruling(
            "2008-04-01",
            "If the creature card put onto the battlefield has protection from black (or anything that prevents " +
                "this from legally being attached), this won't be able to attach to it. Then this will go to the " +
                "graveyard as a state-based action, causing the creature to be sacrificed."
        )
        ruling(
            "2005-08-01",
            "Necromancy enters as an enchantment and then becomes an Enchant Creature Aura as a triggered " +
                "ability upon entering. It follows all the rules for Auras from then on."
        )
        ruling(
            "2004-10-04",
            "The bringing of the creature onto the battlefield and then putting Necromancy on it is all done " +
                "as part of the resolution."
        )
    }
}
