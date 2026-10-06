package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.csp.cards.DarkDepths
import com.wingedsheep.mtg.sets.definitions.gtc.cards.ThespiansStage
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Thespian's Stage (GTC) — "{2}, {T}: This land becomes a copy of target land, except it has this
 * ability." The copy takes copiable values only, so a Stage copying Dark Depths has none of its ice
 * counters and the copied state trigger fires at once; the legend rule lets its controller keep the
 * copy, which then sacrifices itself for Marit Lage.
 */
class ThespiansStageScenarioTest : FunSpec({

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ThespiansStage, DarkDepths))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    val copyAbilityId = ThespiansStage.activatedAbilities[1].id

    fun activateCopy(driver: GameTestDriver, player: EntityId, stage: EntityId, target: EntityId) {
        driver.giveColorlessMana(player, 2)
        driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = stage,
                abilityId = copyAbilityId,
                targets = listOf(ChosenTarget.Permanent(target)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).error shouldBe null
    }

    test("copying a Forest makes it a Forest that keeps the copy ability") {
        val (driver, you) = newGame()
        val stage = driver.putLandOnBattlefield(you, "Thespian's Stage")
        val forest = driver.putLandOnBattlefield(you, "Forest")

        activateCopy(driver, you, stage, forest)
        driver.bothPass()

        driver.state.getEntity(stage)?.get<CardComponent>()?.name shouldBe "Forest"
        withClue("the Stage stays tapped — becoming a copy doesn't untap it") {
            driver.isTapped(stage) shouldBe true
        }
        driver.untapPermanent(stage)
        driver.giveColorlessMana(you, 2)
        withClue("\"except it has this ability\" — the copy ability is still activatable") {
            driver.legalActions(you).any { it.action is ActivateAbility &&
                (it.action as ActivateAbility).sourceId == stage &&
                (it.action as ActivateAbility).abilityId == copyAbilityId } shouldBe true
        }
    }

    test("copying Dark Depths: keep the counterless copy through the legend rule and get Marit Lage") {
        val (driver, you) = newGame()
        val depthsCard = driver.putCardInHand(you, "Dark Depths")
        driver.playLand(you, depthsCard).error shouldBe null
        val depths = driver.findPermanent(you, "Dark Depths")!!
        driver.state.getEntity(depths)?.get<CountersComponent>()?.counters?.get(CounterType.ICE) shouldBe 10
        val stage = driver.putLandOnBattlefield(you, "Thespian's Stage")

        activateCopy(driver, you, stage, depths)

        var guard = 0
        while (guard++ < 20 && (driver.state.stack.isNotEmpty() || driver.state.pendingDecision != null)) {
            if (driver.state.pendingDecision != null) {
                // Legend rule: two Dark Depths — keep the Stage copy (no ice counters).
                driver.submitCardSelection(you, listOf(stage)).error shouldBe null
            } else {
                driver.bothPass()
            }
        }

        withClue("the original Dark Depths went to the graveyard to the legend rule") {
            driver.getGraveyardCardNames(you).count { it == "Dark Depths" } shouldBe 1
        }
        withClue("the Stage copy sacrificed itself to its copied state trigger") {
            driver.getGraveyardCardNames(you).contains("Thespian's Stage") shouldBe true
        }
        driver.findPermanent(you, "Marit Lage").shouldNotBeNull()
    }
})
