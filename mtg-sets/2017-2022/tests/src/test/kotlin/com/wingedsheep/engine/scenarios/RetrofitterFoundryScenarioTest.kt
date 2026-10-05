package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.c18.cards.RetrofitterFoundry
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Retrofitter Foundry (C18 #57).
 *
 *   {3}: Untap this artifact.
 *   {2}, {T}: Create a 1/1 colorless Servo artifact creature token.
 *   {1}, {T}, Sacrifice a Servo: Create a 1/1 colorless Thopter artifact creature token with flying.
 *   {T}, Sacrifice a Thopter: Create a 4/4 colorless Construct artifact creature token.
 *
 * Walks the whole upgrade chain in one turn, using the untap ability between steps, and checks that
 * each sacrifice cost only accepts its own creature type.
 */
class RetrofitterFoundryScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()
    private val untap = RetrofitterFoundry.activatedAbilities[0]
    private val makeServo = RetrofitterFoundry.activatedAbilities[1]
    private val makeThopter = RetrofitterFoundry.activatedAbilities[2]
    private val makeConstruct = RetrofitterFoundry.activatedAbilities[3]

    private fun TestGame.activate(abilityId: AbilityId, sacrifice: EntityId? = null) =
        execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = findPermanent("Retrofitter Foundry")!!,
                abilityId = abilityId,
                costPayment = sacrifice?.let { AdditionalCostPayment(sacrificedPermanents = listOf(it)) },
            )
        ).also {
            withClue("Activation should succeed: ${it.error}") { it.error shouldBe null }
            if (hasPendingDecision()) submitManaSourcesAutoPay()
            resolveStack()
        }

    init {
        test("Servo, untap, Thopter, untap, Construct") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Retrofitter Foundry")
                .withLandsOnBattlefield(1, "Plains", 9)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.activate(makeServo.id)
            val servo = game.findPermanent("Servo Token")
            withClue("A Servo was created") { servo shouldNotBe null }

            game.activate(untap.id)
            game.activate(makeThopter.id, sacrifice = servo)
            withClue("The Servo was sacrificed for a flying Thopter") {
                game.findPermanent("Servo Token") shouldBe null
                val thopter = game.findPermanent("Thopter Token")
                thopter shouldNotBe null
                stateProjector.project(game.state).hasKeyword(thopter!!, Keyword.FLYING) shouldBe true
            }

            game.activate(untap.id)
            game.activate(makeConstruct.id, sacrifice = game.findPermanent("Thopter Token"))
            withClue("The Thopter was sacrificed for a 4/4 Construct") {
                game.findPermanent("Thopter Token") shouldBe null
                val construct = game.findPermanent("Construct Token")
                construct shouldNotBe null
                stateProjector.project(game.state).getPower(construct!!) shouldBe 4
                stateProjector.project(game.state).getToughness(construct) shouldBe 4
            }
        }

        test("the Thopter ability can't sacrifice a non-Servo") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Retrofitter Foundry")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = game.findPermanent("Retrofitter Foundry")!!,
                    abilityId = makeThopter.id,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(game.findPermanent("Grizzly Bears")!!)),
                )
            )
            withClue("Grizzly Bears is not a Servo") { result.error shouldNotBe null }
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
