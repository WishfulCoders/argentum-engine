package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Emrakul, the Exigent Doom (Reality Fracture #1) — {10} Legendary Creature — Eldrazi 12/12:
 *   When you cast this spell, untap all lands you control.
 *   Flying, trample
 *   Ward—Sacrifice three permanents.
 *   {3}, Exile this card from your hand: Target land gains "{T}: Add {C}{C}" until this card is
 *   cast from exile. You may cast this card for as long as it remains exiled.
 */
class EmrakulTheExigentDoomScenarioTest : ScenarioTestBase() {

    private val name = "Emrakul, the Exigent Doom"

    private fun handAbilityId() = cardRegistry.getCard(name)!!
        .activatedAbilities.first { it.activateFromZone == Zone.HAND }.id

    /** Twelve Forests; pay the {3} with the first three, targeting the fourth. */
    private fun TestGame.activateFromHand(): Pair<EntityId, List<EntityId>> {
        val forests = findPermanents("Forest")
        forests.size shouldBe 12
        val emrakul = findCardsInHand(1, name).first()
        val land = forests[3]
        val result = execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = emrakul,
                abilityId = handAbilityId(),
                targets = listOf(ChosenTarget.Permanent(land)),
                paymentStrategy = PaymentStrategy.Explicit(forests.take(3))
            )
        )
        withClue("{3}, exile Emrakul from hand: ${result.error}") { result.error shouldBe null }
        return land to forests
    }

    private fun TestGame.grantsOn(land: EntityId) =
        state.grantedActivatedAbilities.filter { it.entityId == land }

    init {
        test("exiling it from hand grants the land {T}: Add {C}{C} and lets you cast it from exile") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, name)
                .withLandsOnBattlefield(1, "Forest", 12)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val emrakul = game.findCardsInHand(1, name).first()
            val (land, _) = game.activateFromHand()
            withClue("the cost exiles the card before the ability resolves") {
                game.isInExile(1, name) shouldBe true
            }
            game.resolveStack()

            val grant = game.grantsOn(land).single()
            grant.duration shouldBe Duration.UntilSourceCastFromExile
            game.state.mayPlayPermissions.any { emrakul in it.cardIds && it.permanent } shouldBe true
            withClue("the land now offers the granted {T}: Add {C}{C} mana ability") {
                game.getLegalActions(1).any {
                    val action = it.action as? ActivateAbility
                    action?.sourceId == land && action.abilityId == grant.ability.id
                } shouldBe true
            }

            // The grant is not end-of-turn: it's still there on the next turn.
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.grantsOn(land).size shouldBe 1
            game.isInExile(1, name) shouldBe true
        }

        test("casting it from exile ends the grant, and the cast trigger untaps all your lands") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, name)
                .withLandsOnBattlefield(1, "Forest", 12)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val emrakul = game.findCardsInHand(1, name).first()
            val (land, forests) = game.activateFromHand()
            game.resolveStack()

            // Nine untapped lands: eight plain Forests + the granted land's {C}{C} = exactly the {10}.
            // Auto-pay has to tap the granted land for {C}{C} (not {G}) to get there.
            withClue("nine untapped lands, one of them making {C}{C}: Emrakul is castable from exile") {
                game.getLegalActions(1).any {
                    (it.action as? CastSpell)?.cardId == emrakul && it.isAffordable
                } shouldBe true
            }
            val cast = game.execute(CastSpell(game.player1Id, emrakul))
            withClue("auto-pay Emrakul from exile for {10}: ${cast.error}") { cast.error shouldBe null }

            withClue("the grant ends the moment the card is cast from exile") {
                game.grantsOn(land).size shouldBe 0
            }
            withClue("every land paid for something before the cast trigger resolves") {
                forests.all { game.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe true
            }

            game.resolveStack()

            game.isOnBattlefield(name) shouldBe true
            withClue("the cast trigger untapped all lands you control") {
                forests.none { game.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe true
            }
            game.grantsOn(land).size shouldBe 0
        }

        test("tapping the granted land by hand for {C}{C} also pays for the cast") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, name)
                .withLandsOnBattlefield(1, "Forest", 12)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val emrakul = game.findCardsInHand(1, name).first()
            val (land, _) = game.activateFromHand()
            game.resolveStack()

            val grant = game.grantsOn(land).single()
            val tapForTwo = game.execute(ActivateAbility(game.player1Id, land, grant.ability.id))
            withClue("tap the land for {C}{C}: ${tapForTwo.error}") { tapForTwo.error shouldBe null }
            val cast = game.execute(CastSpell(game.player1Id, emrakul))
            withClue("cast Emrakul with {C}{C} floating: ${cast.error}") { cast.error shouldBe null }
            game.grantsOn(land).size shouldBe 0
            game.resolveStack()
            game.isOnBattlefield(name) shouldBe true
        }
    }
}
