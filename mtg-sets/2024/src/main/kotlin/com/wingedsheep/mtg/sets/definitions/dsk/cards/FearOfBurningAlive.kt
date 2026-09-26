package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fear of Burning Alive
 * {4}{R}{R}
 * Enchantment Creature — Nightmare
 * 4/4
 * When this creature enters, it deals 4 damage to each opponent.
 * Delirium — Whenever a source you control deals noncombat damage to an opponent, if there are four
 * or more card types among cards in your graveyard, this creature deals that amount of damage to
 * target creature that player controls.
 *
 * Implementation notes:
 * - The ETB is an [EffectTarget.PlayerRef] over [Player.EachOpponent] (the source attribution is
 *   the enchantment creature itself, matching "it deals").
 * - The Delirium ability is an observer trigger: `dealsDamage(NonCombat, Opponent, sourceFilter =
 *   "you control", binding = ANY)` so any source you control (including this creature's own ETB
 *   damage to an opponent) re-fires it. The intervening-if (CR 603.4) is [Conditions.Delirium] —
 *   checked both when the trigger would fire and on resolution. The redealt damage amount is
 *   [ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT] ("that amount"), the damaged player is the
 *   triggering player ([Player.TriggeringPlayer]), and the damage source is this creature
 *   ([EffectTarget.Self], "this creature deals").
 */
val FearOfBurningAlive = card("Fear of Burning Alive") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment Creature — Nightmare"
    power = 4
    toughness = 4
    oracleText = "When this creature enters, it deals 4 damage to each opponent.\n" +
        "Delirium — Whenever a source you control deals noncombat damage to an opponent, if there " +
        "are four or more card types among cards in your graveyard, this creature deals that amount " +
        "of damage to target creature that player controls."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DealDamage(
            amount = 4,
            target = EffectTarget.PlayerRef(Player.EachOpponent),
        )
        description = "When this creature enters, it deals 4 damage to each opponent."
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Any.youControl()).dealsDamage(Recipient.Opponent, damageType = DamageType.NonCombat)
        interveningIf = Conditions.Delirium()
        val t = target(
            TargetFilter(
                GameObjectFilter.Creature.targetPlayerControls(
                    EffectTarget.PlayerRef(Player.TriggeringPlayer),
                ),
            ),
        )
        effect = Effects.DealDamage(
            amount = DynamicAmounts.triggerDamageAmount(),
            target = t,
            damageSource = EffectTarget.Self,
        )
        description = "Delirium — Whenever a source you control deals noncombat damage to an " +
            "opponent, if there are four or more card types among cards in your graveyard, this " +
            "creature deals that amount of damage to target creature that player controls."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "135"
        artist = "J.P. Targete"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b282f8e3-8b79-47e9-8c18-62284211442b.jpg?1726286352"
    }
}
