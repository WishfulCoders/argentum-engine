package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Darkpact
 * {B}{B}{B}
 * Sorcery
 * Remove this card from your deck before playing if you're not playing for ante.
 * You own target card in the ante. Exchange that card with the top card of your library.
 *
 * Only legal in a game played for ante (CR 407.3, enforced by deck validation).
 * - "You own target card in the ante" is [Effects.ChangeOwnership]: the card is yours for keeps
 *   (the server carries the change out of the game).
 * - The exchange is between two zones, so both cards must have the same owner (CR 701.12d) —
 *   which the first sentence just arranged. The top card is gathered *before* the target goes
 *   on top, so the swap names the card that was on top when Darkpact resolved. With an empty
 *   library the exchange still happens: the target goes to your library and nothing comes back
 *   (701.12d, "even if one zone is empty").
 */
val Darkpact = card("Darkpact") {
    manaCost = "{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Remove this card from your deck before playing if you're not playing for ante.\n" +
        "You own target card in the ante. Exchange that card with the top card of your library."

    spell {
        val anted = target(TargetFilter(GameObjectFilter.Any, zone = Zone.ANTE))
        effect = Effects.Pipeline {
            val top = gather(CardSource.TopOfLibrary(1))
            run(Effects.ChangeOwnership(anted))
            run(Effects.Move(anted, Zone.LIBRARY, placement = ZonePlacement.Top))
            move(top, CardDestination.ToZone(Zone.ANTE))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "99"
        artist = "Quinton Hoover"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e78db688-93a2-47f5-9aa5-9158a72cd973.jpg?1783948697"
    }
}
