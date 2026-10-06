package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.scripting.effects.CopyRecipient
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class NextMainPhaseAndChainManaCostSerializationTest : FunSpec({

    test("a next-main-phase delayed trigger keeps both main phases through a round trip") {
        val effect = Effects.AtBeginningOfYourNextMainPhase(Effects.AddColorlessMana(3))
        effect as CreateDelayedTriggerEffect
        effect.step shouldBe Step.PRECOMBAT_MAIN
        effect.alsoAtSteps shouldBe listOf(Step.POSTCOMBAT_MAIN)
        val encoded = CardSerialization.json.encodeToString<Effect>(effect)
        CardSerialization.json.decodeFromString<Effect>(encoded) shouldBe effect
        effect.description shouldBe "create a delayed trigger at the beginning of the next main phase"
    }

    test("a single-step delayed trigger serializes without the new field") {
        val effect = Effects.CreateDelayedTrigger(Effects.DrawCards(1), step = Step.END)
        CardSerialization.json.encodeToString<Effect>(effect) shouldNotContain "alsoAtSteps"
    }

    test("a chain copy with a mana cost round-trips and reads 'may pay {R}{R}'") {
        val t = EffectTarget.BoundVariable("t0")
        val effect = Effects.ChainCopy(
            action = Effects.DealDamage(3, t),
            target = t,
            offerTo = CopyRecipient.AFFECTED_PLAYER,
            copyTarget = Targets.Any,
            copyCost = Costs.pay.Mana("{R}{R}")
        )
        val encoded = CardSerialization.json.encodeToString<Effect>(effect)
        CardSerialization.json.decodeFromString<Effect>(encoded) shouldBe effect
        effect.description shouldContain "may pay {R}{R}. If the player does, they may copy this spell"
    }

    test("non-mana copy costs keep their own wording") {
        val t = EffectTarget.BoundVariable("t0")
        Effects.ChainCopy(
            action = Effects.DealDamage(3, t),
            target = t,
            offerTo = CopyRecipient.AFFECTED_PLAYER,
            copyTarget = Targets.Any,
            copyCost = Costs.pay.Discard()
        ).description shouldContain "may discard a card. If the player does"
    }
})
