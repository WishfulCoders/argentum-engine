package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.DynamicAmounts

/**
 * Bronze Tablet
 * {6}
 * Artifact
 * Remove this card from your deck before playing if you're not playing for ante.
 * This artifact enters tapped.
 * {4}, {T}: Exile this artifact and target nontoken permanent an opponent owns. That player may
 * pay 10 life. If they do, put this card into its owner's graveyard. Otherwise, that player owns
 * this card and you own the other exiled card.
 *
 * Only legal in a game played for ante (CR 407.3, enforced by deck validation). Per its rulings:
 * - A Tablet that left the battlefield before resolution isn't exiled (`fromZone`), but the
 *   opponent still chooses, and if they don't pay, ownership still swaps.
 * - They can't pay 10 life with less than 10 (CR 119.4) — `Gate.MayPay` skips the offer then.
 * - A target gone by resolution makes the ability fizzle, so nothing happens.
 * The swap reads the target's owner *before* the target changes hands, so "that player" is
 * always the opponent who owned it.
 */
val BronzeTablet = card("Bronze Tablet") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Remove this card from your deck before playing if you're not playing for ante.\n" +
        "This artifact enters tapped.\n" +
        "{4}, {T}: Exile this artifact and target nontoken permanent an opponent owns. That player " +
        "may pay 10 life. If they do, put this card into its owner's graveyard. Otherwise, that " +
        "player owns this card and you own the other exiled card."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        val stake = target(TargetFilter(GameObjectFilter.Permanent.nontoken().ownedByOpponent()))
        val thatPlayer = Player.OwnerOf("target permanent")
        effect = Effects.Move(EffectTarget.Self, Zone.EXILE, fromZone = Zone.BATTLEFIELD) then
            Effects.Exile(stake) then
            Effects.MayPay(
                cost = Effects.PayDynamicLife(DynamicAmounts.fixed(10), thatPlayer),
                then = Effects.Move(EffectTarget.Self, Zone.GRAVEYARD, fromZone = Zone.EXILE),
                otherwise = Effects.ChangeOwnership(EffectTarget.Self, EffectTarget.PlayerRef(thatPlayer)) then
                    Effects.ChangeOwnership(stake),
                decisionMaker = EffectTarget.PlayerRef(thatPlayer),
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "42"
        artist = "Tom Wänerstrand"
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6fb10552-dd47-4f8a-ac7c-8c2b61e56736.jpg?1783948364"
    }
}
