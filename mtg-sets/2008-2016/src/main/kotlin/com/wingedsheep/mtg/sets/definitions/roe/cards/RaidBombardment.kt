package com.wingedsheep.mtg.sets.definitions.roe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Raid Bombardment
 * {2}{R}
 * Enchantment
 *
 * Whenever a creature you control with power 2 or less attacks, this enchantment deals 1 damage to
 * the player or planeswalker that creature is attacking.
 *
 * Modeling notes:
 *  - One trigger per qualifying attacker ([TriggerBinding.ANY] over a power-2-or-less creature you
 *    control). The filter is checked as the ability triggers, not again on resolution — the ruling
 *    that a creature whose power changes afterwards still has the damage dealt.
 *  - The recipient is [EffectTarget.AttackedBy] of the triggering creature: the planeswalker itself
 *    when it attacks one (not its controller), and the one it was attacking if it has left the
 *    battlefield by resolution. Not targeted, so there is no choice when the ability goes on the
 *    stack. Attacking a battle deals nothing (2023 ruling); the engine does not model battles yet,
 *    and a non-planeswalker permanent resolves to nothing when it does.
 */
val RaidBombardment = card("Raid Bombardment") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control with power 2 or less attacks, this enchantment deals 1 damage to the player or planeswalker that creature is attacking."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().powerAtMost(2)).attacks()
        effect = Effects.DealDamage(1, EffectTarget.AttackedBy(EffectTarget.TriggeringEntity))
        description = "Whenever a creature you control with power 2 or less attacks, this enchantment deals 1 damage to the player or planeswalker that creature is attacking."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "161"
        artist = "Matt Cavotta"
        flavorText = "Goblins in the first wave of a fire raid always bemoan the aim of everyone else."
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c2d1a48-efde-4134-95f0-b23f6cf85259.jpg?1783941971"
        ruling("2023-09-01", "Raid Bombardment's ability won't do anything when a creature you control with power 2 or less attacks a battle.")
        ruling("2018-12-07", "The power of the attacking creature is checked only when the ability triggers. Once it triggers, Raid Bombardment will deal 1 damage to the appropriate player or planeswalker even if the creature's power changes before the ability resolves.")
        ruling("2018-12-07", "If you attack with multiple creatures with power 2 or less, Raid Bombardment's ability triggers for each of them separately.")
        ruling("2018-12-07", "If the attacking creature leaves the battlefield before Raid Bombardment's triggered ability resolves, Raid Bombardment deals 1 damage to the player or planeswalker that creature was attacking before it left the battlefield.")
    }
}
