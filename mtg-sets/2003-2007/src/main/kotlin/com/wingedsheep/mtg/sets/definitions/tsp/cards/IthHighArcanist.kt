package com.wingedsheep.mtg.sets.definitions.tsp.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ith, High Arcanist
 * {5}{W}{U}
 * Legendary Creature — Human Wizard
 * 3 / 5
 * Vigilance
 * {T}: Untap target attacking creature. Prevent all combat damage that would be dealt to and dealt by that creature this turn.
 * Suspend 4—{W}{U}
 *
 * The two sentences of the activated ability are one composite over the same bound target:
 * [Effects.Untap], then [Effects.PreventCombatDamageToAndBy] — the facade for a combat-only
 * shield in both directions, which is exactly "dealt to and dealt by that creature this turn".
 */
val IthHighArcanist = card("Ith, High Arcanist") {
    manaCost = "{5}{W}{U}"
    colorIdentity = "UW"
    typeLine = "Legendary Creature — Human Wizard"
    power = 3
    toughness = 5
    oracleText = "Vigilance\n" +
        "{T}: Untap target attacking creature. Prevent all combat damage that would be dealt to and dealt by that creature this turn.\n" +
        "Suspend 4—{W}{U}"

    keywords(Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.Tap
        val t = target(TargetFilter.AttackingCreature)
        effect = Effects.Untap(t) then Effects.PreventCombatDamageToAndBy(t)
    }

    keywordAbility(KeywordAbility.suspend("{W}{U}", 4))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "241"
        artist = "Zoltan Boros & Gabor Szikszai"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8dd98dea-8012-49bb-84cf-dd8ed5c032cf.jpg"
    }
}
