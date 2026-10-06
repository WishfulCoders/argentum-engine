package com.wingedsheep.mtg.sets.definitions.ice.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Dance of the Dead
 * {1}{B}
 * Enchantment — Aura
 * Enchant creature card in a graveyard
 * When this Aura enters, if it's on the battlefield, it loses "enchant creature card in a graveyard"
 * and gains "enchant creature put onto the battlefield with this Aura." Put enchanted creature card
 * onto the battlefield tapped under your control and attach this Aura to it. When this Aura leaves
 * the battlefield, that creature's controller sacrifices it.
 * Enchanted creature gets +1/+1 and doesn't untap during its controller's untap step.
 * At the beginning of the upkeep of enchanted creature's controller, that player may pay {1}{B}. If
 * the player does, untap that creature.
 *
 * Animate Dead's reanimation-Aura shape (`Effects.EnchantPutOntoBattlefield` after the return, then
 * a delayed "leaves the battlefield" sacrifice that remembers the returned creature), returning the
 * card tapped. The untap tax is an attached upkeep trigger: it is controlled by the enchanted
 * creature's controller, who decides whether to pay.
 */
val DanceOfTheDead = card("Dance of the Dead") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature card in a graveyard\n" +
        "When this Aura enters, if it's on the battlefield, it loses \"enchant creature card in a graveyard\" " +
        "and gains \"enchant creature put onto the battlefield with this Aura.\" Put enchanted creature card " +
        "onto the battlefield tapped under your control and attach this Aura to it. When this Aura leaves " +
        "the battlefield, that creature's controller sacrifices it.\n" +
        "Enchanted creature gets +1/+1 and doesn't untap during its controller's untap step.\n" +
        "At the beginning of the upkeep of enchanted creature's controller, that player may pay {1}{B}. " +
        "If the player does, untap that creature."

    auraTarget = TargetObject(filter = TargetFilter.CreatureInGraveyard)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.SourceInZone(Zone.BATTLEFIELD)
        effect = Effects.Pipeline {
            val returned = gather(
                CardSource.FromZone(Zone.GRAVEYARD, Player.Each, GameObjectFilter.Creature.attachedToBySource())
            )
            move(returned, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You, ZonePlacement.Tapped))
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

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    triggeredAbility {
        trigger = Triggers.attached.beginningOf(Step.UPKEEP)
        effect = Effects.MayPay(
            cost = ManaCost.parse("{1}{B}"),
            then = Effects.Untap(EffectTarget.EnchantedCreature)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "118"
        artist = "Randy Gallegos"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7c53ba4-9956-4cd6-85ca-2d6b61a5127c.jpg?1783947505"
        ruling(
            "2008-04-01",
            "This is a new wording. Dance of the Dead is now an Aura. You target a creature card in a graveyard " +
                "when you cast it. It enters attached to that card. Then it returns that card to the battlefield, " +
                "and attaches itself to that card again (since the card is treated as a new object on the battlefield)."
        )
        ruling(
            "2008-04-01",
            "Once the creature is returned to the battlefield, Dance of the Dead can't be attached to anything " +
                "other than it (unless Dance of the Dead somehow manages to put a different creature onto the " +
                "battlefield). Attempting to move Dance of the Dead to another creature won't work."
        )
        ruling(
            "2008-04-01",
            "If the creature card put onto the battlefield has protection from black (or anything that prevents " +
                "this from legally being attached), this won't be able to attach to it. Then this will go to the " +
                "graveyard as a state-based action, causing the creature to be sacrificed."
        )
        ruling(
            "2004-10-04",
            "If more than one Dance of the Dead ends up on a creature, each contributes a +1/+1. But you only " +
                "have to pay the untap cost once. You may pay for each one, however, and untap the card more than " +
                "once during upkeep."
        )
    }
}
