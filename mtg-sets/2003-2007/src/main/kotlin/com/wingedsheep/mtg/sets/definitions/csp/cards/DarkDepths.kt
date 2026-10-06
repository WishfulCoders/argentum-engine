package com.wingedsheep.mtg.sets.definitions.csp.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dark Depths
 * Legendary Snow Land
 * Dark Depths enters with ten ice counters on it.
 * {3}: Remove an ice counter from Dark Depths.
 * When Dark Depths has no ice counters on it, sacrifice it. If you do, create Marit Lage, a
 * legendary 20/20 black Avatar creature token with flying and indestructible.
 *
 * The last ability is a state trigger (CR 603.8): it triggers whenever Dark Depths has no ice
 * counters, however that came about — the {3} ability, Vampire Hexmage, or a Thespian's Stage
 * copy that never had the counters. It doesn't retrigger while on the stack, and "if you do"
 * reads whether the sacrifice actually happened (a Dark Depths gone before resolution makes no
 * Marit Lage). No mana ability — Dark Depths doesn't tap for mana.
 */
val DarkDepths = card("Dark Depths") {
    colorIdentity = ""
    typeLine = "Legendary Snow Land"
    oracleText = "Dark Depths enters with ten ice counters on it.\n" +
        "{3}: Remove an ice counter from Dark Depths.\n" +
        "When Dark Depths has no ice counters on it, sacrifice it. If you do, create Marit Lage, a " +
        "legendary 20/20 black Avatar creature token with flying and indestructible."

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.ICE,
            count = 10,
            selfOnly = true,
        )
    )

    activatedAbility {
        cost = Costs.Mana("{3}")
        effect = Effects.RemoveCounters(CounterType.ICE, 1, EffectTarget.Self)
        description = "{3}: Remove an ice counter from Dark Depths."
    }

    stateTriggeredAbility {
        condition = Conditions.SourceCounterCountAtMost(CounterType.ICE, 0)
        effect = Effects.IfYouDo(
            action = SacrificeSelfEffect,
            then = Effects.CreateToken(
                count = 1,
                power = 20,
                toughness = 20,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Avatar"),
                keywords = setOf(Keyword.FLYING, Keyword.INDESTRUCTIBLE),
                name = "Marit Lage",
                legendary = true,
                imageUri = "https://cards.scryfall.io/normal/front/8/f/8f5c3863-c876-48a8-9bc8-be20cd61074c.jpg?1783943286",
            ),
            successCriterion = SuccessCriterion.PermanentsSacrificed,
        )
        description = "When Dark Depths has no ice counters on it, sacrifice it. If you do, create Marit Lage, " +
            "a legendary 20/20 black Avatar creature token with flying and indestructible."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "145"
        artist = "Stephan Martiniere"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92409c3a-fb1a-4205-9fe1-0f5affc7b21d.jpg?1783943318"

        ruling("2022-12-08", "The last ability of Dark Depths is a state trigger. It won't trigger again while the ability is on the stack, but if the ability is countered and Dark Depths is still on the battlefield with no ice counters on it, it will trigger again immediately.")
        ruling("2022-12-08", "If Dark Depths leaves the battlefield before its triggered ability resolves, you won't be able to sacrifice it, so you won't create Marit Lage.")
        ruling("2022-12-08", "Dark Depths doesn't have a mana ability. It doesn't tap for colorless mana.")
    }
}
