package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.usg.cards.SneakAttack
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Sneak Attack — "{R}: You may put a creature card from your hand onto the battlefield. That
 * creature gains haste. Sacrifice the creature at the beginning of the next end step."
 *
 * The cheat-in with haste, the cross-step delayed sacrifice, and a "may" decline that still spends
 * the activation.
 */
class SneakAttackScenarioTest : FunSpec({

    val sneakAbilityId = SneakAttack.activatedAbilities[0].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SneakAttack))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun activate(driver: GameTestDriver, player: EntityId, sneak: EntityId) {
        driver.giveMana(player, Color.RED, 1)
        driver.submit(ActivateAbility(playerId = player, sourceId = sneak, abilityId = sneakAbilityId))
            .outcome shouldBe Outcome.Done
        driver.bothPass()
    }

    test("puts a creature onto the battlefield with haste, sacrificed at the next end step") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val sneak = driver.putPermanentOnBattlefield(player, "Sneak Attack")
        val fatty = driver.putCardInHand(player, "Gurmag Angler")

        activate(driver, player, sneak)
        driver.submitCardSelection(player, listOf(fatty))

        val angler = driver.findPermanent(player, "Gurmag Angler").shouldNotBeNull()
        driver.state.projectedState.getKeywords(angler) shouldContain Keyword.HASTE.name

        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        driver.findPermanent(player, "Gurmag Angler") shouldBe null
        driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD)) shouldContain fatty
    }

    test("declining the put leaves the hand alone") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val sneak = driver.putPermanentOnBattlefield(player, "Sneak Attack")
        val fatty = driver.putCardInHand(player, "Gurmag Angler")

        activate(driver, player, sneak)
        driver.submitCardSelection(player, emptyList())

        driver.findPermanent(player, "Gurmag Angler") shouldBe null
        driver.state.getZone(ZoneKey(player, Zone.HAND)) shouldContain fatty
    }
})
