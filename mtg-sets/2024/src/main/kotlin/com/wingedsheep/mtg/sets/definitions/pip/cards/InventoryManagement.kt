package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Inventory Management — Fallout #105
 * {R}{W} · Instant
 *
 * Split second
 * For each Aura and Equipment you control, you may attach it to a creature you control.
 *
 * The Auras and Equipment are snapshotted as the spell resolves and visited one at a time; for
 * each, its controller may move it to a creature they control it could legally be attached to
 * (CR 701.3a — an Aura's enchant restriction, protection, an Equipment's creature requirement).
 * Its current host is never offered: attaching it to the creature it's already on does nothing
 * (CR 701.3b), and declining the "may" leaves it where it is. The new host isn't targeted, so
 * hexproof and shroud don't matter.
 */
val InventoryManagement = card("Inventory Management") {
    manaCost = "{R}{W}"
    typeLine = "Instant"
    oracleText = "Split second (As long as this spell is on the stack, players can't cast spells or activate abilities that aren't mana abilities.)\n" +
        "For each Aura and Equipment you control, you may attach it to a creature you control."

    keywords(Keyword.SPLIT_SECOND)

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(
                GameObjectFilter.Permanent
                    .withAnySubtype(Subtype.AURA.value, Subtype.EQUIPMENT.value)
                    .youControl()
            ),
            Effects.May(
                Effects.AttachToChosenHost(
                    EffectTarget.IterationEntity,
                    GameObjectFilter.Creature.youControl()
                ),
                prompt = "Attach this to a creature you control?"
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "105"
        artist = "Kamila Szutenberg"
        flavorText = "Constant danger requires constant preparedness."
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a1acb59-a679-4c6d-8458-8fe5556b9d4d.jpg?1783912388"
        ruling("2024-03-08", "You can’t try to attach an Aura or Equipment to a creature if that Aura or Equipment can’t legally be attached to it.")
        ruling("2024-03-08", "Players still get priority while a spell with split second is on the stack; their options are just limited to mana abilities and certain special actions.")
        ruling("2024-03-08", "Split second doesn’t stop triggered abilities from triggering. If one does, its controller puts it on the stack and chooses targets for it, if any. Those abilities will resolve as normal.")
        ruling("2024-03-08", "After a spell with split second resolves (or otherwise leaves the stack), players may again cast spells and activate abilities before the next object on the stack resolves.")
    }
}
