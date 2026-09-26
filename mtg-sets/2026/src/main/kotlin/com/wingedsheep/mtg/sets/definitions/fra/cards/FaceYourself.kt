package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Targets

/**
 * Face Yourself — Reality Fracture #83
 * {5}{R}{R} · Sorcery
 *
 * For each creature target player controls, create a token that's a copy of that creature, except
 * it has haste and "At the beginning of the end step, if you don't control a planeswalker,
 * sacrifice this creature."
 *
 * The Second Harvest / Multiversal Incursion shape aimed at a chosen player:
 * [Effects.ForEachInGroup] snapshots "creatures target player controls" once at resolution
 * (CR 611.2c), and [EffectTarget.IterationEntity] is the creature being iterated. Every token is created under
 * the caster's control — the target player only chooses whose board is mirrored, so targeting
 * yourself copies your own creatures.
 *
 * Both exceptions are copiable values of the token (CR 707.9b): haste via `addedKeywords`, and the
 * end-step trigger via `triggeredAbilities`, which the token keeps for as long as it exists. The
 * trigger fires at the beginning of *every* end step, not just its controller's, and "if you don't
 * control a planeswalker" is an intervening-if (CR 603.4) — checked both as the step begins and
 * again on resolution — read from the token controller's point of view.
 */
val FaceYourself = card("Face Yourself") {
    manaCost = "{5}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "For each creature target player controls, create a token that's a copy of that " +
        "creature, except it has haste and \"At the beginning of the end step, if you don't control " +
        "a planeswalker, sacrifice this creature.\""

    val sacrificeUnlessPlaneswalker = TriggeredAbility.create(
        trigger = Triggers.anyPlayer.beginningOf(Step.END),
        effect = Effects.SacrificeTarget(EffectTarget.Self),
        interveningIf = Conditions.YouControl(GameObjectFilter.Planeswalker, negate = true),
    )

    spell {
        target(Targets.Player)
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Creature.targetPlayerControls()),
            effect = Effects.CreateTokenCopyOfTarget(
                target = EffectTarget.IterationEntity,
                addedKeywords = setOf(Keyword.HASTE),
                triggeredAbilities = listOf(sacrificeUnlessPlaneswalker),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "83"
        artist = "Ekaterina Burmak"
        flavorText = "\"I only wish you could see the necessity of this. Your counterparts all have.\"\n" +
            "—The Theorist, Jace Beleren"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3ccf8f64-19bd-4fdf-b70a-30a042bacf2f.jpg?1789127528"
        inBooster = false
    }
}
