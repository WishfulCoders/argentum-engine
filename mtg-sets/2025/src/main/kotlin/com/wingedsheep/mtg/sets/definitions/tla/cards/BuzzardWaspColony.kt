package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Buzzard-Wasp Colony — Avatar: The Last Airbender #88
 * {3}{B} · Creature — Bird Insect · 2/2
 *
 * Flying
 * When this creature enters, you may sacrifice an artifact or creature. If you do, draw a card.
 * Whenever another creature you control dies, if it had counters on it, put its counters on this creature.
 *
 * The ETB "you may sacrifice … If you do, draw" pay-then-payoff is a [GatedEffect] with a
 * [Gate.MayPay] sacrificing an artifact or creature you control.
 *
 * "Another creature you control dies" is `Triggers.<subject>.leaves(to, excludeTo, excludeSacrifice)` with an OTHER binding
 * (excludes this creature itself). The intervening "if it had counters on it" (CR 603.4) is
 * `Conditions.TriggeringEntityHadCounters`, reading the dying creature's last-known counters.
 * `Effects.MoveAllLastKnownCounters` moves *every* counter kind from the dying creature onto
 * this creature (the destination, [EffectTarget.Self]).
 */
val BuzzardWaspColony = card("Buzzard-Wasp Colony") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Bird Insect"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "When this creature enters, you may sacrifice an artifact or creature. If you do, draw a card.\n" +
        "Whenever another creature you control dies, if it had counters on it, put its counters on this creature."

    keywords(Keyword.FLYING)

    // When this creature enters, you may sacrifice an artifact or creature. If you do, draw a card.
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.MayPay(
            cost = Effects.SacrificeOwn(GameObjectFilter.CreatureOrArtifact),
            then = Effects.DrawCards(1)
        )
        description = "When this creature enters, you may sacrifice an artifact or creature. " +
            "If you do, draw a card."
    }

    // Whenever another creature you control dies, if it had counters on it,
    // put its counters on this creature.
    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).dies()
        interveningIf = Conditions.TriggeringEntityHadCounters
        effect = Effects.MoveAllLastKnownCounters(EffectTarget.Self)
        description = "Whenever another creature you control dies, if it had counters on it, " +
            "put its counters on this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "88"
        artist = "Thomas Chamberlain-Keen"
        flavorText = "What doesn't survive the Si Wong Desert assures that the buzzard-wasps do."
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42d83229-0555-4361-8964-4b525c825843.jpg?1764120607"
    }
}
