package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Kellan, Daring Traveler // Journey On
 * {1}{W} // {G}
 * Legendary Creature — Human Faerie Scout // Sorcery — Adventure
 * 2/3
 * Rare — The Lost Caverns of Ixalan #231
 *
 * Kellan, Daring Traveler:
 *   "Whenever Kellan attacks, reveal the top card of your library. If it's a creature card
 *    with mana value 3 or less, put it into your hand. Otherwise, you may put it into your
 *    graveyard."
 *
 * Journey On (Adventure — Sorcery):
 *   "Create X Map tokens, where X is one plus the number of opponents who control an
 *    artifact. (Then exile this card. You may cast the creature later from exile.)"
 *
 * The attack trigger gathers the top card into a `revealed` collection (with `revealed = true`
 * so every player sees it regardless of branch), then branches on whether that card is a
 * creature card with mana value ≤ 3 via [CollectionContainsMatch]. On a match it moves to
 * hand; otherwise the controller *may* move it to the graveyard (declining leaves it on top
 * of the library — no default move). Empty library reveals nothing and neither branch fires.
 *
 * Journey On's X is [DynamicAmount.Add] of one and [DynamicAmount.CountPlayersWith] over
 * [Player.EachOpponent] with the per-player "controls an artifact" test ([Conditions.ControlArtifact],
 * whose `Player.You` rebinds to each candidate opponent). Zero qualifying opponents → X = 1.
 * (CR 715: casting the Adventure exiles the card on resolution and lets the caster cast it as
 * the creature spell while it remains in exile.)
 */
val KellanDaringTraveler = card("Kellan, Daring Traveler") {
    manaCost = "{1}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Human Faerie Scout"
    oracleText = "Whenever Kellan attacks, reveal the top card of your library. If it's a " +
        "creature card with mana value 3 or less, put it into your hand. Otherwise, you may " +
        "put it into your graveyard."
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(1, Player.You), revealed = true)
            run(Effects.If(
                condition = whenMatches(revealed, GameObjectFilter.Creature.manaValueAtMost(3)),
                then = Effects.Pipeline { toHand(revealed) },
                otherwise = Effects.May(
                    Effects.Pipeline { toGraveyard(revealed) },
                    descriptionOverride = "Put the revealed card into your graveyard?"
                )
            ))
        }
    }

    adventure("Journey On") {
        manaCost = "{G}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Create X Map tokens, where X is one plus the number of opponents who " +
            "control an artifact. (Then exile this card. You may cast the creature later from exile.)"
        spell {
            effect = Effects.CreateMapToken(
                1 + DynamicAmounts.countPlayersWith(
                    scope = Player.EachOpponent,
                    condition = Conditions.ControlArtifact
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "231"
        artist = "Marta Nael"
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01739030-c280-492b-a5c9-b3e9f6debc6d.jpg?1782694426"
    }
}
