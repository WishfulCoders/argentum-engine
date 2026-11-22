package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Brotherhood Outcast — Fallout #12
 * {2}{W} · Creature — Human Soldier · 3/2
 *
 * When this creature enters, choose one —
 * • Return target Aura or Equipment card with mana value 3 or less from your graveyard to the
 *   battlefield.
 * • Put a shield counter on target creature.
 *
 * A returned Aura isn't cast, so the targeted move to the battlefield asks its controller what it
 * enchants as it enters (ignoring hexproof/shroud, bound by its enchant ability); with no legal
 * host it stays in the graveyard. A returned Equipment enters unattached.
 */
val BrotherhoodOutcast = card("Brotherhood Outcast") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, choose one —\n" +
        "• Return target Aura or Equipment card with mana value 3 or less from your graveyard to the battlefield.\n" +
        "• Put a shield counter on target creature. (If it would be dealt damage or destroyed, remove a shield counter from it instead.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            mode("Return an Aura or Equipment card with mana value 3 or less from your graveyard to the battlefield") {
                val returned = target(
                    TargetFilter.CardInGraveyard
                        .withAnySubtype("Aura", "Equipment")
                        .manaValueAtMost(3)
                        .ownedByYou()
                )
                effect = Effects.Move(returned, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
            },
            mode("Put a shield counter on target creature") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.AddCounters(CounterType.SHIELD, 1, creature)
            }
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "12"
        artist = "Brian Valeza"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e96e656-7146-420d-aaae-3aedd773eea2.jpg?1783912418"
        ruling("2024-03-08", "If an Aura is put onto the battlefield without being cast, the Aura’s controller-to-be chooses what it will enchant as it comes back onto the battlefield. An Aura put onto the battlefield this way doesn’t target anything (so it could be attached to an opponent’s permanent with hexproof, for example), but the Aura’s enchant ability restricts what it can be attached to. If the Aura can’t legally be attached to anything, it remains in its current zone.")
        ruling("2024-03-08", "Shield counters don’t prevent players from sacrificing creatures.")
        ruling("2024-03-08", "If a permanent that would be dealt damage has more than one shield counter on it, that damage is prevented and only one shield counter is removed.")
        ruling("2024-03-08", "If a permanent with a shield counter is dealt unpreventable damage, that damage will be dealt and a shield counter will still be removed.")
    }
}
