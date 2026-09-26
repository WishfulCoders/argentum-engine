package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Feature test for the **double strike keyword counter** (CR 122.1b / 702.4d).
 *
 * `CounterType.DOUBLE_STRIKE` is mapped to `Keyword.DOUBLE_STRIKE` in `StateProjector.KEYWORD_COUNTER_MAP`,
 * so a permanent carrying one or more double strike counters projects the Double Strike keyword — exactly
 * like the existing first strike / vigilance keyword counters. Backs Mai, Jaded Edge's exhaust ability.
 */
class DoubleStrikeCounterScenarioTest : ScenarioTestBase() {

    // A {0} sorcery that puts a double strike counter on target creature.
    private val markDoubleStrike = card("Mark Double Strike") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Put a double strike counter on target creature."
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.AddCounters(CounterType.DOUBLE_STRIKE, 1, t)
        }
    }

    init {
        cardRegistry.register(markDoubleStrike)

        context("double strike counter") {

            test("a double strike counter grants the Double Strike keyword via projection") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withCardInHand(1, "Mark Double Strike")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                withClue("no double strike before the counter") {
                    game.state.projectedState.hasKeyword(giant, Keyword.DOUBLE_STRIKE) shouldBe false
                }

                game.castSpell(1, "Mark Double Strike", giant).error shouldBe null
                game.resolveStack()

                withClue("a double strike counter projects the Double Strike keyword") {
                    game.state.projectedState.hasKeyword(giant, Keyword.DOUBLE_STRIKE) shouldBe true
                }
            }
        }
    }
}
