package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetOther
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Restless Ridgeline
 * Land
 *
 * This land enters tapped.
 * {T}: Add {R} or {G}.
 * {2}{R}{G}: This land becomes a 3/4 red and green Dinosaur creature until end of turn. It's still a land.
 * Whenever this land attacks, another target attacking creature gets +2/+0 until end of turn.
 *   Untap that creature.
 *
 * Mirrors the "Restless" creature-land cycle (see [RestlessAnchorage], [RestlessPrairie]). The
 * attack trigger is an intrinsic triggered ability of the land itself; the land can only attack
 * while it is a creature, so the trigger is effectively active only after the animate ability
 * resolves. "Another target attacking creature" excludes the Ridgeline via [TargetOther]; the
 * +2/+0 buff and the untap both apply to that same chosen creature ([Effects.Composite]).
 */
val RestlessRidgeline = card("Restless Ridgeline") {
    typeLine = "Land"
    colorIdentity = "RG"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {R} or {G}.\n" +
        "{2}{R}{G}: This land becomes a 3/4 red and green Dinosaur creature until end of turn. It's still a land.\n" +
        "Whenever this land attacks, another target attacking creature gets +2/+0 until end of turn. Untap that creature."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{2}{R}{G}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 3,
            toughness = 4,
            creatureTypes = setOf("Dinosaur"),
            colors = setOf(Color.RED.name, Color.GREEN.name),
            duration = Duration.EndOfTurn,
        )
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creature = target(TargetOther(TargetObject(filter = TargetFilter.AttackingCreature)))
        effect = Effects.ModifyStats(
            power = 2,
            toughness = 0,
            target = creature,
            duration = Duration.EndOfTurn,
        ) then
            Effects.Untap(creature)
        description = "Whenever this land attacks, another target attacking creature gets +2/+0 until end of turn. Untap that creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "283"
        artist = "Álvaro Calvo Escudero"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/abde5bed-dc4e-4b2b-820c-18d4d0cf8042.jpg?1782694386"
    }
}
