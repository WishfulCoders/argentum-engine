package com.wingedsheep.mtg.sets.definitions.c18.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Loyal Apprentice — Commander 2018 #23
 * {1}{R} · Creature — Human Artificer · 2/1
 * Haste
 * Lieutenant — At the beginning of combat on your turn, if you control your commander, create a
 * 1/1 colorless Thopter artifact creature token with flying. That token gains haste until end of turn.
 *
 * "Lieutenant" is an ability word. "Your commander" is a commander *you* own (CR 903.3 — the
 * commander's owner is the player who designated it), so the intervening-if (CR 603.4) is
 * [Conditions.YouControl] over a battlefield commander that is also owned by you: a stolen
 * opponent's commander doesn't count, and with partners controlling either one is enough
 * (ruling). The check is made both when the trigger would fire and at resolution.
 *
 * "That token" is [EffectTarget.PipelineTarget] over [CREATED_TOKENS]; haste is *granted* for
 * [Duration.EndOfTurn] rather than printed on the token, since the Thopter outlives the turn.
 */
val LoyalApprentice = card("Loyal Apprentice") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Artificer"
    power = 2
    toughness = 1
    oracleText = "Haste\n" +
        "Lieutenant — At the beginning of combat on your turn, if you control your commander, " +
        "create a 1/1 colorless Thopter artifact creature token with flying. That token gains " +
        "haste until end of turn."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        interveningIf = Conditions.YouControl(GameObjectFilter.Permanent.commander().ownedByYou())
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true,
        ) then Effects.GrantKeyword(
            Keyword.HASTE,
            EffectTarget.PipelineTarget(CREATED_TOKENS, 0),
            Duration.EndOfTurn,
        )
        description = "Lieutenant — At the beginning of combat on your turn, if you control your " +
            "commander, create a 1/1 colorless Thopter artifact creature token with flying. That " +
            "token gains haste until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "23"
        artist = "Joe Slucher"
        flavorText = "Those with knowledge are wise to listen to those with imagination."
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5fef5fe-3e59-4fd6-8a88-7c1cee56892f.jpg?1783934336"
        ruling("2018-07-13", "If you don't control your commander as the lieutenant ability resolves, you won't get its effect.")
        ruling("2018-07-13", "The lieutenant effect happens only once each combat, even if you somehow control multiple commanders (perhaps because you have two commanders with a partner ability from the Battlebond™ set).")
        ruling("2018-07-13", "If you have multiple commanders, you need to control only one for the lieutenant effect to happen.")
    }
}
