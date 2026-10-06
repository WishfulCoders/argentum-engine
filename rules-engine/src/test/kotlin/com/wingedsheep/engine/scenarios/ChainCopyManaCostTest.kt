package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.CopyRecipient
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Engine tests for a chain copy whose cost is **mana** ("Then that player or that permanent's
 * controller may pay {R}{R}. If the player does, they may copy this spell…" — Chain Lightning).
 *
 * Rules pinned here:
 *  - CR 118.3: a player can't pay a cost without the resources — an unaffordable mana cost (no
 *    sources, or sources of the wrong colour) means no offer at all.
 *  - CR 605.3a: the cost is a mana payment an effect asks for mid-resolution, so the *recipient*
 *    (not the spell's controller) pays it through a mana-payment window; floating mana counts.
 *  - "If the player does": declining the payment window means no copy is made and nothing is
 *    tapped.
 *  - Recursion: the copy is itself the chain spell, so whoever it hits may pay to copy it again.
 *  - A copy cost the chain flow can't collect is never offered for free (the latent bug where any
 *    cost other than sacrifice/discard silently gave the copy away).
 */
class ChainCopyManaCostTest : FunSpec({

    fun chainSpell(name: String, cost: PayCost) = card(name) {
        manaCost = "{R}"
        typeLine = "Sorcery"
        spell {
            val t = target(Targets.Any)
            effect = Effects.ChainCopy(
                action = Effects.DealDamage(3, t),
                target = t,
                offerTo = CopyRecipient.AFFECTED_PLAYER,
                copyTarget = Targets.Any,
                copyCost = cost
            )
        }
    }

    val manaChain = chainSpell("Test Mana Chain", Costs.pay.Mana("{R}{R}"))
    val lifeChain = chainSpell("Test Life Chain", Costs.pay.PayLife(2))

    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(manaChain, lifeChain))
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return Triple(driver, caster, opponent)
    }

    fun GameTestDriver.castAt(caster: EntityId, name: String, target: EntityId) {
        val spell = putCardInHand(caster, name)
        giveMana(caster, Color.RED, 1)
        castSpell(caster, spell, listOf(target)).outcome shouldBe Outcome.Done
        bothPass()
    }

    fun GameTestDriver.pool(player: EntityId) = state.getEntity(player)!!.get<ManaPoolComponent>()!!

    test("no red sources: the copy is never offered (CR 118.3)") {
        val (driver, caster, opponent) = setup()
        driver.putLandOnBattlefield(opponent, "Forest")
        driver.putLandOnBattlefield(opponent, "Forest")

        driver.castAt(caster, "Test Mana Chain", opponent)

        driver.isPaused shouldBe false
        driver.getLifeTotal(opponent) shouldBe 17
        driver.stackSize shouldBe 0
    }

    test("the recipient pays {R}{R} through the mana window, then copies onto a new target") {
        val (driver, caster, opponent) = setup()
        val m1 = driver.putLandOnBattlefield(opponent, "Mountain")
        val m2 = driver.putLandOnBattlefield(opponent, "Mountain")

        driver.castAt(caster, "Test Mana Chain", opponent)
        driver.getLifeTotal(opponent) shouldBe 17

        val offer = driver.pendingDecision
        offer.shouldBeInstanceOf<YesNoDecision>()
        offer.playerId shouldBe opponent
        offer.yesText shouldBe "Pay {R}{R}"
        driver.submitDecision(opponent, YesNoResponse(offer.id, true))

        // The payment window belongs to the recipient, mid-resolution, and may be declined.
        val window = driver.pendingDecision
        window.shouldBeInstanceOf<SelectManaSourcesDecision>()
        window.playerId shouldBe opponent
        window.canDecline shouldBe true
        window.availableSources.map { it.entityId } shouldContainExactlyInAnyOrder listOf(m1, m2)
        driver.submitDecision(opponent, ManaSourcesSelectedResponse(window.id, listOf(m1, m2)))

        driver.isTapped(m1) shouldBe true
        driver.isTapped(m2) shouldBe true

        val retarget = driver.pendingDecision
        retarget.shouldBeInstanceOf<SelectCardsDecision>()
        retarget.playerId shouldBe opponent
        driver.submitDecision(opponent, CardsSelectedResponse(retarget.id, listOf(caster)))

        // The copy is on the stack, controlled by the opponent; nobody can pay for a third.
        driver.stackSize shouldBe 1
        driver.bothPass()
        driver.getLifeTotal(caster) shouldBe 17
        driver.isPaused shouldBe false
        driver.stackSize shouldBe 0
        driver.pool(opponent).red shouldBe 0
    }

    test("declining the payment window means no copy and nothing tapped") {
        val (driver, caster, opponent) = setup()
        val m1 = driver.putLandOnBattlefield(opponent, "Mountain")
        val m2 = driver.putLandOnBattlefield(opponent, "Mountain")

        driver.castAt(caster, "Test Mana Chain", opponent)
        driver.submitYesNo(opponent, true)

        val window = driver.pendingDecision
        window.shouldBeInstanceOf<SelectManaSourcesDecision>()
        driver.submitDecision(opponent, ManaSourcesSelectedResponse(window.id, declined = true))

        driver.isPaused shouldBe false
        driver.stackSize shouldBe 0
        driver.isTapped(m1) shouldBe false
        driver.isTapped(m2) shouldBe false
        driver.getLifeTotal(caster) shouldBe 20
    }

    test("floating mana pays the cost directly, with no source window") {
        val (driver, caster, opponent) = setup()
        driver.giveMana(opponent, Color.RED, 2)

        driver.castAt(caster, "Test Mana Chain", opponent)
        driver.submitYesNo(opponent, true)

        val retarget = driver.pendingDecision
        retarget.shouldBeInstanceOf<SelectCardsDecision>()
        driver.pool(opponent).red shouldBe 0
        driver.submitDecision(opponent, CardsSelectedResponse(retarget.id, listOf(caster)))
        driver.bothPass()
        driver.getLifeTotal(caster) shouldBe 17
    }

    test("the copy chains back: whoever it hits may pay to copy it again") {
        val (driver, caster, opponent) = setup()
        driver.putLandOnBattlefield(opponent, "Mountain")
        driver.putLandOnBattlefield(opponent, "Mountain")
        val c1 = driver.putLandOnBattlefield(caster, "Mountain")
        val c2 = driver.putLandOnBattlefield(caster, "Mountain")

        driver.castAt(caster, "Test Mana Chain", opponent)
        driver.submitYesNo(opponent, true)
        driver.submitManaAutoPayOrDecline(opponent, autoPay = true)
        driver.submitCardSelection(opponent, listOf(caster))
        driver.bothPass()
        driver.getLifeTotal(caster) shouldBe 17

        // The original caster is now the affected player of the copy and may pay {R}{R} in turn.
        val offer = driver.pendingDecision
        offer.shouldBeInstanceOf<YesNoDecision>()
        offer.playerId shouldBe caster
        driver.submitDecision(caster, YesNoResponse(offer.id, false))
        driver.isTapped(c1) shouldBe false
        driver.isTapped(c2) shouldBe false
        driver.stackSize shouldBe 0
    }

    test("a copy cost the chain flow can't collect is never handed out for free") {
        val (driver, caster, opponent) = setup()

        driver.castAt(caster, "Test Life Chain", opponent)

        driver.isPaused shouldBe false
        driver.stackSize shouldBe 0
        driver.getLifeTotal(opponent) shouldBe 17
    }
})
