package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets

/**
 * Cabal Interrogator
 * {1}{B}
 * Creature — Zombie Wizard
 * 1/1
 * {X}{B}, {T}: Target player reveals X cards from their hand and you choose one of them.
 * That player discards that card. Activate only as a sorcery.
 */
val CabalInterrogator = card("Cabal Interrogator") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Wizard"
    power = 1
    toughness = 1
    oracleText = "{X}{B}, {T}: Target player reveals X cards from their hand and you choose one of them. That player discards that card. Activate only as a sorcery."

    activatedAbility {
        val player = target(Targets.Player)
        cost = Costs.Composite(Costs.Mana("{X}{B}"), Costs.Tap)
        timing = TimingRule.SorcerySpeed
        effect = Effects.Pipeline {
            // 1. Gather all cards from target player's hand
            val hand = gather(CardSource.FromZone(Zone.HAND, player.asPlayer))
            // 2. Target player chooses X cards to reveal (auto-selects all if ≤X, skips if empty)
            val revealed = chooseExactly(DynamicAmounts.xValue(), from = hand, chooser = Chooser.TargetPlayer)
            // 3. Controller chooses 1 to discard
            val toDiscard = chooseExactly(1, from = revealed, chooser = Chooser.Controller)
            // 4. Move chosen card to target player's graveyard
            discard(toDiscard, player.asPlayer)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "57"
        artist = "Tony Szczudlo"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/256a7a37-6f47-47a3-b149-5692aee8b34a.jpg?1562526599"
    }
}
