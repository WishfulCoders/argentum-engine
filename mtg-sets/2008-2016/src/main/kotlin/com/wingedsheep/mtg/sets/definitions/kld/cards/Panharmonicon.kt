package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalETBOrLTBTriggers
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Panharmonicon
 * {4}
 * Artifact
 * If an artifact or creature entering causes a triggered ability of a permanent you control to
 * trigger, that ability triggers an additional time.
 *
 * The entering artifact or creature needn't be yours (per ruling) — only the permanent whose
 * ability triggers must be — so `mustBeYouControl = false`, as Elesh Norn, Mother of Machines.
 */
val Panharmonicon = card("Panharmonicon") {
    manaCost = "{4}"
    typeLine = "Artifact"
    oracleText = "If an artifact or creature entering causes a triggered ability of a permanent you control " +
        "to trigger, that ability triggers an additional time."

    staticAbility {
        ability = AdditionalETBOrLTBTriggers(
            filter = GameObjectFilter.Artifact or GameObjectFilter.Creature,
            mustBeYouControl = false,
            description = "If an artifact or creature entering causes a triggered ability of a permanent you " +
                "control to trigger, that ability triggers an additional time"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "226"
        artist = "Volkan Baǵa"
        flavorText = "All who listen hear their own song, a unique melody played just for them."
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15856326-d943-476a-9d31-898b9f990bb6.jpg?1783937152"
        ruling("2021-03-19", "You don't need to control the permanent entering the battlefield, only the permanent that has the triggered ability.")
        ruling("2021-03-19", "Replacement effects are unaffected by Panharmonicon's ability. For example, a creature that enters the battlefield with one +1/+1 counter on it won't receive an additional +1/+1 counter.")
        ruling("2021-03-19", "If you control two Panharmonicons, an artifact or creature entering the battlefield causes abilities to trigger three times, not four.")
    }
}
