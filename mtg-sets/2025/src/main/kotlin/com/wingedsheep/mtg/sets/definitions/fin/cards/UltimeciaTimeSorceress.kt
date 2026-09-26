package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step

/**
 * Ultimecia, Time Sorceress // Ultimecia, Omnipotent — Final Fantasy #247
 * {3}{U}{B} · Legendary Creature — Human Warlock · 4/5 // Legendary Creature — Nightmare Warlock · 7/7
 *
 * Front — Ultimecia, Time Sorceress:
 *   Whenever Ultimecia enters or attacks, surveil 2.
 *   At the beginning of your end step, you may pay {4}{U}{U}{B}{B} and exile eight cards
 *   from your graveyard. If you do, transform Ultimecia.
 *
 * Back — Ultimecia, Omnipotent:
 *   Menace
 *   Time Compression — When this creature transforms into Ultimecia, Omnipotent, take an
 *   extra turn after this one.
 *
 * "Enters or attacks" is the Gilgamesh/Frodo shape: two sibling triggered abilities. The
 * end-step pay-and-exile is an [Effects.MayPay] (Gate.MayPay) whose cost composes a mana
 * payment with a choose-exactly-8 exile pipeline, wrapped in a [Effects.If] requiring
 * eight cards in your graveyard — the MayPay affordability pre-pass checks the mana half but
 * cannot see into the pipeline half, so the condition keeps an unpayable "yes" from being
 * offered (you can't partially pay a cost).
 */
private val UltimeciaOmnipotent = card("Ultimecia, Omnipotent") {
    manaCost = ""
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Nightmare Warlock"
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Time Compression — When this creature transforms into Ultimecia, Omnipotent, take an " +
        "extra turn after this one."
    power = 7
    toughness = 7

    keywords(Keyword.MENACE)

    // Time Compression — When this creature transforms into Ultimecia, Omnipotent, take an
    // extra turn after this one.
    triggeredAbility {
        trigger = Triggers.self.transforms(true)
        effect = Effects.TakeExtraTurn()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "247"
        artist = "Mikio Masuda"
        flavorText = "\"Time... it will not wait... no matter... how hard you hold on. It escapes you...\""
        imageUri = "https://cards.scryfall.io/normal/back/2/d/2d6a2b68-5407-464e-a335-7866fd969c30.jpg?1782686404"
    }
}

private val UltimeciaTimeSorceressFront = card("Ultimecia, Time Sorceress") {
    manaCost = "{3}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Human Warlock"
    oracleText = "Whenever Ultimecia enters or attacks, surveil 2. (Look at the top two cards " +
        "of your library, then put any number of them into your graveyard and the rest on top " +
        "of your library in any order.)\n" +
        "At the beginning of your end step, you may pay {4}{U}{U}{B}{B} and exile eight cards " +
        "from your graveyard. If you do, transform Ultimecia."
    power = 4
    toughness = 5

    // "Whenever Ultimecia enters …"
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Surveil(2)
    }

    // "… or attacks, surveil 2."
    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Surveil(2)
    }

    // At the beginning of your end step, you may pay {4}{U}{U}{B}{B} and exile eight cards
    // from your graveyard. If you do, transform Ultimecia.
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.If(
            condition = Conditions.CardsInGraveyardAtLeast(8),
            then = Effects.MayPay(
                cost = Effects.PayMana("{4}{U}{U}{B}{B}") then
                    Effects.Pipeline {
                        val grave = gather(
                            CardSource.FromZone(Zone.GRAVEYARD, Player.You),
                        )
                        val toExile = chooseExactly(
                            8,
                            from = grave,
                            prompt = "Choose eight cards from your graveyard to exile",
                        )
                        move(toExile, CardDestination.ToZone(Zone.EXILE))
                    },
                then = Effects.Transform(EffectTarget.Self),
                descriptionOverride = "Pay {4}{U}{U}{B}{B} and exile eight cards from your " +
                    "graveyard? If you do, transform Ultimecia.",
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "247"
        artist = "Mikio Masuda"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d6a2b68-5407-464e-a335-7866fd969c30.jpg?1782686404"
    }
}

val UltimeciaTimeSorceress: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = UltimeciaTimeSorceressFront,
    backFace = UltimeciaOmnipotent,
)
