package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The eleven keywords Super-Adaptoid can absorb, each paired with the keyword counter (CR 122.1b)
 * that grants it. Printed order: haste first, then "do the same for" the other ten.
 */
private val ADAPTABLE_KEYWORDS: List<Pair<Keyword, CounterType>> = listOf(
    Keyword.HASTE to CounterType.HASTE,
    Keyword.FLYING to CounterType.FLYING,
    Keyword.FIRST_STRIKE to CounterType.FIRST_STRIKE,
    Keyword.DOUBLE_STRIKE to CounterType.DOUBLE_STRIKE,
    Keyword.DEATHTOUCH to CounterType.DEATHTOUCH,
    Keyword.INDESTRUCTIBLE to CounterType.INDESTRUCTIBLE,
    Keyword.LIFELINK to CounterType.LIFELINK,
    Keyword.MENACE to CounterType.MENACE,
    Keyword.REACH to CounterType.REACH,
    Keyword.TRAMPLE to CounterType.TRAMPLE,
    Keyword.VIGILANCE to CounterType.VIGILANCE,
)

/**
 * "If that creature has <K> and Super-Adaptoid doesn't, put a <K> counter on Super-Adaptoid",
 * once per keyword in [ADAPTABLE_KEYWORDS].
 *
 * Both halves of each check read projected state, which is what makes the card behave: the target's
 * keyword may be granted rather than printed, and Super-Adaptoid's "doesn't" has to account for
 * counters he picked up on an earlier trigger *and* earlier in this very resolution (the composite
 * applies its parts in order against the updated state, so he never stacks two counters for one
 * keyword).
 */
private fun absorbKeywords(creature: EffectTarget): Effect = Effects.Composite(
    ADAPTABLE_KEYWORDS.map { (keyword, counter) ->
        Effects.If(
            condition = Conditions.All(
                Conditions.TargetMatchesFilter(GameObjectFilter.Any.withKeyword(keyword), creature),
                Conditions.Not(Conditions.SourceHasKeyword(keyword)),
            ),
            then = Effects.AddCounters(counter, 1, EffectTarget.Self),
        )
    }
)

/**
 * Super-Adaptoid — Marvel Super Heroes #250 (rare)
 * {2} · Legendary Artifact Creature — Robot Villain · * /2
 *
 * Super-Adaptoid's power is equal to the number of legendary creatures you control.
 * Whenever Super-Adaptoid enters or attacks, choose another target creature. If that creature has
 * haste and Super-Adaptoid doesn't, put a haste counter on Super-Adaptoid. Do the same for flying,
 * first strike, double strike, deathtouch, indestructible, lifelink, menace, reach, trample, and
 * vigilance.
 *
 * Implementation notes:
 * - Only power is characteristic-defining (toughness is a printed 2), so this is the single-stat
 *   `dynamicPower(...)` rather than the both-stats `dynamicStats(...)`. He counts himself when he's
 *   on the battlefield — he is a legendary creature — which is why he reads as a 1/2 alone.
 * - "Enters or attacks" is two triggered abilities sharing one effect, the repo's established idiom
 *   for that wording (Dread Osseosaur, Queen's Bay Paladin).
 * - The eleven keyword clauses are generated from [ADAPTABLE_KEYWORDS] instead of being written out
 *   eleven times. Each is a plain conditional counter placement — the counters themselves do the
 *   granting through `StateProjector.KEYWORD_COUNTER_MAP`, so nothing here grants a keyword
 *   directly, and the keywords stick around after the target leaves.
 * - The target is [TargetFilter.OtherCreature] ("another target creature"), so he can't feed on
 *   himself. Note it is a *target*, not a choice on resolution: if it becomes illegal the whole
 *   trigger is countered and he absorbs nothing.
 */
val SuperAdaptoid = card("Super-Adaptoid") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Legendary Artifact Creature — Robot Villain"
    toughness = 2
    dynamicPower(
        DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature.legendary()).count()
    )
    oracleText = "Super-Adaptoid's power is equal to the number of legendary creatures you " +
        "control.\n" +
        "Whenever Super-Adaptoid enters or attacks, choose another target creature. If that " +
        "creature has haste and Super-Adaptoid doesn't, put a haste counter on Super-Adaptoid. Do " +
        "the same for flying, first strike, double strike, deathtouch, indestructible, lifelink, " +
        "menace, reach, trample, and vigilance."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val otherCreature = target(TargetFilter.OtherCreature)
        effect = absorbKeywords(otherCreature)
        description = "Whenever Super-Adaptoid enters, choose another target creature. If that " +
            "creature has haste and Super-Adaptoid doesn't, put a haste counter on " +
            "Super-Adaptoid. Do the same for flying, first strike, double strike, deathtouch, " +
            "indestructible, lifelink, menace, reach, trample, and vigilance."
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val otherCreature = target(TargetFilter.OtherCreature)
        effect = absorbKeywords(otherCreature)
        description = "Whenever Super-Adaptoid attacks, choose another target creature. If that " +
            "creature has haste and Super-Adaptoid doesn't, put a haste counter on " +
            "Super-Adaptoid. Do the same for flying, first strike, double strike, deathtouch, " +
            "indestructible, lifelink, menace, reach, trample, and vigilance."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "250"
        artist = "John Tyler Christopher"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fbbd8609-5a00-4188-96fe-77251579b88d.jpg?1783902890"
    }
}
