package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fable of the Mirror-Breaker // Reflection of Kiki-Jiki
 * {2}{R} — Enchantment — Saga
 * // Enchantment Creature — Goblin Shaman 2/2 (red color indicator)
 *
 * Front — Fable of the Mirror-Breaker:
 *   (As this Saga enters and after your draw step, add a lore counter.)
 *   I — Create a 2/2 red Goblin Shaman creature token with "Whenever this token attacks, create a
 *     Treasure token."
 *   II — You may discard up to two cards. If you do, draw that many cards.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 *
 * Back — Reflection of Kiki-Jiki:
 *   {1}, {T}: Create a token that's a copy of another target nonlegendary creature you control,
 *   except it has haste. Sacrifice it at the beginning of the next end step.
 *
 * Chapter II is [Patterns.Hand.discardUpToThenDraw]: choosing to discard zero cards draws zero,
 * which is exactly "you may … if you do, draw that many". Chapter III is the standard
 * transforming-Saga finale ([Effects.ExileAndReturnTransformed]). The back face is Kiki-Jiki,
 * Mirror Breaker's copy ability with a {1} added to the cost and "another" on the target
 * (`excludeSelf`), so the Reflection can never copy itself.
 */
private val ReflectionOfKikiJiki = card("Reflection of Kiki-Jiki") {
    manaCost = ""
    colorIndicator = "R"
    colorIdentity = "R"
    typeLine = "Enchantment Creature — Goblin Shaman"
    power = 2
    toughness = 2
    oracleText = "{1}, {T}: Create a token that's a copy of another target nonlegendary creature you " +
        "control, except it has haste. Sacrifice it at the beginning of the next end step."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        val creature = target(
            TargetFilter(GameObjectFilter.Creature.youControl().nonlegendary(), excludeSelf = true),
        )
        effect = Effects.CreateTokenCopyOfTarget(
            target = creature,
            addedKeywords = setOf(Keyword.HASTE),
            sacrificeAtStep = Step.END,
        )
        description = "{1}, {T}: Create a token that's a copy of another target nonlegendary creature you " +
            "control, except it has haste. Sacrifice it at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "141"
        artist = "Joseph Meehan"
        flavorText = "Blessed by Keiga, the Tide Star, Kiki-Jiki would one day master the art of duplication."
        imageUri = "https://cards.scryfall.io/normal/back/2/4/24c0d87b-0049-4beb-b9cb-6f813b7aa7dc.jpg?1783923875"
    }
}

private val FableOfTheMirrorBreakerFront = card("Fable of the Mirror-Breaker") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I — Create a 2/2 red Goblin Shaman creature token with \"Whenever this token attacks, create a " +
        "Treasure token.\"\n" +
        "II — You may discard up to two cards. If you do, draw that many cards.\n" +
        "III — Exile this Saga, then return it to the battlefield transformed under your control."

    // I — Create a 2/2 red Goblin Shaman creature token with "Whenever this token attacks, create a Treasure token."
    sagaChapter(1) {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Goblin", "Shaman"),
            triggeredAbilities = listOf(
                TriggeredAbility.create(
                    trigger = Triggers.self.attacks(),
                    effect = Effects.CreateTreasure(
                        imageUri = "https://cards.scryfall.io/normal/front/6/9/6911181d-573b-41eb-96a4-799c96e008fc.jpg?1783923711",
                    ),
                ),
            ),
            imageUri = "https://cards.scryfall.io/normal/front/0/d/0d9461c3-f545-4efb-926e-759961db0495.jpg?1783923713",
        )
    }

    // II — You may discard up to two cards. If you do, draw that many cards.
    sagaChapter(2) {
        effect = Patterns.Hand.discardUpToThenDraw(2)
    }

    // III — Exile this Saga, then return it to the battlefield transformed under your control.
    sagaChapter(3) {
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "141"
        artist = "Joseph Meehan"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/24c0d87b-0049-4beb-b9cb-6f813b7aa7dc.jpg?1783923875"
        ruling("2022-02-18", "The token created by Reflection of Kiki-Jiki copies exactly what was printed on the original creature (except that the copy also has haste) and nothing else (unless it's copying a creature that's a token or that's copying something else). It doesn't copy whether the creature is tapped or untapped, whether it has any counters on it or Auras and/or Equipment attached to it, or any non-copy effects that changed its power, toughness, types, color, and so on. Most notably, if the target creature isn't normally a creature, the copy won't be a creature.")
        ruling("2022-02-18", "If Reflection of Kiki-Jiki's ability creates multiple tokens due to a replacement effect (such as the one Doubling Season creates), you'll sacrifice each of them.")
        ruling("2022-02-18", "The mana value of a transforming double-faced card is the mana value of its front face, no matter which face is up.")
    }
}

val FableOfTheMirrorBreaker: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = FableOfTheMirrorBreakerFront,
    backFace = ReflectionOfKikiJiki,
)
