package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Desert Were-Worm — The Hobbit #92
 * {4}{R}{R} · Creature — Dragon Wurm · Rare
 * 0/5
 *
 * This creature gets +2/+0 for each Mountain you control.
 * Whenever you attack with creatures with total power 12 or greater for the first time each turn,
 * untap all attacking creatures. After this phase, there is an additional combat phase.
 *
 * Modeling notes:
 *  - The Mountain pump is a static [GrantDynamicStats] on the source, so it feeds the total
 *    power the attack trigger measures — a Were-Worm swinging alongside six Mountains is already
 *    12 power by itself.
 *  - "For the first time each turn" is `oncePerTurn` on the ability paired with an
 *    intervening-if [Compare] on the attackers' total power (CR 603.4). Because the condition is
 *    checked before the ability triggers, an under-12 attack in an earlier combat doesn't spend
 *    the turn's single use — the trigger still fires on a later combat that does reach 12.
 *  - "Untap all attacking creatures" is every attacker on the battlefield, not just yours, so the
 *    group filter is unscoped by controller.
 *  - "After this phase, there is an additional combat phase" is [Effects.AddCombatPhase] alone —
 *    combat only, with no trailing main phase (CR 500.8).
 */
val DesertWereWorm = card("Desert Were-Worm") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon Wurm"
    power = 0
    toughness = 5
    oracleText = "This creature gets +2/+0 for each Mountain you control.\n" +
        "Whenever you attack with creatures with total power 12 or greater for the first time " +
        "each turn, untap all attacking creatures. After this phase, there is an additional " +
        "combat phase."

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Land.withSubtype(Subtype.MOUNTAIN)
            ).count() * 2,
            toughnessBonus = DynamicAmounts.fixed(0)
        )
    }

    triggeredAbility {
        trigger = Triggers.you.attacks()
        triggerRestriction = Conditions.CompareAmounts(
            left = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Creature.attacking()
            ).sumPower(),
            operator = ComparisonOperator.GTE,
            right = 12
        )
        oncePerTurn = true
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(baseFilter = GameObjectFilter.Creature.attacking()),
            effect = Effects.Untap(EffectTarget.IterationEntity)
        ) then
            Effects.AddCombatPhase
        description = "Whenever you attack with creatures with total power 12 or greater for the " +
            "first time each turn, untap all attacking creatures. After this phase, there is an " +
            "additional combat phase."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "92"
        artist = "Aldo Domínguez"
        flavorText = "Fight wild Were-worms in the Last Desert\n" +
            "—Expression meaning \"an impossible task\""
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc12c22a-11ff-4fb0-bc42-dd8490b8efb7.jpg?1784733924"
    }
}
