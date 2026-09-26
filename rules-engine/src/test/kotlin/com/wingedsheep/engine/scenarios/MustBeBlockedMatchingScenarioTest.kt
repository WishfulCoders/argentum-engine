package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import com.wingedsheep.engine.core.Outcome
import io.kotest.matchers.shouldNotBe

/**
 * Rule 509.1c requires the declaration of blockers to obey the maximum satisfiable number of
 * blocking requirements. When blockers differ in what they can block, that maximum is a
 * matching problem, not a per-attacker check: with attackers A (ground) and B (flying) both
 * "must be blocked if able", a ground-only blocker X and a flying blocker Y, the only
 * declaration obeying both requirements is X→A, Y→B. Parking Y on A and leaving X idle obeys
 * one requirement where two were satisfiable — illegal.
 *
 * Both attackers get the requirement from Deadly Allure ("Target creature gains deathtouch
 * until end of turn and must be blocked this turn if able").
 */
class MustBeBlockedMatchingScenarioTest : FunSpec({

    test("asymmetric blockers must be assigned so both must-be-blocked attackers are covered") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // p1 attacks with a ground creature and a flier, both must-be-blocked.
        val groundAttacker = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val flyingAttacker = driver.putCreatureOnBattlefield(p1, "Birds of Paradise")
        driver.removeSummoningSickness(groundAttacker)
        driver.removeSummoningSickness(flyingAttacker)

        // p2's blockers: Bears can block only the ground attacker; Birds can block either.
        val groundBlocker = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")
        val flexibleBlocker = driver.putCreatureOnBattlefield(p2, "Birds of Paradise")

        val allure1 = driver.putCardInHand(p1, "Deadly Allure")
        val allure2 = driver.putCardInHand(p1, "Deadly Allure")
        driver.giveMana(p1, Color.BLACK, 2)
        driver.castSpell(p1, allure1, targets = listOf(groundAttacker)).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.castSpell(p1, allure2, targets = listOf(flyingAttacker)).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(p1, listOf(groundAttacker, flyingAttacker), p2).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        // Parking the flexible blocker on the ground attacker strands the flier: the ground
        // blocker can't reach it, so only one requirement is obeyed where two are satisfiable.
        driver.declareBlockers(p2, mapOf(flexibleBlocker to listOf(groundAttacker))).outcome shouldNotBe Outcome.Done

        // Same coverage count the other way round: flier blocked, ground attacker strandable
        // only by wasting the ground blocker — leaving it idle is illegal too.
        driver.declareBlockers(p2, mapOf(flexibleBlocker to listOf(flyingAttacker))).outcome shouldNotBe Outcome.Done

        // The unique full-coverage assignment is legal.
        driver.declareBlockers(
            p2,
            mapOf(groundBlocker to listOf(groundAttacker), flexibleBlocker to listOf(flyingAttacker))
        ).outcome shouldBe Outcome.Done
    }

    /**
     * The declare-blockers legal action carries `mandatoryBlockerAssignments` — the pre-filled
     * blocks the web client and `AIPlayer`'s fallback submit. It used to cover only
     * "must block" requirements (Lure, provoke), not "must be blocked if able", so a declaration
     * built from it left the attacker unblocked and was then rejected for breaking 509.1c. It
     * now carries one satisfying assignment, from the same maximum matching the validator uses.
     */
    test("the declare-blockers legal action offers an assignment that obeys every must-be-blocked requirement") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val groundAttacker = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val flyingAttacker = driver.putCreatureOnBattlefield(p1, "Birds of Paradise")
        driver.removeSummoningSickness(groundAttacker)
        driver.removeSummoningSickness(flyingAttacker)
        val groundBlocker = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")
        val flexibleBlocker = driver.putCreatureOnBattlefield(p2, "Birds of Paradise")

        val allure1 = driver.putCardInHand(p1, "Deadly Allure")
        val allure2 = driver.putCardInHand(p1, "Deadly Allure")
        driver.giveMana(p1, Color.BLACK, 2)
        driver.castSpell(p1, allure1, targets = listOf(groundAttacker)).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.castSpell(p1, allure2, targets = listOf(flyingAttacker)).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(p1, listOf(groundAttacker, flyingAttacker), p2).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        val declareBlockers = driver.legalActions(p2).single { it.action is DeclareBlockers }
        val offered = declareBlockers.mandatoryBlockerAssignments ?: emptyMap()
        withClue("the offered blocks are the unique assignment covering both attackers") {
            offered shouldBe mapOf(
                groundBlocker to listOf(groundAttacker),
                flexibleBlocker to listOf(flyingAttacker),
            )
        }
        withClue("a declaration built from the offered blocks is legal") {
            driver.declareBlockers(p2, offered).outcome shouldBe Outcome.Done
        }
    }

    test("a lone must-be-blocked attacker is offered its blocker") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Gaea's Protector: "Gaea's Protector must be blocked if able."
        val protector = driver.putCreatureOnBattlefield(p1, "Gaea's Protector")
        driver.removeSummoningSickness(protector)
        val bears = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(p1, listOf(protector), p2).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        withClue("leaving the Protector unblocked is illegal while Grizzly Bears can block it") {
            driver.declareBlockers(p2, emptyMap()).outcome shouldNotBe Outcome.Done
        }
        val offered = driver.legalActions(p2).single { it.action is DeclareBlockers }
            .mandatoryBlockerAssignments ?: emptyMap()
        offered shouldBe mapOf(bears to listOf(protector))
        driver.declareBlockers(p2, offered).outcome shouldBe Outcome.Done
    }
})
