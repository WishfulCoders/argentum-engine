package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Raffine, Scheming Seer (SNC #213)
 * {W}{U}{B}  Legendary Creature — Sphinx Demon  1/4
 *
 * Flying, ward {1}
 * Whenever you attack, target attacking creature connives X, where X is the number of attacking
 * creatures.
 *
 * Modeling: X counts every attacking creature as the trigger resolves (as Mishra, Claimed by Gix
 * does), not as attackers were declared. Connive X (CR 701.50d) draws X, discards X, and puts a
 * +1/+1 counter on the target for each nonland card discarded.
 */
val RaffineSchemingSeer = card("Raffine, Scheming Seer") {
    manaCost = "{W}{U}{B}"
    colorIdentity = "WUB"
    typeLine = "Legendary Creature — Sphinx Demon"
    power = 1
    toughness = 4
    oracleText = "Flying, ward {1}\n" +
        "Whenever you attack, target attacking creature connives X, where X is the number of " +
        "attacking creatures. (Draw X cards, then discard X cards. Put a +1/+1 counter on that " +
        "creature for each nonland card discarded this way.)"

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{1}")))

    triggeredAbility {
        trigger = Triggers.you.attacks()
        val creature = target(TargetFilter.AttackingCreature)
        effect = Effects.Connive(
            creature,
            DynamicAmounts.battlefield(Player.Each, GameObjectFilter.Creature.attacking()).count()
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "213"
        artist = "Johannes Voss"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/716a44b4-f6b0-4f14-a270-6442aed3251f.jpg?1783923074"
        ruling(
            "2022-04-29",
            "Connive X is a variant of connive. If a creature connives X, its controller will draw X " +
                "cards, discard X cards, then put a number of +1/+1 counters on the conniving " +
                "permanent equal to the number of nonland cards discarded this way."
        )
    }
}
