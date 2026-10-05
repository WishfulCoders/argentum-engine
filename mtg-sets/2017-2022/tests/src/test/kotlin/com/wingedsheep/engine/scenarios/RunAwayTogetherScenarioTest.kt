package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.RunAwayTogether
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.types.shouldBeInstanceOf
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.CastSpell

/**
 * Run Away Together — "Choose two target creatures controlled by different players. Return those
 * creatures to their owners' hands."
 *
 * The only restriction is that the two controllers differ: in multiplayer the pair can be two
 * different opponents' creatures. A token returned to hand ceases to exist (CR 704.5d).
 */
class RunAwayTogetherScenarioTest : FunSpec({

    fun GameTestDriver.createMouseTokenOnBattlefield(playerId: EntityId): EntityId {
        val tokenId = EntityId.generate()
        val tokenCard = CardComponent(
            cardDefinitionId = "token:Mouse",
            name = "Mouse Token",
            manaCost = ManaCost.ZERO,
            typeLine = TypeLine.parse("Creature - Mouse"),
            baseStats = CreatureStats(1, 1),
            colors = setOf(Color.WHITE),
            ownerId = playerId,
        )
        val container = ComponentContainer.of(
            tokenCard,
            TokenComponent,
            ControllerComponent(playerId),
            SummoningSicknessComponent,
        )
        replaceState(
            state
                .withEntity(tokenId, container)
                .addToZone(ZoneKey(playerId, Zone.BATTLEFIELD), tokenId),
        )
        return tokenId
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + RunAwayTogether)
        return driver
    }

    test("opponent's token ceases to exist when Run Away Together returns it to hand") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ownCreature = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        val opponentToken = driver.createMouseTokenOnBattlefield(opponent)

        val spell = driver.putCardInHand(activePlayer, "Run Away Together")
        driver.giveMana(activePlayer, Color.BLUE, 2)

        val result = driver.castSpell(activePlayer, spell, listOf(ownCreature, opponentToken))
        result.outcome shouldBe Outcome.Done
        driver.bothPass()

        // Non-token bounces normally: leaves battlefield, ends up in owner's hand.
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldBe null
        driver.getHand(activePlayer).any { driver.getCardName(it) == "Grizzly Bears" } shouldBe true

        // Token leaves battlefield and is removed from the game (704.5d).
        driver.findPermanent(opponent, "Mouse Token") shouldBe null
        driver.getHand(opponent).contains(opponentToken) shouldBe false
        driver.getHand(opponent).any { driver.getCardName(it) == "Mouse Token" } shouldBe false
        driver.state.getEntity(opponentToken) shouldBe null
    }

    test("your own token ceases to exist when Run Away Together returns it to hand") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ownToken = driver.createMouseTokenOnBattlefield(activePlayer)
        val opponentCreature = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val spell = driver.putCardInHand(activePlayer, "Run Away Together")
        driver.giveMana(activePlayer, Color.BLUE, 2)

        val result = driver.castSpell(activePlayer, spell, listOf(ownToken, opponentCreature))
        result.outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.findPermanent(activePlayer, "Mouse Token") shouldBe null
        driver.getHand(activePlayer).contains(ownToken) shouldBe false
        driver.getHand(activePlayer).any { driver.getCardName(it) == "Mouse Token" } shouldBe false
        driver.state.getEntity(ownToken) shouldBe null

        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
        driver.getHand(opponent).any { driver.getCardName(it) == "Grizzly Bears" } shouldBe true
    }
    fun threePlayerDriver(): Pair<GameTestDriver, List<EntityId>> {
        val driver = createDriver()
        val players = driver.initMultiplayer(decks = List(3) { Deck.of("Island" to 40) })
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to players
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (stackSize > 0 && guard++ < 12) passPriority(priorityPlayer!!)
    }

    test("in multiplayer it can target creatures controlled by two different opponents") {
        val (driver, players) = threePlayerDriver()
        val (caster, opponentA, opponentB) = players
        val bearsA = driver.putCreatureOnBattlefield(opponentA, "Grizzly Bears")
        val bearsB = driver.putCreatureOnBattlefield(opponentB, "Grizzly Bears")

        val spell = driver.putCardInHand(caster, "Run Away Together")
        driver.giveMana(caster, Color.BLUE, 2)

        driver.castSpell(caster, spell, listOf(bearsA, bearsB)).outcome shouldBe Outcome.Done
        driver.resolveStack()

        driver.findPermanent(opponentA, "Grizzly Bears") shouldBe null
        driver.findPermanent(opponentB, "Grizzly Bears") shouldBe null
        driver.getHand(opponentA).any { driver.getCardName(it) == "Grizzly Bears" } shouldBe true
        driver.getHand(opponentB).any { driver.getCardName(it) == "Grizzly Bears" } shouldBe true
    }

    test("two creatures controlled by the same player can't be chosen") {
        val (driver, players) = threePlayerDriver()
        val (caster, opponentA, _) = players
        val first = driver.putCreatureOnBattlefield(opponentA, "Grizzly Bears")
        val second = driver.putCreatureOnBattlefield(opponentA, "Grizzly Bears")

        val spell = driver.putCardInHand(caster, "Run Away Together")
        driver.giveMana(caster, Color.BLUE, 2)

        driver.castSpell(caster, spell, listOf(first, second)).outcome.shouldBeInstanceOf<Outcome.Rejected>()
    }

    test("not offered when every creature is controlled by one player") {
        val (driver, players) = threePlayerDriver()
        val (caster, opponentA, _) = players
        driver.putCreatureOnBattlefield(opponentA, "Grizzly Bears")
        driver.putCreatureOnBattlefield(opponentA, "Grizzly Bears")

        val spell = driver.putCardInHand(caster, "Run Away Together")
        driver.giveMana(caster, Color.BLUE, 2)

        driver.legalActions(caster).none { it.action is CastSpell && (it.action as CastSpell).cardId == spell } shouldBe true
    }

    test("the offered targets span every player's creatures") {
        val (driver, players) = threePlayerDriver()
        val (caster, opponentA, opponentB) = players
        val mine = driver.putCreatureOnBattlefield(caster, "Grizzly Bears")
        val bearsA = driver.putCreatureOnBattlefield(opponentA, "Grizzly Bears")
        val bearsB = driver.putCreatureOnBattlefield(opponentB, "Grizzly Bears")

        val spell = driver.putCardInHand(caster, "Run Away Together")
        driver.giveMana(caster, Color.BLUE, 2)

        val cast = driver.legalActions(caster).first { (it.action as? CastSpell)?.cardId == spell }
        val requirement = cast.targetRequirements!!.single()
        requirement.minTargets shouldBe 2
        requirement.differentControllers shouldBe true
        requirement.validTargets shouldContainExactlyInAnyOrder listOf(mine, bearsA, bearsB)
    }

    test("both targets are illegal when they end up under one controller before it resolves") {
        val (driver, players) = threePlayerDriver()
        val (caster, opponentA, opponentB) = players
        val bearsA = driver.putCreatureOnBattlefield(opponentA, "Grizzly Bears")
        val bearsB = driver.putCreatureOnBattlefield(opponentB, "Grizzly Bears")

        val spell = driver.putCardInHand(caster, "Run Away Together")
        driver.giveMana(caster, Color.BLUE, 2)
        driver.castSpell(caster, spell, listOf(bearsA, bearsB)).outcome shouldBe Outcome.Done

        // Opponent B gains control of opponent A's creature in response.
        driver.replaceState(driver.state.updateEntity(bearsA) { it.with(ControllerComponent(opponentB)) })
        driver.resolveStack()

        driver.state.getBattlefield().contains(bearsA) shouldBe true
        driver.state.getBattlefield().contains(bearsB) shouldBe true
    }
})
