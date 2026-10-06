package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * `ActivationRestriction.OnlyAsInstant` on a mana ability — "Activate only as an instant"
 * (CR 602.5e), the Lion's Eye Diamond shape.
 *
 * - CR 605.1: the ability is still a mana ability regardless of the timing restriction, so it
 *   doesn't use the stack and resolves at once (CR 605.3b).
 * - CR 602.5e / 304.5: it can be activated only while its controller holds priority — including
 *   with a spell on the stack (instant timing, not sorcery timing).
 * - CR 605.3a's other two windows are closed to it: it can't be activated while a cost is being paid
 *   or "whenever a rule or effect asks for a mana payment" (here, ward). 2004-10-04 Lion's Eye Diamond
 *   ruling: "it can only be activated at times when you can cast an instant."
 * - The affordability helpers never count it, so nothing is highlighted castable on its mana.
 *
 * Every assertion is paired with an unrestricted twin, so a test fails only on the restriction.
 */
class OnlyAsInstantManaAbilityTest : FunSpec({

    val instantDiamond = card("Test Instant Diamond") {
        manaCost = "{0}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Composite(Costs.DiscardHand, Costs.SacrificeSelf)
            effect = Effects.AddAnyColorMana(3)
            manaAbility = true
            restrictions = listOf(ActivationRestriction.OnlyAsInstant)
        }
    }
    val freeDiamond = card("Test Unrestricted Diamond") {
        manaCost = "{0}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Composite(Costs.DiscardHand, Costs.SacrificeSelf)
            effect = Effects.AddAnyColorMana(3)
            manaAbility = true
        }
    }
    val wardedBear = card("Test Warded Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))
    }

    val instantAbility = instantDiamond.script.activatedAbilities.single().id
    val freeAbility = freeDiamond.script.activatedAbilities.single().id

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(instantDiamond, freeDiamond, wardedBear))
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.pool(player: EntityId): ManaPoolComponent =
        state.getEntity(player)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    fun GameTestDriver.activate(player: EntityId, source: EntityId, ability: com.wingedsheep.sdk.scripting.AbilityId) =
        submit(ActivateAbility(playerId = player, sourceId = source, abilityId = ability, manaColorChoice = Color.RED))

    /** Casts Lightning Bolt at an opponent's Ward {2} creature and lets ward ask for its mana. */
    fun GameTestDriver.openWardWindow(you: EntityId): SelectManaSourcesDecision {
        val bear = putCreatureOnBattlefield(getOpponent(you), "Test Warded Bear")
        giveMana(you, Color.RED, 1)
        val bolt = putCardInHand(you, "Lightning Bolt")
        castSpellWithTargets(you, bolt, listOf(ChosenTarget.Permanent(bear))).error shouldBe null
        bothPass()
        return pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
    }

    test("with priority it is a mana ability: the hand is discarded, mana is added, nothing uses the stack") {
        val d = driver()
        val you = d.activePlayer!!
        val diamond = d.putPermanentOnBattlefield(you, "Test Instant Diamond")
        d.putCardInHand(you, "Lightning Bolt")
        d.putCardInHand(you, "Mountain")

        d.activate(you, diamond, instantAbility).error shouldBe null

        d.pool(you).red shouldBe 3
        d.getHandSize(you) shouldBe 0
        d.getGraveyardCardNames(you) shouldContain "Test Instant Diamond"
        withClue("CR 605.3b — a mana ability resolves immediately, it never goes on the stack") {
            d.state.stack.size shouldBe 0
        }
        d.priorityPlayer shouldBe you
    }

    test("instant timing, not sorcery timing: it can be activated with a spell on the stack") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        val diamond = d.putPermanentOnBattlefield(you, "Test Instant Diamond")
        d.giveMana(you, Color.RED, 1)
        val bolt = d.putCardInHand(you, "Lightning Bolt")
        d.castSpellWithTargets(you, bolt, listOf(ChosenTarget.Player(opponent))).error shouldBe null
        d.state.stack.size shouldBe 1

        d.activate(you, diamond, instantAbility).error shouldBe null
        d.pool(you).red shouldBe 3
        d.state.stack.size shouldBe 1
    }

    test("not offered and rejected while a rule asks for a mana payment (ward) — its unrestricted twin is") {
        val d = driver()
        val you = d.activePlayer!!
        val instant = d.putPermanentOnBattlefield(you, "Test Instant Diamond")
        val free = d.putPermanentOnBattlefield(you, "Test Unrestricted Diamond")
        d.openWardWindow(you)

        val offered = d.services.legalActionEnumerator.enumerateManaAbilities(d.state, you)
            .mapNotNull { it.action as? ActivateAbility }.map { it.sourceId }
        offered shouldNotContain instant
        offered shouldContain free

        val rejected = d.activate(you, instant, instantAbility)
        rejected.error.shouldNotBeNull()
        withClue("the rejected activation paid nothing") {
            d.findPermanent(you, "Test Instant Diamond") shouldBe instant
            d.pool(you).total shouldBe 0
        }

        d.activate(you, free, freeAbility).error shouldBe null
        d.pool(you).red shouldBe 3
        withClue("the window stays open, now coverable from the floating mana") {
            d.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        }
    }

    test("ward can't be paid with it: with no other mana the cost is unpayable and the spell is countered") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.putPermanentOnBattlefield(you, "Test Instant Diamond")
        val bear = d.putCreatureOnBattlefield(opponent, "Test Warded Bear")
        d.giveMana(you, Color.RED, 1)
        val bolt = d.putCardInHand(you, "Lightning Bolt")
        d.castSpellWithTargets(you, bolt, listOf(ChosenTarget.Permanent(bear))).error shouldBe null
        d.bothPass()

        withClue("the Diamond is no payment source, so ward finds {2} unpayable and never opens a window") {
            (d.pendingDecision is SelectManaSourcesDecision).shouldBeFalse()
        }
        repeat(4) { if (d.state.stack.isNotEmpty()) d.bothPass() }
        d.findPermanent(opponent, "Test Warded Bear") shouldBe bear
        d.getGraveyardCardNames(you) shouldContain "Lightning Bolt"
        d.findPermanent(you, "Test Instant Diamond") shouldNotBe null
    }

    test("affordability never counts it as payment mana; its unrestricted twin is counted") {
        val instantGame = driver()
        val p1 = instantGame.activePlayer!!
        instantGame.putPermanentOnBattlefield(p1, "Test Instant Diamond")
        val solver1 = instantGame.services.manaSolver
        solver1.getAvailableManaCount(instantGame.state, p1) shouldBe 0
        solver1.canPay(instantGame.state, p1, ManaCost.parse("{R}")).shouldBeFalse()
        withClue("a spell needing its mana is not offered as castable") {
            val bolt = instantGame.putCardInHand(p1, "Lightning Bolt")
            instantGame.legalActions(p1).filter { (it.action as? com.wingedsheep.engine.core.CastSpell)?.cardId == bolt }
                .none { it.affordable }.shouldBeTrue()
        }

        val freeGame = driver()
        val p2 = freeGame.activePlayer!!
        freeGame.putPermanentOnBattlefield(p2, "Test Unrestricted Diamond")
        freeGame.services.manaSolver.getAvailableManaCount(freeGame.state, p2) shouldBe 3
        freeGame.services.manaSolver.canPay(freeGame.state, p2, ManaCost.parse("{R}")).shouldBeTrue()
    }

    test("activated first, with priority, its floating mana pays for the spell") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        val diamond = d.putPermanentOnBattlefield(you, "Test Instant Diamond")
        d.activate(you, diamond, instantAbility).error shouldBe null
        val bolt = d.putCardInHand(you, "Lightning Bolt")

        d.castSpellWithTargets(you, bolt, listOf(ChosenTarget.Player(opponent))).error shouldBe null
        d.bothPass()

        d.getLifeTotal(opponent) shouldBe 17
        d.pool(you).red shouldBe 2
    }

    test("the opponent can't activate it — it needs its controller holding priority (CR 602.5e)") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        val diamond = d.putPermanentOnBattlefield(opponent, "Test Instant Diamond")
        withClue("the non-priority player is not offered it") {
            d.services.legalActionEnumerator.enumerateManaAbilities(d.state, opponent)
                .mapNotNull { it.action as? ActivateAbility }.map { it.sourceId } shouldNotContain diamond
        }
        d.activate(opponent, diamond, instantAbility).error.shouldNotBeNull()
        d.findPermanent(opponent, "Test Instant Diamond") shouldBe diamond
    }
})
