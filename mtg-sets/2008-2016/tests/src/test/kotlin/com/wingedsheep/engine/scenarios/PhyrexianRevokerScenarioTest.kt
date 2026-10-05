package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.chosenCardName
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Phyrexian Revoker (MBS #122) — {2} Artifact Creature — Phyrexian Horror, 2/1.
 *
 *   As this creature enters, choose a nonland card name.
 *   Activated abilities of sources with the chosen name can't be activated.
 *
 * Pins the nonland name pool, the lock on a named permanent's ability, and — unlike Sorcerous
 * Spyglass — that mana abilities are locked too.
 */
class PhyrexianRevokerScenarioTest : ScenarioTestBase() {

    private fun TestGame.castRevokerNaming(name: String) {
        castSpell(1, "Phyrexian Revoker").error shouldBe null
        if (hasPendingDecision()) submitManaSourcesAutoPay()
        resolveStack()
        val decision = getPendingDecision()
        withClue("Revoker presents an as-enters card-name choice") {
            (decision is ChooseOptionDecision) shouldBe true
        }
        decision as ChooseOptionDecision
        withClue("The pool is nonland names") {
            decision.options shouldContain name
            decision.options shouldNotContain "Forest"
        }
        submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf(name)))
        resolveStack()
        state.getEntity(findPermanent("Phyrexian Revoker")!!)?.chosenCardName() shouldBe name
    }

    init {
        test("naming a card stops its activated ability") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Phyrexian Revoker")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardOnBattlefield(1, "Prodigal Sorcerer")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val sorcerer = game.findPermanent("Prodigal Sorcerer")!!
            withClue("Before Revoker, the Sorcerer's ping is a legal action") {
                game.getLegalActions(1).any { (it.action as? ActivateAbility)?.sourceId == sorcerer } shouldBe true
            }

            game.castRevokerNaming("Prodigal Sorcerer")

            withClue("After naming it, the ping is no longer offered") {
                game.getLegalActions(1).none { (it.action as? ActivateAbility)?.sourceId == sorcerer } shouldBe true
            }
            val ping = cardRegistry.getCard("Prodigal Sorcerer")!!.script.activatedAbilities.first()
            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = sorcerer,
                    abilityId = ping.id,
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                )
            )
            withClue("Direct activation of the named source's ability is rejected") { result.error shouldNotBe null }
        }

        test("mana abilities of the named source are locked too") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Phyrexian Revoker")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castRevokerNaming("Llanowar Elves")

            val elves = game.findPermanent("Llanowar Elves")!!
            val manaAbility = cardRegistry.getCard("Llanowar Elves")!!.script.activatedAbilities.first()
            val result = game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = elves, abilityId = manaAbility.id)
            )
            withClue("Unlike Sorcerous Spyglass, Revoker has no mana-ability exemption") {
                result.error shouldNotBe null
            }
            withClue("The Elves' mana ability is not offered as a legal action") {
                game.getLegalActions(1).none { (it.action as? ActivateAbility)?.sourceId == elves } shouldBe true
            }
        }
    }
}
