package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.dsl.Targets

/**
 * Wand of Ith
 * {4}
 * Artifact
 * {3}, {T}: Target player reveals a card at random from their hand. If it's a land card, that
 * player discards it unless they pay 1 life. If it isn't a land card, the player discards it
 * unless they pay life equal to its mana value. Activate only during your turn.
 *
 * Gather → Random(1) → reveal, then the documented partition-and-gate shape: `FilterCollection`
 * splits the revealed card into a land pile and a nonland pile, and each branch pays its own
 * ransom. Exactly one of the two piles is ever non-empty — but `PayOrSuffer` prompts
 * unconditionally rather than checking whether its suffer effect would do anything, so each branch
 * is wrapped in a `ConditionalOnCollectionEffect`. Without that the player is asked to pay for both
 * branches on every activation, and the empty one charges for nothing.
 *
 * The two ransoms differ only in price, and the nonland one is why this card needed new
 * vocabulary: "life equal to its mana value" is a rule, not a number. `Costs.pay.PayDynamicLife`
 * reads it off the revealed card at payment time via `ManaValueSumOfCollection` — the pile holds
 * exactly one card, so the sum *is* that card's mana value, the same trick Necropolis uses for its
 * counters.
 *
 * The payer is the targeted player, not the Wand's controller: `PayOrSufferEffect.player` is
 * pointed at `ContextPlayer(0)`. An empty hand reveals nothing and costs nothing.
 */
val WandOfIth = card("Wand of Ith") {
    manaCost = "{4}"
    typeLine = "Artifact"
    oracleText = "{3}, {T}: Target player reveals a card at random from their hand. If it's a " +
        "land card, that player discards it unless they pay 1 life. If it isn't a land card, the " +
        "player discards it unless they pay life equal to its mana value. Activate only during " +
        "your turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        val victim = target(Targets.Player)
        restrictions = listOf(ActivationRestriction.OnlyDuringYourTurn)

        val payer = victim

        effect = Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, victim.asPlayer))
            val revealed = chooseRandom(1, from = hand)
            reveal(revealed)
            val (land, nonland) = filterSplit(revealed, GameObjectFilter.Land)
            // Each ransom is gated on its own pile being non-empty. PayOrSuffer prompts
            // unconditionally — it does not ask whether its suffer effect would do anything — so
            // without these gates the player is asked to pay for *both* branches every activation,
            // one of which is always empty.
            ifNotEmpty(land) {
                run(Effects.PayOrSuffer(
                    cost = Costs.pay.PayLife(1),
                    suffer = Effects.Pipeline { discard(land, victim.asPlayer) },
                    player = payer,
                ))
            }
            ifNotEmpty(nonland) {
                run(Effects.PayOrSuffer(
                    cost = Costs.pay.PayDynamicLife(DynamicAmounts.manaValueSumOf(nonland)),
                    suffer = Effects.Pipeline { discard(nonland, victim.asPlayer) },
                    player = payer,
                ))
            }
        }
        description = "{3}, {T}: Target player reveals a card at random from their hand. If it's " +
            "a land card, that player discards it unless they pay 1 life. If it isn't a land " +
            "card, the player discards it unless they pay life equal to its mana value."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "114"
        artist = "Quinton Hoover"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80c9070a-8c70-480e-a476-e00f8e2c71b9.jpg?1783947923"
    }
}
