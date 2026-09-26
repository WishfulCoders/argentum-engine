package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.fra.cards.HallOfEchoes
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Hall of Echoes (FRA #179) — Land.
 *
 * "{T}: Add {C}.
 *  {5}: This land becomes a copy of target creature you control until end of turn. The "legend
 *  rule" doesn't apply to permanents you control this turn."
 */
class HallOfEchoesScenarioTest : ScenarioTestBase() {

    private val copyAbility = HallOfEchoes.activatedAbilities[1].id

    /** Captured before activation — becoming a copy keeps the same permanent (entity). */
    private fun TestGame.hall(): EntityId = findPermanent("Hall of Echoes")!!

    private fun TestGame.activateCopy(hall: EntityId, target: EntityId) =
        execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = hall,
                abilityId = copyAbility,
                targets = listOf(ChosenTarget.Permanent(target)),
            )
        ).also { if (it.error == null && getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay() }

    private fun builder(): ScenarioBuilder {
        var b = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Hall of Echoes")
            .withCardOnBattlefield(1, "Isamaru, Hound of Konda", summoningSickness = false)
            .withLandsOnBattlefield(1, "Plains", 6)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(5) { b = b.withCardInLibrary(1, "Plains") }
        repeat(5) { b = b.withCardInLibrary(2, "Plains") }
        return b
    }

    init {
        test("becomes a copy of the creature — no longer a land — and both legends survive") {
            val game = builder().build()
            val hall = game.hall()
            val isamaru = game.findPermanent("Isamaru, Hound of Konda")!!

            game.activateCopy(hall, isamaru).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            withClue("the land takes the creature's copiable values") {
                game.state.getEntity(hall)?.get<CardComponent>()?.name shouldBe "Isamaru, Hound of Konda"
                projected.isCreature(hall) shouldBe true
                projected.hasType(hall, "LAND") shouldBe false
                projected.isLegendary(hall) shouldBe true
                projected.getPower(hall) shouldBe 2
                projected.getToughness(hall) shouldBe 2
            }
            withClue("the legend rule doesn't apply this turn: no choice, both stay") {
                game.hasPendingDecision() shouldBe false
                game.state.getBattlefield().contains(hall) shouldBe true
                game.state.getBattlefield().contains(isamaru) shouldBe true
            }
            withClue("as a copy it has lost its own abilities") {
                game.getLegalActions(1).none { la ->
                    (la.action as? ActivateAbility)?.sourceId == hall
                } shouldBe true
            }
        }

        test("the exemption covers other permanents you control for the rest of the turn") {
            val game = builder()
                .withCardInHand(1, "Isamaru, Hound of Konda")
                .withLandsOnBattlefield(1, "Plains", 1)
                .build()
            val hall = game.hall()
            val isamaru = game.findPermanent("Isamaru, Hound of Konda")!!

            game.activateCopy(hall, isamaru).error shouldBe null
            game.resolveStack()

            game.castSpell(1, "Isamaru, Hound of Konda").error shouldBe null
            game.resolveStack()

            withClue("three same-named legends under one controller, and no legend-rule prompt") {
                game.hasPendingDecision() shouldBe false
                game.findAllPermanents("Isamaru, Hound of Konda").size shouldBe 3
            }
        }

        test("at end of turn it reverts to Hall of Echoes and the exemption expires") {
            val game = builder().build()
            val hall = game.hall()
            val isamaru = game.findPermanent("Isamaru, Hound of Konda")!!

            game.activateCopy(hall, isamaru).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

            val projected = game.state.projectedState
            withClue("the copy effect has ended") {
                game.state.getEntity(hall)?.get<CardComponent>()?.name shouldBe "Hall of Echoes"
                projected.hasType(hall, "LAND") shouldBe true
                projected.isCreature(hall) shouldBe false
            }
            game.state.grantedStaticAbilities.none {
                it.ability is com.wingedsheep.sdk.scripting.LegendRuleDoesNotApplyTo
            } shouldBe true
        }

        test("without activating, the legend rule still applies to your legends") {
            val game = builder()
                .withCardInHand(1, "Isamaru, Hound of Konda")
                .build()

            game.castSpell(1, "Isamaru, Hound of Konda").error shouldBe null
            game.resolveStack()

            withClue("two same-named legends force the legend-rule choice") {
                game.hasPendingDecision() shouldBe true
            }
        }
    }
}
