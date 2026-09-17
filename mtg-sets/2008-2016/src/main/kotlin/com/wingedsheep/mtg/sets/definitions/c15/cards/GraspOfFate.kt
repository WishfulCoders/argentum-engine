package com.wingedsheep.mtg.sets.definitions.c15.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Grasp of Fate
 * {1}{W}{W}
 * Enchantment
 * When this enchantment enters, for each opponent, exile up to one target nonland permanent that
 * player controls until this enchantment leaves the battlefield. (Those permanents return under
 * their owners' control.)
 *
 * "Until this enchantment leaves" is a duration (CR 610.3), not Oblivion Ring's second trigger:
 * if Grasp of Fate has already left when the trigger resolves, nothing is exiled (2015-11-04
 * ruling), and the permanents come back the moment it leaves, with nothing in between. That is
 * `MoveUntilSourceLeaves`, which checks the source is still on the battlefield. "For each
 * opponent" is one optional target an opponent controls, as for Omega, Heartless Evolution: the
 * engine plays two-player games.
 */
val GraspOfFate = card("Grasp of Fate") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, for each opponent, exile up to one target nonland permanent " +
        "that player controls until this enchantment leaves the battlefield. (Those permanents return under " +
        "their owners' control.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val exiled = target(
            "up to one target nonland permanent that player controls",
            TargetPermanent(optional = true, filter = TargetFilter.NonlandPermanentOpponentControls),
        )
        effect = Effects.MoveUntilSourceLeaves(exiled, Zone.EXILE)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "3"
        artist = "Tomasz Jedruszek"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42e1e595-4000-4c89-a2cd-dbaec25baa71.jpg?1783938117"
        ruling("2015-11-04", "If Grasp of Fate leaves the battlefield before its triggered ability resolves, no nonland permanents will be exiled.")
        ruling("2015-11-04", "If a token is exiled, it ceases to exist. It won't be returned to the battlefield.")
        ruling("2015-11-04", "The exiled cards return to the battlefield immediately after Grasp of Fate leaves the battlefield. Nothing happens between the two events, including state-based actions.")
    }
}
