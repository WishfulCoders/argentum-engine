package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Vault 101: Birthday Party — Fallout #28
 * {3}{W} · Enchantment — Saga · Rare
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Create a 1/1 white Human Soldier creature token and a Food token.
 * II, III — You may put an Aura or Equipment card from your hand or graveyard onto the
 * battlefield. If an Equipment is put onto the battlefield this way, you may attach it to a
 * creature you control.
 *
 * Chapters II and III are Danitha, Benalia's Hope's hand + graveyard `chooseUpTo(1)` (choosing
 * nothing is the "you may"), but with no fixed host. An Aura goes through the ordinary
 * battlefield move, so its controller picks what it enchants as it enters — not targeted, any
 * legal object (CR 303.4f) — and one with nothing to enchant stays where it was (CR 303.4g),
 * which leaves the same game as never choosing it (the ruling's "can't choose to put it onto the
 * battlefield"). `moveTracked` records what actually entered, so only an Equipment that really
 * arrived gets Inventory Management's optional `AttachToChosenHost` onto a creature you control
 * (only those it could legally be attached to are offered).
 */
val Vault101BirthdayParty = card("Vault 101: Birthday Party") {
    manaCost = "{3}{W}"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Create a 1/1 white Human Soldier creature token and a Food token. (A Food token is an " +
        "artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n" +
        "II, III — You may put an Aura or Equipment card from your hand or graveyard onto the " +
        "battlefield. If an Equipment is put onto the battlefield this way, you may attach it to a " +
        "creature you control."

    sagaChapter(1) {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human", "Soldier"),
            imageUri = "https://cards.scryfall.io/normal/front/7/3/737741e7-b2b2-4c41-ab7c-4eeaf9f37b71.jpg?1783912046"
        ) then Effects.CreateFood(1)
    }

    for (chapter in 2..3) {
        sagaChapter(chapter) {
            effect = Effects.Pipeline {
                val candidates = gather(
                    CardSource.FromMultipleZones(
                        listOf(Zone.HAND, Zone.GRAVEYARD),
                        Player.You,
                        GameObjectFilter.Any.withAnySubtype(Subtype.AURA.value, Subtype.EQUIPMENT.value)
                    )
                )
                val putting = chooseUpTo(
                    1,
                    from = candidates,
                    prompt = "You may put an Aura or Equipment card onto the battlefield"
                )
                val entered = moveTracked(putting, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
                ifNotEmpty(entered, filter = GameObjectFilter.Any.withSubtype(Subtype.EQUIPMENT.value)) {
                    run(
                        Effects.May(
                            Effects.AttachToChosenHost(entered.asTarget, GameObjectFilter.Creature.youControl()),
                            prompt = "Attach this Equipment to a creature you control?",
                            feasibility = FeasibilityCheck.ControlsPermanentMatching(GameObjectFilter.Creature.youControl())
                        )
                    )
                }
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "28"
        artist = "Andrea Piparo"
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc513659-ea88-4d04-82bd-5bb2e740d957.jpg?1783912412"
        ruling(
            "2024-03-08",
            "An Aura put onto the battlefield this way doesn't target anything (so it could be attached to " +
                "an opponent's permanent with hexproof, for example), but the Aura's enchant ability restricts " +
                "what it can be attached to. If the Aura can't legally be attached to anything, you can't " +
                "choose to put it onto the battlefield at all."
        )
    }
}
