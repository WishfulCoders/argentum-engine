package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thieving Skydiver
 * {1}{U}
 * Creature — Merfolk Rogue
 * 2/1
 * Kicker {X}. X can't be 0.
 * Flying
 * When this creature enters, if it was kicked, gain control of target artifact with mana value X
 * or less. If that artifact is an Equipment, attach it to this creature.
 *
 * - "X can't be 0" is the card's `minimumXValue = 1`: the kicked cast announces X ≥ 1 (the X
 *   picker starts at 1, a kicked cast needs at least {1} more, and the engine refuses X = 0). The
 *   unkicked cast has no X and is unaffected.
 * - The enters trigger reads the spell's X (CR 107.3m) for its "mana value X or less" target, and
 *   is an intervening-if on being kicked, so an unkicked Skydiver — or one put onto the
 *   battlefield without being cast — triggers nothing.
 * - The control change has no duration: it lasts after the Skydiver leaves (2020-09-25). The
 *   target may be an artifact you already control.
 * - The Equipment check is made on resolution, after control changes; if the Skydiver has left
 *   the battlefield by then, the Equipment stays where it is (2020-09-25).
 */
val ThievingSkydiver = card("Thieving Skydiver") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Rogue"
    power = 2
    toughness = 1
    oracleText = "Kicker {X}. X can't be 0. (You may pay an additional {X} as you cast this spell.)\n" +
        "Flying\n" +
        "When this creature enters, if it was kicked, gain control of target artifact with mana value " +
        "X or less. If that artifact is an Equipment, attach it to this creature."

    keywordAbility(KeywordAbility.kicker("{X}"))
    minimumXValue = 1
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = WasKicked
        val artifact = target(TargetFilter(GameObjectFilter.Artifact.manaValueAtMostX()))
        effect = Effects.GainControl(artifact) then
            Effects.If(
                Conditions.TargetMatchesFilter(GameObjectFilter.Artifact.withSubtype("Equipment"), artifact),
                Effects.AttachTargetEquipmentToCreature(artifact, EffectTarget.Self)
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "85"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff84ea71-e477-44f7-a3f8-77fef708efeb.jpg?1783929385"
        ruling(
            "2020-09-25",
            "Thieving Skydiver's ability can target an artifact you already control. You'll attach it to " +
                "Thieving Skydiver if it's an Equipment."
        )
        ruling(
            "2020-09-25",
            "The control-change effect of Thieving Skydiver lasts indefinitely. It doesn't wear off during the " +
                "cleanup step, and it doesn't expire if Thieving Skydiver leaves the battlefield. In a multiplayer " +
                "game, it does expire if you leave the game."
        )
        ruling(
            "2020-09-25",
            "If the Equipment can't be attached to Thieving Skydiver, most likely because Thieving Skydiver has " +
                "left the battlefield before its triggered ability resolves, the Equipment remains attached to " +
                "whatever it's currently attached to or remains unattached if attached to nothing."
        )
    }
}
