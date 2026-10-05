package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.lea.cards.ManaVault
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Mana Vault (LEA #259).
 *
 *   This artifact doesn't untap during your untap step.
 *   At the beginning of your upkeep, you may pay {4}. If you do, untap this artifact.
 *   At the beginning of your draw step, if this artifact is tapped, it deals 1 damage to you.
 *   {T}: Add {C}{C}{C}.
 *
 * Pins the cross-step chain: the untap step skips it, the upkeep may-pay untaps it, and the
 * draw-step intervening "if" only deals damage when it is still tapped.
 */
class ManaVaultScenarioTest : ScenarioTestBase() {

    private val manaAbility = ManaVault.activatedAbilities.single()

    private fun board() = scenario()
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, "Mana Vault", tapped = true)
        .withLandsOnBattlefield(1, "Mountain", 4)
        .apply {
            repeat(4) { withCardInLibrary(1, "Island") }
            repeat(4) { withCardInLibrary(2, "Island") }
        }
        .withActivePlayer(1)
        .withTurnNumber(3)
        .inPhase(Phase.BEGINNING, Step.UNTAP)
        .build()

    private fun TestGame.vaultTapped(): Boolean =
        state.getEntity(findPermanent("Mana Vault")!!)?.has<TappedComponent>() == true

    init {
        test("stays tapped through the untap step; declining to pay deals 1 damage in the draw step") {
            val game = board()
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("Mana Vault does not untap during the untap step") { game.vaultTapped() shouldBe true }

            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false)
            game.resolveStack()
            withClue("Declining leaves it tapped") { game.vaultTapped() shouldBe true }

            game.passUntilPhase(Phase.BEGINNING, Step.DRAW)
            game.resolveStack()
            withClue("A tapped Mana Vault deals 1 damage to its controller in the draw step") {
                game.getLifeTotal(1) shouldBe 19
            }
        }

        test("paying {4} in upkeep untaps it, so the draw step deals no damage") {
            val game = board()
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true)
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()
            withClue("Paying {4} untaps Mana Vault") { game.vaultTapped() shouldBe false }

            game.passUntilPhase(Phase.BEGINNING, Step.DRAW)
            game.resolveStack()
            withClue("An untapped Mana Vault deals no damage") { game.getLifeTotal(1) shouldBe 20 }
        }

        test("tapping it adds three colorless mana") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Mana Vault")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val vault = game.findPermanent("Mana Vault")!!
            val result = game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = vault, abilityId = manaAbility.id)
            )
            withClue("Mana ability should activate: ${result.error}") { result.error shouldBe null }
            val pool = game.state.getEntity(game.player1Id)
                ?.get<ManaPoolComponent>()
            withClue("The pool holds {C}{C}{C}") { pool?.colorless shouldBe 3 }
            game.vaultTapped() shouldBe true
        }
    }
}
