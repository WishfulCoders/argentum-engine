package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.core.Step

/**
 * Dance of Many
 * {U}{U}
 * Enchantment
 * When this enchantment enters, create a token that's a copy of target nontoken creature.
 * When this enchantment leaves the battlefield, exile the token.
 * When the token leaves the battlefield, sacrifice this enchantment.
 * At the beginning of your upkeep, sacrifice this enchantment unless you pay {U}{U}.
 *
 * Four clauses, three of which are only meaningful because they all point at *the* token — the one
 * this enchantment made, not any copy on the battlefield. That linkage is the token-provenance
 * stamp: the ETB clause mints its copy with `stampCreator = true`, and the two leave-triggers find
 * it again through `GameObjectFilter.Any.createdBySource()`, the same shape Tetravus uses to
 * reabsorb its own Tetravites. Without it, a second Dance of Many would exile the first one's token.
 *
 * `stampCreator` is new on the *copy* path; it already existed for plain token creation. The two
 * are the same provenance record, and the reason to want it is the same: several sources can mint
 * indistinguishable tokens.
 *
 * The token-leaves trigger has to be ANY-bound with a filter rather than SELF-bound, because the
 * permanent that leaves is the token, not the enchantment. The enchantment-leaves trigger is
 * ordinary SELF: `createdBySource()` still resolves against it by last-known information after it
 * has gone.
 */
val DanceOfMany = card("Dance of Many") {
    manaCost = "{U}{U}"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, create a token that's a copy of target nontoken " +
        "creature.\nWhen this enchantment leaves the battlefield, exile the token.\nWhen the " +
        "token leaves the battlefield, sacrifice this enchantment.\nAt the beginning of your " +
        "upkeep, sacrifice this enchantment unless you pay {U}{U}."

    triggeredAbility {
        val creature = target(TargetFilter(GameObjectFilter.Creature.nontoken()))
        trigger = Triggers.self.enters()
        effect = Effects.CreateTokenCopyOfTarget(
            target = creature,
            stampCreator = true,
        )
        description = "When this enchantment enters, create a token that's a copy of target nontoken creature."
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.Pipeline {
            val danceToken = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Any.createdBySource(),
                    player = Player.Each,
                )
            )
            move(danceToken, CardDestination.ToZone(Zone.EXILE), moveType = MoveType.Default)
        }
        description = "When this enchantment leaves the battlefield, exile the token."
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Any.createdBySource()).leaves()
        effect = SacrificeSelfEffect
        description = "When the token leaves the battlefield, sacrifice this enchantment."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.Mana("{U}{U}"),
            suffer = SacrificeSelfEffect,
        )
        description = "At the beginning of your upkeep, sacrifice this enchantment unless you pay {U}{U}."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "22"
        artist = "Sandra Everingham"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13453abe-3f05-4956-8493-382d7d2af699.jpg?1783947944"
    }
}
