package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Construct a Cosmic Cube — Marvel Super Heroes #90
 * {2}{B} · Enchantment — Plan
 *
 * Whenever you draw your second card each turn, create a 2/1 black Villain creature token with
 * menace and put a plan counter on this enchantment.
 * When the seventh plan counter is put on this enchantment, sacrifice it. When you do, you control
 * target opponent during their next turn.
 *
 * Modeling notes:
 *  - "Whenever you draw your second card each turn" is `Triggers.<player>.drawsNth(n)``(2)`, which tracks
 *    the per-turn draw count and fires exactly once — including when a single multi-card draw
 *    crosses the threshold.
 *  - "When the **seventh** plan counter is put on this enchantment" composes from existing
 *    vocabulary: a SELF-bound `Triggers.<subject>.getsCounters(type, by, firstTimeEachTurn, batch)` on [CounterType.PLAN] gated by
 *    `triggerRestriction = `[Conditions.SourceCounterCountAtLeast]`(PLAN, 7)`. The at-least gate is
 *    behaviourally exact here because the payoff **sacrifices its own source**, so the enchantment
 *    is gone before an eighth counter could ever land — the threshold can never fire twice. No
 *    dedicated "Nth counter" trigger event is needed.
 *  - "Sacrifice it. When you do, …" is a mandatory [ReflexiveTriggerEffect] (`optional = false`);
 *    the Mindslaver-style takeover is [Effects.HijackNextTurn] on the reflexive ability's target
 *    opponent, chosen as that second stack object is put on the stack (CR 603.12).
 */
val ConstructACosmicCube = card("Construct a Cosmic Cube") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Plan"
    oracleText = "Whenever you draw your second card each turn, create a 2/1 black Villain " +
        "creature token with menace and put a plan counter on this enchantment.\n" +
        "When the seventh plan counter is put on this enchantment, sacrifice it. When you do, " +
        "you control target opponent during their next turn. (You see all cards that player " +
        "could see and make all decisions for them.)"

    triggeredAbility {
        trigger = Triggers.you.drawsNth(2)
        effect = Effects.CreateToken(
            power = 2,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf(Subtype.VILLAIN.value),
            keywords = setOf(Keyword.MENACE),
            imageUri = "https://cards.scryfall.io/normal/front/4/a/4a51b6a0-9a54-4f01-b959-0a28c15d103f.jpg?1783902804",
        ) then
            Effects.AddCounters(CounterType.PLAN, 1, EffectTarget.Self)
        description = "Whenever you draw your second card each turn, create a 2/1 black Villain " +
            "creature token with menace and put a plan counter on this enchantment."
    }

    triggeredAbility {
        trigger = Triggers.self.getsCounters(CounterType.PLAN)
        triggerRestriction = Conditions.SourceCounterCountAtLeast(CounterType.PLAN, 7)
        effect = Effects.ReflexiveTrigger(
            action = Effects.SacrificeTarget(EffectTarget.Self),
            optional = false,
            descriptionOverride = "Sacrifice this enchantment. When you do, you control target " +
                "opponent during their next turn.",
        ) {
            val opponent = target(Targets.Opponent)
            effect = Effects.HijackNextTurn(opponent)
        }
        description = "When the seventh plan counter is put on this enchantment, sacrifice it. " +
            "When you do, you control target opponent during their next turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "90"
        artist = "Eugene Maslovski"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/448de757-ac16-4529-b851-1a1331b821a5.jpg?1783902946"
    }
}
