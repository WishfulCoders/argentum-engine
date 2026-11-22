package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.c20.cards.DeflectingSwat
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Deflecting Swat — "If you control a commander, you may cast this spell without paying its mana
 * cost. You may choose new targets for target spell or ability."
 *
 * Sideswipe already pins the retarget on a spell; this pins the two things Swat adds: the target
 * may be an *ability* on the stack, and the `{0}` cast is gated on controlling a commander.
 */
class DeflectingSwatScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(DeflectingSwat))
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 40)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.tagCommander(entity: EntityId, owner: EntityId) =
        replaceState(state.updateEntity(entity) { it.with(CommanderComponent(ownerId = owner)) })

    fun freeSwat(player: EntityId, swat: EntityId, target: EntityId) = CastSpell(
        playerId = player,
        cardId = swat,
        targets = listOf(ChosenTarget.Spell(target)),
        useAlternativeCost = true,
        alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE
    )

    test("with a commander, a free Swat sends an opponent's activated ability at their own creature") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        d.tagCommander(d.putCreatureOnBattlefield(me, "Savannah Lions"), me)
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val sorcerer = d.putCreatureOnBattlefield(opp, "Prodigal Sorcerer")
        d.removeSummoningSickness(sorcerer)
        val theirLions = d.putCreatureOnBattlefield(opp, "Savannah Lions")
        val swat = d.putCardInHand(me, "Deflecting Swat")

        d.passPriority(me)
        val ping = d.cardRegistry.getCard("Prodigal Sorcerer")!!.activatedAbilities[0].id
        d.submit(
            ActivateAbility(
                playerId = opp,
                sourceId = sorcerer,
                abilityId = ping,
                targets = listOf(ChosenTarget.Permanent(bears))
            )
        ).error shouldBe null
        val ability = d.getTopOfStack()!!
        d.passPriority(opp)

        d.submit(freeSwat(me, swat, ability)).error shouldBe null
        d.stackSize shouldBe 2
        d.bothPass() // Swat resolves and offers the ability's one target slot
        d.submitCardSelection(me, listOf(theirLions)).error shouldBe null
        d.bothPass() // the ping resolves at its new target

        d.stackSize shouldBe 0
        d.findPermanent(me, "Grizzly Bears") shouldNotBe null
        d.state.getBattlefield().contains(theirLions) shouldBe false
    }

    test("without a commander the free cast is refused, and paying {2}{R} retargets a spell") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val swat = d.putCardInHand(me, "Deflecting Swat")
        val bolt = d.putCardInHand(opp, "Lightning Bolt")
        d.giveMana(opp, Color.RED, 1)

        d.passPriority(me)
        d.submit(
            CastSpell(opp, bolt, targets = listOf(ChosenTarget.Player(me)), paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        val boltOnStack = d.getTopOfStack()!!
        d.passPriority(opp)

        d.submit(freeSwat(me, swat, boltOnStack)).error shouldNotBe null

        d.giveMana(me, Color.RED, 3)
        d.submit(
            CastSpell(me, swat, targets = listOf(ChosenTarget.Spell(boltOnStack)), paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        d.bothPass()
        d.submitCardSelection(me, listOf(opp)).error shouldBe null
        d.bothPass()

        d.getLifeTotal(me) shouldBe 40
        d.getLifeTotal(opp) shouldBe 37
    }
})
