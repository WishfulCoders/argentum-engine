package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kroxa, Titan of Death's Hunger
 * {B}{R}
 * Legendary Creature — Elder Giant
 * 6/6
 * When Kroxa enters, sacrifice it unless it escaped.
 * Whenever Kroxa enters or attacks, each opponent discards a card, then each opponent who didn't
 * discard a nonland card this way loses 3 life.
 * Escape—{B}{B}{R}{R}, Exile five other cards from your graveyard.
 *
 * Phlage's shape (its MH3 sibling): "unless it escaped" is an [Effects.If] over [Conditions.Escaped]
 * checked on resolution, and the enters-or-attacks trigger fires on entry whether or not Kroxa
 * escaped. The punisher runs per opponent ([Effects.ForEachPlayer] rebinds `Player.You` to the
 * opponent): they choose and discard a card, and lose 3 life unless that card was a nonland card —
 * so discarding a land, or having no card to discard, both cost 3 life (ruling 2020-01-24).
 *
 * Known deviation: in multiplayer, the ruling has every opponent set a card aside before all are
 * discarded at once; the per-opponent loop discards one opponent at a time. Identical in a
 * two-player game.
 */
private val kroxaPunisher: Effect = Effects.ForEachPlayer(
    Player.EachOpponent,
    Effects.Pipeline {
        val hand = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Any))
        val discarded = chooseExactly(
            1,
            from = hand,
            chooser = Chooser.Controller,
            prompt = "Choose a card to discard",
        )
        discard(discarded)
        ifNotEmpty(discarded, filter = GameObjectFilter.Nonland) {
            run(Effects.Nothing)
        } orElse {
            run(Effects.LoseLife(3, EffectTarget.PlayerRef(Player.You)))
        }
    },
)

val KroxaTitanOfDeathsHunger = card("Kroxa, Titan of Death's Hunger") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Elder Giant"
    power = 6
    toughness = 6
    oracleText = "When Kroxa enters, sacrifice it unless it escaped.\n" +
        "Whenever Kroxa enters or attacks, each opponent discards a card, then each opponent who didn't " +
        "discard a nonland card this way loses 3 life.\n" +
        "Escape—{B}{B}{R}{R}, Exile five other cards from your graveyard. " +
        "(You may cast this card from your graveyard for its escape cost.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.If(Conditions.Not(Conditions.Escaped), SacrificeSelfEffect)
        description = "When Kroxa enters, sacrifice it unless it escaped."
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = kroxaPunisher
        description = "Whenever Kroxa enters, each opponent discards a card, then each opponent who didn't " +
            "discard a nonland card this way loses 3 life."
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = kroxaPunisher
        description = "Whenever Kroxa attacks, each opponent discards a card, then each opponent who didn't " +
            "discard a nonland card this way loses 3 life."
    }

    keywordAbility(KeywordAbility.escape("{B}{B}{R}{R}", Costs.additional.ExileOtherCards(5)))

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "221"
        artist = "Vincent Proce"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/cee0459b-9aac-4d2f-abe4-4d5fedde7eb8.jpg?1783931520"
        ruling("2020-01-24", "Kroxa's second ability triggers when it enters the battlefield, even if it didn't escape.")
        ruling("2020-01-24", "An opponent loses 3 life if they discard a land card or if they can't discard a card at all.")
        ruling("2020-01-24", "Kroxa's first ability causes you to sacrifice it if you didn't cast it, or if it was cast using any permission other than an escape ability.")
    }
}
