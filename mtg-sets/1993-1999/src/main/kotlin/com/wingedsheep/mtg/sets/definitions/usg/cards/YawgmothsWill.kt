package com.wingedsheep.mtg.sets.definitions.usg.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Yawgmoth's Will — Urza's Saga #171
 * {2}{B} · Sorcery
 *
 * Until end of turn, you may play lands and cast spells from your graveyard.
 * If a card would be put into your graveyard from anywhere this turn, exile that card instead.
 *
 * Both halves outlive the sorcery, so both are anchored off the battlefield:
 *  - The permission is a [MayCastFromGraveyard] with `playLands = true` granted to the *player*
 *    ([EffectTarget.Controller]) until end of turn — "play lands" through the land-play special
 *    action (CR 305.1, a land drop as usual) and "cast spells" at their normal timing and cost. The
 *    filter is [GameObjectFilter.Any]: a land is never cast (CR 305.9), it is played.
 *  - The replacement (CR 614.1a) is a floating, controller-scoped [RedirectZoneChange] granted from
 *    the resolving spell (the Malicious Eclipse shape). It starts applying as the Will resolves, so
 *    the Will itself is exiled on its way to the graveyard (ruling), and it applies to costs and
 *    effects alike. "A card": a token is not a card, so tokens still die normally.
 */
val YawgmothsWill = card("Yawgmoth's Will") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Until end of turn, you may play lands and cast spells from your graveyard.\n" +
        "If a card would be put into your graveyard from anywhere this turn, exile that card instead."

    spell {
        effect = Effects.GrantStaticAbility(
            ability = MayCastFromGraveyard(filter = GameObjectFilter.Any, playLands = true),
            target = EffectTarget.Controller,
            duration = Duration.EndOfTurn,
        ) then Effects.GrantReplacementEffect(
            replacement = RedirectZoneChange(
                newDestination = Zone.EXILE,
                appliesTo = EventPattern.ZoneChangeEvent(
                    filter = GameObjectFilter.Any.nontoken().ownedByYou(),
                    to = Zone.GRAVEYARD,
                ),
            ),
            target = EffectTarget.Self,
            duration = Duration.EndOfTurn,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "171"
        artist = "Ron Spencer"
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6d3e3c3a-d351-4d91-8884-312d4b6f540d.jpg?1783946334"
        ruling("2004-10-04", "The second ability creates a replacement effect. It applies to both costs and effects.")
        ruling("2004-10-04", "If you play a card using Yawgmoth's Will and something triggers only when \"cast from your hand\", that something will not trigger. Such things trigger based on where the card came from.")
        ruling("2004-10-04", "To \"play a card\" is to either cast a spell or to put a land onto the battlefield using the main phase special action.")
        ruling("2004-10-04", "It will exile itself since it goes to the graveyard after its effect starts.")
        ruling("2004-10-04", "If you cast a Buyback spell, then there will be two effects trying to replace where the card goes. You get to choose if the Buyback returns the card to your hand or the card gets exiled.")
    }
}
