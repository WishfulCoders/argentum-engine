package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.nph.cards.Batterskull
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Batterskull (NPH #130).
 *
 *   Living weapon. Equipped creature gets +4/+4 and has vigilance and lifelink.
 *   {3}: Return this Equipment to its owner's hand.
 *   Equip {5}
 *
 * Pins the living-weapon Germ (a 4/4 vigilance lifelink), lifelink in combat, and the bounce that
 * leaves the 0/0 Germ to die.
 */
class BatterskullScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()
    private val bounce = Batterskull.activatedAbilities.single { !it.isEquipAbility }

    init {
        test("living weapon makes a 4/4 vigilance lifelink Germ; bouncing Batterskull kills the Germ") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Batterskull")
                .withLandsOnBattlefield(1, "Plains", 8)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpell(1, "Batterskull")
            withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val germ = game.findPermanent("Phyrexian Germ Token")
            withClue("A Germ token should exist") { germ shouldNotBe null }
            val skull = game.findPermanent("Batterskull")!!
            game.state.getEntity(skull)?.get<AttachedToComponent>()?.targetId shouldBe germ
            val projected = stateProjector.project(game.state)
            withClue("The Germ is a 4/4 with vigilance and lifelink") {
                projected.getPower(germ!!) shouldBe 4
                projected.getToughness(germ) shouldBe 4
                projected.hasKeyword(germ, Keyword.VIGILANCE) shouldBe true
                projected.hasKeyword(germ, Keyword.LIFELINK) shouldBe true
            }

            val result = game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = skull, abilityId = bounce.id)
            )
            withClue("Bounce should activate: ${result.error}") { result.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            withClue("Batterskull returned to its owner's hand") { game.isInHand(1, "Batterskull") shouldBe true }
            withClue("The now-0/0 Germ died") { game.findPermanent("Phyrexian Germ Token") shouldBe null }
        }

        test("equipped attacker gains life and stays untapped") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Batterskull", "Grizzly Bears")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers()
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            withClue("6 combat damage dealt, 6 life gained") {
                game.getLifeTotal(2) shouldBe 14
                game.getLifeTotal(1) shouldBe 26
            }
            withClue("Vigilance: the attacker did not tap") {
                game.state.getEntity(game.findPermanent("Grizzly Bears")!!)?.has<TappedComponent>() shouldBe false
            }
        }
    }
}
