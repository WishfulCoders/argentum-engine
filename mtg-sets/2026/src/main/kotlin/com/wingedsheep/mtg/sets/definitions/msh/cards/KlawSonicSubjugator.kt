package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Klaw, Sonic Subjugator — Marvel Super Heroes #103
 * {2}{B} · Legendary Creature — Human Rogue Villain · 2/2
 *
 * Sonic Attack — When Klaw enters, target player reveals a number of cards from their hand equal
 * to one plus the number of creature cards in your graveyard. You choose one of them. That player
 * discards that card.
 *
 * "Sonic Attack" is an ability word (CR 207.2c) — flavor only, no rules meaning.
 *
 * The body is the Blackmail / Cabal Interrogator reveal-choose-discard pipeline: gather the target
 * player's hand, have *them* pick the reveal set, have the controller pick one of those, then
 * discard it. The only twist is the reveal count, which is computed at resolution as
 * `1 + creature cards in your graveyard` ([DynamicAmount.Add] over [DynamicAmount.Count] on
 * `Player.You` / [Zone.GRAVEYARD]) — "your" is Klaw's controller, not the target player. A hand
 * smaller than that number simply reveals everything ([SelectionMode.ChooseExactly] auto-selects
 * the whole collection when it can't reach the requested count), and an empty hand makes both
 * selection steps and the discard no-ops.
 */
val KlawSonicSubjugator = card("Klaw, Sonic Subjugator") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Rogue Villain"
    power = 2
    toughness = 2
    oracleText = "Sonic Attack — When Klaw enters, target player reveals a number of cards from " +
        "their hand equal to one plus the number of creature cards in your graveyard. You choose " +
        "one of them. That player discards that card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            // 1. Gather the target player's hand.
            val klawHand = gather(CardSource.FromZone(Zone.HAND, player.asPlayer))
            // 2. That player reveals 1 + creature cards in your graveyard of them.
            val klawRevealed = chooseExactly(
                1 + DynamicAmounts.creatureCardsInYourGraveyard(),
                from = klawHand,
                chooser = Chooser.TargetPlayer,
                prompt = "Choose cards to reveal"
            )
            // 3. Klaw's controller chooses one of the revealed cards.
            val klawChosen = chooseExactly(
                1,
                from = klawRevealed,
                chooser = Chooser.Controller,
                prompt = "Choose a card that player discards"
            )
            // 4. That player discards it.
            discard(klawChosen, player.asPlayer)
        }
        description = "Sonic Attack — When Klaw enters, target player reveals a number of cards " +
            "from their hand equal to one plus the number of creature cards in your graveyard. " +
            "You choose one of them. That player discards that card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "103"
        artist = "Andreia Ugrai"
        flavorText = "\"Listen to the sound of death. Hear the sound of your own cells exploding.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c79a86f8-24e9-49a2-8b1c-72a72fed1985.jpg?1783902943"
    }
}
