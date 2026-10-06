package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.UntapSelfDuringOtherUntapSteps
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Endbringer
 * {5}{C}
 * Creature — Eldrazi
 * 5/5
 * Untap this creature during each other player's untap step.
 * {T}: This creature deals 1 damage to any target.
 * {C}, {T}: Target creature can't attack or block this turn.
 * {C}{C}, {T}: Draw a card.
 */
val Endbringer = card("Endbringer") {
    manaCost = "{5}{C}"
    colorIdentity = ""
    typeLine = "Creature — Eldrazi"
    power = 5
    toughness = 5
    oracleText = "Untap this creature during each other player's untap step.\n" +
        "{T}: This creature deals 1 damage to any target.\n" +
        "{C}, {T}: Target creature can't attack or block this turn.\n" +
        "{C}{C}, {T}: Draw a card."

    staticAbility {
        ability = UntapSelfDuringOtherUntapSteps
    }

    activatedAbility {
        cost = Costs.Tap
        val any = target(Targets.Any)
        effect = Effects.DealDamage(1, any)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{C}"), Costs.Tap)
        val t = target(TargetFilter.Creature)
        effect = Effects.CantAttackOrBlock(t)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{C}{C}"), Costs.Tap)
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "3"
        artist = "Vincent Proce"
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6bba7509-db77-414f-926f-1f28a4117831.jpg?1783937930"
        ruling("2016-01-22", "Endbringer untaps at the same time as the active player's permanents. You can't choose to not untap it at that time.")
        ruling("2016-01-22", "If an effect states that Endbringer doesn't untap during your untap step, that effect won't apply during another player's untap step.")
        ruling("2016-01-22", "Activating the second activated ability after a creature has legally been declared as an attacker or blocker won't change or undo that attack or block.")
    }
}
