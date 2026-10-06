package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.stack.CopyTargetSpellExecutor
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.CopyTargetSpellEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class SpellCopyTargetDecisionTest : FunSpec({
    test("copy slots are optimized together so two improvements cannot pick the same target") {
        val blast = card("Test Divided Copy Blast") {
            manaCost = "{0}"
            typeLine = "Instant"
            spell {
                target(TargetObject(filter = TargetFilter.Creature, count = 2))
                effect = Effects.DividedDamage(total = 4, minTargets = 2, maxTargets = 2)
            }
        }
        val d = GameTestDriver()
        d.registerCards(TestCards.all + blast)
        d.initMirrorMatch(Deck.of("Island" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val a = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val b = d.putPermanentOnBattlefield(d.player1, "Hill Giant")
        d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val spell = d.putCardInHand(d.player1, blast.name)
        d.submit(CastSpell(d.player1, spell,
            targets = listOf(ChosenTarget.Permanent(a), ChosenTarget.Permanent(b)),
            damageDistribution = mapOf(a to 2, b to 2), paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        val predicates = PredicateEvaluator(cardRegistry = d.cardRegistry)
        val result = CopyTargetSpellExecutor(predicates.amounts, TargetFinder(predicates)).execute(
            d.state, CopyTargetSpellEffect(EffectTarget.ContextTarget(0)),
            EffectContext(controllerId = d.player1, sourceId = null, targets = listOf(ChosenTarget.Spell(spell))))
        result.error shouldBe null
        d.replaceState(result.state)
        val question = d.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        question.targetRequirements.size shouldBe 2
        val responder = DecisionResponder(GameSimulator(d.cardRegistry), AIPlayer.defaultEvaluator())
        val response = responder.respond(d.state, question, d.player1)
        d.submit(SubmitDecision(d.player1, response)).error shouldBe null
    }
})
