package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.DamageCantBePrevented
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Questing Beast
 * {2}{G}{G}
 * Legendary Creature — Beast
 * 4/4
 * Vigilance, deathtouch, haste
 * Questing Beast can't be blocked by creatures with power 2 or less.
 * Combat damage that would be dealt by creatures you control can't be prevented.
 * Whenever Questing Beast deals combat damage to an opponent, it deals that much damage to target
 * planeswalker that player controls.
 *
 * - The evasion is [CantBeBlockedBy] over `Creature.powerAtMost(2)`, checked as blockers are
 *   declared (a later power change doesn't unblock it — first ruling).
 * - The prevention shutoff is a [DamageCantBePrevented] scoped to combat damage from creatures you
 *   control; it only stops effects that *prevent* (second ruling).
 * - "That player" is the damaged opponent, so the target is a planeswalker
 *   `controlledByTriggeringPlayer()`; with none, the trigger has no target and does nothing
 *   (fourth ruling). The amount is the combat damage just dealt, and the second hit is not
 *   combat damage (third ruling).
 */
val QuestingBeast = card("Questing Beast") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Beast"
    power = 4
    toughness = 4
    oracleText = "Vigilance, deathtouch, haste\n" +
        "Questing Beast can't be blocked by creatures with power 2 or less.\n" +
        "Combat damage that would be dealt by creatures you control can't be prevented.\n" +
        "Whenever Questing Beast deals combat damage to an opponent, it deals that much damage to target planeswalker that player controls."

    keywords(Keyword.VIGILANCE, Keyword.DEATHTOUCH, Keyword.HASTE)

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.Creature.powerAtMost(2))
    }

    replacementEffect(
        DamageCantBePrevented(
            appliesTo = EventPattern.DamageEvent(
                source = GameObjectFilter.Creature.youControl(),
                damageType = DamageType.Combat,
            )
        )
    )

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.Opponent)
        val planeswalker = target(TargetFilter(GameObjectFilter.Planeswalker.controlledByTriggeringPlayer()))
        effect = Effects.DealDamage(DynamicAmounts.triggerDamageAmount(), planeswalker)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "171"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e41cf82d-3213-47ce-a015-6e51a8b07e4f.jpg?1783932604"
        ruling("2019-10-04", "Once a creature with power 3 or greater has blocked this creature, changing the power of the blocking creature won't cause this creature to become unblocked.")
        ruling("2019-10-04", "Questing Beast only stops combat damage from being prevented by effects that specifically use the word “prevent.”")
        ruling("2019-10-04", "The damage Questing Beast deals to the target planeswalker as its last ability resolves isn't combat damage.")
        ruling("2019-10-04", "If the opponent dealt damage controls no planeswalkers, Questing Beast's last ability simply does nothing.")
    }
}
