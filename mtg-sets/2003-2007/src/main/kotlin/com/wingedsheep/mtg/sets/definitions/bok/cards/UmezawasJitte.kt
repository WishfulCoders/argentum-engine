package com.wingedsheep.mtg.sets.definitions.bok.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Umezawa's Jitte
 * {2}
 * Legendary Artifact — Equipment
 *
 * Whenever equipped creature deals combat damage, put two charge counters on Umezawa's Jitte.
 * Remove a charge counter from Umezawa's Jitte: Choose one —
 * • Equipped creature gets +2/+2 until end of turn.
 * • Target creature gets -1/-1 until end of turn.
 * • You gain 2 life.
 * Equip {2}
 *
 * The trigger is [Triggers.attached] `.dealsCombatDamage()` with no recipient: the equipped
 * creature's combat damage in one combat damage step is dealt simultaneously (CR 510.2) and is one
 * trigger event (CR 603.2c), so trampling over a blocker or splitting damage between two blockers
 * puts two counters on the Jitte, not four. A double striker deals combat damage in two steps and
 * so triggers twice. The counters live on the Jitte itself ([EffectTarget.Self]) and stay when it
 * is moved or unattached.
 *
 * The activated ability has no mana cost — only removing a charge counter — so it can be used any
 * time its controller has priority, several times in response to itself. The +2/+2 mode reads the
 * equipped creature at resolution: it buffs whatever the Jitte is attached to then, does nothing if
 * it is unattached, and falls back to the creature it was last attached to if the Jitte has left the
 * battlefield ([EffectTarget.EquippedCreature] resolves through last-known information, CR 608.2h).
 *
 * Known engine limitation, shared with every modal activated ability (Lost Jitte included — see
 * `ModalEffectExecutor`): the mode, and the -1/-1 mode's target, are picked as the ability resolves
 * rather than on activation (CR 602.2b / 700.2a, and the 2005-02-01 ruling below).
 */
val UmezawasJitte = card("Umezawa's Jitte") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "Whenever equipped creature deals combat damage, put two charge counters on Umezawa's Jitte.\n" +
        "Remove a charge counter from Umezawa's Jitte: Choose one —\n" +
        "• Equipped creature gets +2/+2 until end of turn.\n" +
        "• Target creature gets -1/-1 until end of turn.\n" +
        "• You gain 2 life.\n" +
        "Equip {2}"

    // Whenever equipped creature deals combat damage, put two charge counters on Umezawa's Jitte.
    triggeredAbility {
        trigger = Triggers.attached.dealsCombatDamage()
        effect = Effects.AddCounters(CounterType.CHARGE, 2, EffectTarget.Self)
    }

    // Remove a charge counter from Umezawa's Jitte: Choose one —
    activatedAbility {
        cost = Costs.RemoveCounterFromSelf(CounterType.CHARGE)
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.ModifyStats(2, 2, EffectTarget.EquippedCreature),
                "Equipped creature gets +2/+2 until end of turn"
            ),
            mode("Target creature gets -1/-1 until end of turn") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.ModifyStats(-1, -1, creature)
            },
            Mode.noTarget(
                Effects.GainLife(2),
                "You gain 2 life"
            ),
            countsAsModalSpell = false
        )
        description = "Remove a charge counter from Umezawa's Jitte: Choose one — Equipped creature " +
            "gets +2/+2 until end of turn; Target creature gets -1/-1 until end of turn; or You gain 2 life."
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "163"
        artist = "Christopher Moeller"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b6e5956-f795-451b-bb24-56462d1ced27.jpg?1783944176"

        ruling(
            "2005-02-01",
            "Umezawa's Jitte's activated ability generates a modal choice. The choice is made when " +
                "the ability is activated."
        )
        ruling(
            "2005-02-01",
            "The ability can be used any time Umezawa's Jitte's controller has priority — only the " +
                "\"target creature\" choice has additional requirements. Choosing the \"Equipped " +
                "creature gets +2/+2 until end of turn\" mode does nothing if the Jitte isn't equipped " +
                "to a creature when the ability resolves."
        )
        ruling(
            "2005-02-01",
            "If the Jitte leaves the battlefield after the \"+2/+2\" mode is announced but before it " +
                "resolves, the bonus is given to the creature that was most recently equipped once the " +
                "ability resolves."
        )
        ruling(
            "2005-02-01",
            "If the Jitte is moved after the \"+2/+2\" mode is announced but before it resolves, the " +
                "bonus is given to the creature that is equipped when the ability resolves."
        )
    }
}
