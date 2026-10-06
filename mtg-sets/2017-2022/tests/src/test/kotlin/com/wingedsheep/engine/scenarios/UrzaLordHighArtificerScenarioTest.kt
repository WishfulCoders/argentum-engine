package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Urza, Lord High Artificer (MH1 #75).
 *
 *   When Urza enters, create a 0/0 Construct artifact creature token with "This token gets +1/+1
 *   for each artifact you control."
 *   Tap an untapped artifact you control: Add {U}.
 *   {5}: Shuffle your library, then exile the top card. Until end of turn, you may play that card
 *   without paying its mana cost.
 */
class UrzaLordHighArtificerScenarioTest : ScenarioTestBase() {

    private val urza = "Urza, Lord High Artificer"

    init {
        test("the Construct counts every artifact you control, itself included") {
            val game = scenario().withPlayers("P1", "P2")
                .withCardInHand(1, urza)
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardOnBattlefield(1, "Sol Ring")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, urza).error shouldBe null
            game.resolveStack()

            val constructs = game.state.getBattlefield().filter {
                game.state.getEntity(it)?.get<CardComponent>()?.name?.startsWith("Construct") == true
            }
            constructs shouldHaveSize 1
            withClue("Sol Ring + the Construct itself = +2/+2 on a 0/0") {
                game.state.projectedState.getPower(constructs.single()) shouldBe 2
                game.state.projectedState.getToughness(constructs.single()) shouldBe 2
            }
        }

        test("tapping a summoning-sick artifact creature pays the mana ability") {
            val game = scenario().withPlayers("P1", "P2")
                .withCardOnBattlefield(1, urza)
                .withCardOnBattlefield(1, "Ornithopter", summoningSickness = true)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val ornithopter = game.findPermanent("Ornithopter")!!
            val manaAbility = cardRegistry.getCard(urza)!!.script.activatedAbilities[0]
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = game.findPermanent(urza)!!,
                    abilityId = manaAbility.id,
                    costPayment = AdditionalCostPayment(tappedPermanents = listOf(ornithopter))
                )
            ).error shouldBe null

            game.state.getEntity(ornithopter)?.has<TappedComponent>() shouldBe true
            game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.blue shouldBe 1
        }

        test("{5}: the exiled card may be played this turn without paying its mana cost") {
            val game = scenario().withPlayers("P1", "P2")
                .withCardOnBattlefield(1, urza)
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardInLibrary(1, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val ability = cardRegistry.getCard(urza)!!.script.activatedAbilities[1]
            game.execute(
                ActivateAbility(game.player1Id, game.findPermanent(urza)!!, ability.id)
            ).error shouldBe null
            game.resolveStack()

            withClue("the only library card was exiled") {
                game.isInExile(1, "Hill Giant") shouldBe true
            }
            withClue("every Island paid the {5}, yet the Giant casts for free") {
                game.castSpellFromExile(1, "Hill Giant").error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Hill Giant") shouldBe true
            }
        }
    }
}
