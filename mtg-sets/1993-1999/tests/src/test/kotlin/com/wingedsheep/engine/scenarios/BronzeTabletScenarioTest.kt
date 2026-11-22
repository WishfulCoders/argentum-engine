package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.ante.AnteSettlement
import com.wingedsheep.engine.mechanics.ante.AnteTransferCause
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.atq.cards.BronzeTablet
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Bronze Tablet {6} — Artifact:
 * "{4}, {T}: Exile this artifact and target nontoken permanent an opponent owns. That player may
 * pay 10 life. If they do, put this card into its owner's graveyard. Otherwise, that player owns
 * this card and you own the other exiled card."
 */
class BronzeTabletScenarioTest : ScenarioTestBase() {

    private fun setUp(opponentLife: Int = 20) = scenario()
        .withPlayers("Player1", "Player2")
        .playingForAnte()
        .withCardOnBattlefield(1, "Bronze Tablet")
        .withLandsOnBattlefield(1, "Mountain", 4)
        .withCardOnBattlefield(2, "Shivan Dragon")
        .withLifeTotal(2, opponentLife)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.activate() {
        val tablet = findPermanent("Bronze Tablet")!!
        val dragon = findPermanent("Shivan Dragon")!!
        execute(
            ActivateAbility(
                player1Id, tablet, BronzeTablet.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Permanent(dragon)),
            )
        ).error shouldBe null
        resolveStack()
    }

    init {
        test("the opponent pays 10 life: the Tablet goes to the graveyard, their card stays exiled") {
            val game = setUp()
            game.activate()

            withClue("the opponent decides") { game.hasPendingDecision() shouldBe true }
            game.answerYesNo(true)

            game.getLifeTotal(2) shouldBe 10
            game.isInGraveyard(1, "Bronze Tablet") shouldBe true
            game.isInExile(2, "Shivan Dragon") shouldBe true
            AnteSettlement.settle(game.state) shouldBe emptyList()
        }

        test("the opponent declines: the two exiled cards swap owners for keeps") {
            val game = setUp()
            game.activate()
            game.answerYesNo(false)

            game.getLifeTotal(2) shouldBe 20
            withClue("you own the Dragon and they own the Tablet, both still in exile") {
                game.isInExile(1, "Shivan Dragon") shouldBe true
                game.isInExile(2, "Bronze Tablet") shouldBe true
                val dragon = game.state.getZone(game.player1Id, Zone.EXILE).single()
                game.state.getEntity(dragon)?.get<OwnerComponent>()?.playerId shouldBe game.player1Id
            }
            val settled = AnteSettlement.settle(game.state).associateBy { it.cardName }
            settled.getValue("Shivan Dragon").toPlayerId shouldBe game.player1Id
            settled.getValue("Bronze Tablet").toPlayerId shouldBe game.player2Id
            settled.values.all { it.cause == AnteTransferCause.OWNERSHIP } shouldBe true
        }

        test("an opponent below 10 life can't pay, so the swap happens without asking (CR 119.4)") {
            val game = setUp(opponentLife = 9)
            game.activate()

            game.hasPendingDecision() shouldBe false
            game.isInExile(1, "Shivan Dragon") shouldBe true
            game.isInExile(2, "Bronze Tablet") shouldBe true
        }
    }
}
