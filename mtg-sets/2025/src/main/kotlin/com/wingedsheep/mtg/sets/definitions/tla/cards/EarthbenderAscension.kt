package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Earthbender Ascension
 * {2}{G}
 * Enchantment
 *
 * When this enchantment enters, earthbend 2. Then search your library for a basic land card,
 * put it onto the battlefield tapped, then shuffle.
 * Landfall — Whenever a land you control enters, put a quest counter on this enchantment. When
 * you do, if it has four or more quest counters on it, put a +1/+1 counter on target creature
 * you control. It gains trample until end of turn.
 *
 * Modeling notes:
 *  - Earthbend is a keyword *action* composed from primitives via [Effects.Earthbend] (animate the
 *    target land, add counters, grant the two return-self triggers) — same as Earthbending Lesson /
 *    Badgermole Cub. Its land is a true target, chosen as the ETB ability goes on the stack.
 *  - The basic-land tutor is the standard `Patterns.Library.searchLibrary` pipeline
 *    (`ChooseUpTo(1)` models the optional "search for a basic land card"), enters tapped, then
 *    shuffles — matching Unlucky Cabbage Merchant.
 *  - The landfall payoff is an intervening-"if" (CR 603.4): putting the quest counter is mandatory,
 *    and only if the enchantment then has four or more quest counters does the boost happen. The
 *    counter add is sequenced first, then [Effects.If] gates the payoff on the live count
 *    (`SourceCounterCountAtLeast`) so no creature is chosen at all when below the threshold. The
 *    creature is picked at resolution via [SelectTargetEffect] (rather than a reflexive target
 *    requirement) precisely so the choice only happens inside the satisfied gate.
 */
val EarthbenderAscension = card("Earthbender Ascension") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, earthbend 2. Then search your library for a basic land card, put it onto the battlefield tapped, then shuffle.\n" +
        "Landfall — Whenever a land you control enters, put a quest counter on this enchantment. When you do, if it has four or more quest counters on it, put a +1/+1 counter on target creature you control. It gains trample until end of turn."

    // When this enchantment enters, earthbend 2. Then search your library for a basic land card,
    // put it onto the battlefield tapped, then shuffle.
    triggeredAbility {
        trigger = Triggers.self.enters()
        val land = target(TargetFilter.Land.youControl())
        effect = Effects.Earthbend(2, land) then
            Patterns.Library.searchLibrary(
                filter = Filters.BasicLand,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true,
                shuffleAfter = true
            )
        description = "When this enchantment enters, earthbend 2. Then search your library for a basic land card, put it onto the battlefield tapped, then shuffle."
    }

    // Landfall — Whenever a land you control enters, put a quest counter on this enchantment. When
    // you do, if it has four or more quest counters on it, put a +1/+1 counter on target creature
    // you control. It gains trample until end of turn.
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.AddCounters(CounterType.QUEST, 1, EffectTarget.Self) then
            Effects.If(
                condition = Conditions.SourceCounterCountAtLeast(CounterType.QUEST, 4),
                then = Effects.Pipeline {
                    val boostedCreature = selectTarget(TargetObject(filter = TargetFilter.CreatureYouControl))
                    run(Effects.AddCounters(
                        CounterType.PLUS_ONE_PLUS_ONE,
                        1,
                        boostedCreature.asTarget
                    ))
                    run(Effects.GrantKeyword(
                        Keyword.TRAMPLE,
                        boostedCreature.asTarget,
                        Duration.EndOfTurn
                    ))
                }
            )
        description = "Landfall — Whenever a land you control enters, put a quest counter on this enchantment. When you do, if it has four or more quest counters on it, put a +1/+1 counter on target creature you control. It gains trample until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "175"
        artist = "Logan Feliciano"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/590a58ab-5e98-4031-8aa6-ce396dc1429f.jpg?1764121189"
    }
}
