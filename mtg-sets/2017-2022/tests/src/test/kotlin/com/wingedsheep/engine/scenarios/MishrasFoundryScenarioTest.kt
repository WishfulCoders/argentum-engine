package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bro.cards.MishrasFoundry
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Mishra's Foundry (BRO #265).
 *
 *  {T}: Add {C}.
 *  {2}: This land becomes a 2/2 Assembly-Worker artifact creature until end of turn. It's still a land.
 *  {1}, {T}: Target attacking Assembly-Worker gets +2/+2 until end of turn.
 */
class MishrasFoundryScenarioTest : FunSpec({

    val animateAbilityId = MishrasFoundry.activatedAbilities[1].id
    val pumpAbilityId = MishrasFoundry.activatedAbilities[2].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        return driver
    }

    test("{2} animates it into a 2/2 Assembly-Worker artifact creature land") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val foundry = driver.putLandOnBattlefield(player, "Mishra's Foundry")
        driver.giveColorlessMana(player, 2)
        driver.submit(
            ActivateAbility(playerId = player, sourceId = foundry, abilityId = animateAbilityId)
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        val projected = driver.state.projectedState
        projected.hasType(foundry, "CREATURE") shouldBe true
        projected.hasType(foundry, "ARTIFACT") shouldBe true
        projected.hasType(foundry, "LAND") shouldBe true
        projected.hasSubtype(foundry, "Assembly-Worker") shouldBe true
        projected.getPower(foundry) shouldBe 2
        projected.getToughness(foundry) shouldBe 2
    }

    test("the pump can't target an Assembly-Worker that isn't attacking") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val worker = driver.putLandOnBattlefield(player, "Mishra's Foundry")
        val pumper = driver.putLandOnBattlefield(player, "Mishra's Foundry")
        driver.giveColorlessMana(player, 2)
        driver.submit(
            ActivateAbility(playerId = player, sourceId = worker, abilityId = animateAbilityId)
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.giveColorlessMana(player, 1)
        driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = pumper,
                abilityId = pumpAbilityId,
                targets = listOf(ChosenTarget.Permanent(worker)),
            )
        ).outcome shouldNotBe Outcome.Done
    }

    test("{1}, {T}: an attacking Assembly-Worker gets +2/+2") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val worker = driver.putLandOnBattlefield(player, "Mishra's Foundry")
        val pumper = driver.putLandOnBattlefield(player, "Mishra's Foundry")
        driver.removeSummoningSickness(worker)
        driver.removeSummoningSickness(pumper)
        driver.giveColorlessMana(player, 2)
        driver.submit(
            ActivateAbility(playerId = player, sourceId = worker, abilityId = animateAbilityId)
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(worker), opponent).error shouldBe null

        driver.giveColorlessMana(player, 1)
        driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = pumper,
                abilityId = pumpAbilityId,
                targets = listOf(ChosenTarget.Permanent(worker)),
            )
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.state.projectedState.getPower(worker) shouldBe 4
        driver.state.projectedState.getToughness(worker) shouldBe 4
    }
})
