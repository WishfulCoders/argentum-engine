package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Fatal Push
 * {B}
 * Instant
 * Destroy target creature if it has mana value 2 or less.
 * Revolt — Destroy that creature if it has mana value 4 or less instead if a permanent left the
 * battlefield under your control this turn.
 *
 * Any creature is a legal target; its mana value is checked only as the spell resolves (2020-08-07
 * ruling), so the target is unrestricted and the mana value is a resolution-time condition.
 * Revolt is [Conditions.YouHadPermanentLeaveBattlefieldThisTurn] (tokens count, and why the
 * permanent left does not matter).
 */
val FatalPush = card("Fatal Push") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target creature if it has mana value 2 or less.\n" +
        "Revolt — Destroy that creature if it has mana value 4 or less instead if a permanent left the battlefield under your control this turn."

    spell {
        val creature = target("target creature", TargetCreature())
        effect = ConditionalEffect(
            condition = Conditions.Any(
                Conditions.TargetMatchesFilter(GameObjectFilter.Creature.manaValueAtMost(2)),
                Conditions.All(
                    Conditions.YouHadPermanentLeaveBattlefieldThisTurn,
                    Conditions.TargetMatchesFilter(GameObjectFilter.Creature.manaValueAtMost(4)),
                ),
            ),
            effect = Effects.Destroy(creature),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "57"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5e81649-9954-424c-89d1-f87d73b66047.jpg?1783936764"
        ruling("2020-08-07", "Fatal Push can target any creature, even one with mana value 5 or greater. The creature's mana value is checked only as Fatal Push resolves.")
        ruling("2020-08-07", "If a creature on the battlefield has {X} in its mana cost, X is considered to be 0.")
        ruling("2020-08-07", "Revolt abilities don't care why the permanent left the battlefield, who caused it to move, or where it moved to.")
        ruling("2020-08-07", "Tokens that leave the battlefield will satisfy a revolt ability.")
    }
}
