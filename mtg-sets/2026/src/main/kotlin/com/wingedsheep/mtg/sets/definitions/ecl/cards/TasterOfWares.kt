package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets

/**
 * Taster of Wares
 * {2}{B}
 * Creature — Goblin Warlock
 * 3/2
 *
 * When this creature enters, target opponent reveals X cards from their hand,
 * where X is the number of Goblins you control. You choose one of those cards.
 * That player exiles it. If an instant or sorcery card is exiled this way, you
 * may cast it for as long as you control this creature, and mana of any type
 * can be spent to cast that spell.
 *
 * Implementation notes:
 * - The cast-from-exile permission carries `MayPlayExpiry.WhileYouControlSource`,
 *   so it ends the moment this creature leaves the battlefield or someone else
 *   gains control of it. Per CR 611.2b the window is one-way: the grant is not
 *   created at all if the Taster is already gone when the trigger resolves, and
 *   getting it back later does not revive a permission that has ended. The card
 *   stays exiled either way — only the permission ends.
 * - "Mana of any type can be spent" is plumbed via `withAnyManaType = true` on
 *   the granted permission, which relaxes colored cost requirements at cast time.
 */
val TasterOfWares = card("Taster of Wares") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Goblin Warlock"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, target opponent reveals X cards from their hand, where X is the number of Goblins you control. You choose one of those cards. That player exiles it. If an instant or sorcery card is exiled this way, you may cast it for as long as you control this creature, and mana of any type can be spent to cast that spell."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        description = "When this creature enters, target opponent reveals X cards from their hand, " +
            "where X is the number of Goblins you control. You choose one of those cards. " +
            "That player exiles it. If an instant or sorcery card is exiled this way, you may cast it " +
            "for as long as you control this creature, and mana of any type can be spent to cast that spell."
        effect = Effects.Pipeline {
            // Snapshot the number of Goblins you control as X
            val goblinCount = storeNumber(
                DynamicAmounts.battlefield(
                    Player.You,
                    GameObjectFilter.Any.withSubtype(Subtype.GOBLIN)
                ).count()
            )
            // Gather opponent's hand
            val hand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            // Opponent chooses X cards to reveal
            val revealed = chooseExactly(
                goblinCount.amount,
                from = hand,
                chooser = Chooser.TargetPlayer,
                prompt = "Reveal X cards from your hand (X = number of Goblins your opponent controls)"
            )
            // You choose one of the revealed cards. Mandatory (ChooseExactly), not a "may":
            // ChooseUpTo would offer a min-0 decision that a player or the AI can decline,
            // and nothing would be exiled. ChooseExactly still resolves cleanly when X = 0 or
            // the hand is empty — an empty collection auto-selects nothing rather than pausing.
            val chosen = chooseExactly(
                1,
                from = revealed,
                chooser = Chooser.Controller,
                prompt = "Choose one of the revealed cards to exile"
            )
            // That player exiles it
            exile(chosen, opponent.asPlayer)
            // If the exiled card is an instant or sorcery, grant cast-from-exile
            val instantOrSorcery = filter(chosen, GameObjectFilter.InstantOrSorcery)
            run(Effects.GrantMayPlayFromExile(
                from = instantOrSorcery,
                expiry = MayPlayExpiry.WhileYouControlSource("this creature"),
                withAnyManaType = true
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "121"
        artist = "Edgar Sánchez Hidalgo"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc0b64b6-8984-431c-8a2f-84402b429e2b.jpg?1767952030"
    }
}
