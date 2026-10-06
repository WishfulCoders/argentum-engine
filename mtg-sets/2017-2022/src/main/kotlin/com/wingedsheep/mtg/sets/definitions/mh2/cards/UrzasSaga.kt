package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Urza's Saga — Modern Horizons 2 #259
 * Enchantment Land — Urza's Saga (no mana cost)
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — This Saga gains "{T}: Add {C}."
 * II — This Saga gains "{2}, {T}: Create a 0/0 colorless Construct artifact creature token with
 *      'This token gets +1/+1 for each artifact you control.'"
 * III — Search your library for an artifact card with mana cost {0} or {1}, put it onto the
 *       battlefield, then shuffle.
 *
 * A Saga that is a land: it can only be *played* (CR 305.9), and playing it picks up the Saga's
 * intrinsic "enters with a lore counter" (CR 714.3a) through `PlayLandHandler`'s Saga-entry hook, so
 * chapter I triggers as it is played (CR 714.2b). Chapters I and II hand the Saga itself a lasting
 * activated ability ([Duration.Permanent] — it ends when the Saga leaves, at the latest the CR 714.4
 * sacrifice after chapter III). Chapter I's grant is a mana ability (CR 605.1a), so the auto-payer
 * sees it like a printed one.
 *
 * Chapter III reads the printed **mana cost**, not mana value (ruling 2021-06-18):
 * [com.wingedsheep.sdk.scripting.predicates.CardPredicate.ManaCostIs] via `withManaCost("{0}", "{1}")`
 * finds Ornithopter's `{0}` and Sol Ring's `{1}` but not a `{U}` or `{X}` artifact, nor one with no
 * mana cost at all (CR 202.1b).
 *
 * The "Urza's" land subtype is carried for type-line fidelity only — it does not interact with Urza's
 * Tower, Mine or Power Plant (ruling), which look for those names rather than the subtype.
 */
val UrzasSaga = card("Urza's Saga") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Enchantment Land — Urza's Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — This Saga gains \"{T}: Add {C}.\"\n" +
        "II — This Saga gains \"{2}, {T}: Create a 0/0 colorless Construct artifact creature token with " +
        "'This token gets +1/+1 for each artifact you control.'\"\n" +
        "III — Search your library for an artifact card with mana cost {0} or {1}, put it onto the " +
        "battlefield, then shuffle."

    // I — This Saga gains "{T}: Add {C}."
    sagaChapter(1) {
        effect = Effects.GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Tap,
                effect = Effects.AddColorlessMana(1),
                isManaAbility = true,
                timing = TimingRule.ManaAbility,
            ),
            target = EffectTarget.Self,
            duration = Duration.Permanent,
        )
    }

    // II — This Saga gains "{2}, {T}: Create a 0/0 colorless Construct artifact creature token
    // with 'This token gets +1/+1 for each artifact you control.'"
    sagaChapter(2) {
        effect = Effects.GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap),
                effect = Effects.CreateToken(
                    power = 0,
                    toughness = 0,
                    colors = emptySet(),
                    creatureTypes = setOf("Construct"),
                    artifactToken = true,
                    staticAbilities = listOf(
                        GrantDynamicStats(
                            filter = GroupFilter.source(),
                            powerBonus = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count(),
                            toughnessBonus = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count(),
                        )
                    ),
                    imageUri = "https://cards.scryfall.io/normal/front/a/7/a7caaf39-8f16-4f1d-bee6-a45674306319.jpg?1783926587",
                ),
            ),
            target = EffectTarget.Self,
            duration = Duration.Permanent,
        )
    }

    // III — Search your library for an artifact card with mana cost {0} or {1}, put it onto the
    // battlefield, then shuffle.
    sagaChapter(3) {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Artifact.withManaCost("{0}", "{1}"),
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "259"
        artist = "Titus Lunter"
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c1e0f201-42cb-46a1-901a-65bb4fc18f6c.jpg?1783926791"
        ruling("2021-06-18", "Urza's Saga is a land, so it can only be played as a land. It cannot be cast as a spell.")
        ruling("2021-06-18", "Urza's Saga gains an ability from its first and second chapters. It keeps those abilities for as long as it's on the battlefield.")
        ruling("2021-06-18", "While resolving the chapter III ability, you can find only a card with actual mana cost {0} or {1}, not mana value 0 or 1. For example, you couldn't find a card with mana cost {U} or one with mana cost {X}.")
        ruling("2021-06-18", "Even though Urza's Saga is a land, it is also still a Saga, and it will be sacrificed after its last chapter ability resolves.")
        ruling("2021-06-18", "Although Urza's Saga has the Urza's land type, it doesn't interact with Urza's Tower, Urza's Mine, or Urza's Power Plant.")
    }
}
