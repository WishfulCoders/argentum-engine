package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.SELF_NOUN_TOKEN
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The generated "You may …" text a consent gate shows as its yes/no prompt. Each case is a shape that
 * reached real players broken: "You may you gain that many life", "You may this creature becomes …",
 * "You may pay 2 life. If you do, . Otherwise, tap this creature" (on a land).
 */
class GatedEffectDescriptionTest : FunSpec({

    test("a clause that already says 'you' drops it after 'you may'") {
        Effects.May(Effects.GainLife(3)).description shouldBe "You may gain 3 life"
    }

    test("another subject takes 'have' and the verb's base form") {
        Effects.May(Effects.ModifyStats(2, 0, EffectTarget.ContextTarget(0))).description shouldBe
            "You may have target get +2/+0 until end of turn"
        Effects.May(Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent))).description shouldBe
            "You may have each opponent lose 1 life"
    }

    test("an imperative clause is left as written") {
        Effects.May(Effects.DrawCards(1)).description shouldBe "You may draw a card"
    }

    test("a pay gate with no payoff says only what declining does") {
        val gate = Effects.MayPay(Effects.PayLife(2), then = Effects.Nothing, otherwise = Effects.Tap(EffectTarget.Self))
        gate.description shouldBe "You may pay 2 life. If you don't, tap this permanent"
        gate.descriptionTemplate shouldBe "You may pay 2 life. If you don't, tap $SELF_NOUN_TOKEN"
    }

    test("a pay gate with a payoff keeps 'If you do' and 'Otherwise'") {
        Effects.MayPay(Effects.PayLife(2), then = Effects.DrawCards(1), otherwise = Effects.Tap(EffectTarget.Self))
            .description shouldBe "You may pay 2 life. If you do, draw a card. Otherwise, tap this permanent"
    }

    test("tapping the source carries the self-noun placeholder") {
        (Effects.Tap(EffectTarget.Self) as TapUntapEffect).descriptionTemplate shouldBe "Tap $SELF_NOUN_TOKEN"
        Effects.Tap(EffectTarget.ContextTarget(0)).description shouldBe "Tap target"
    }
})
