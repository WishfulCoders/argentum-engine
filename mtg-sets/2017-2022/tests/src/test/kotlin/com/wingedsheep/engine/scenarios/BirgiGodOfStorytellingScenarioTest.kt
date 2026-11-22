package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.effects.ManaExpiry
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Birgi, God of Storytelling (KHM #123) — "Whenever you cast a spell, add {R}. Until end of turn,
 * you don't lose this mana as steps and phases end. Creatures you control can boast twice during
 * each of your turns rather than once."
 *
 * Covers the cast trigger adding a kept {R} that survives a step change, and the boast clause
 * letting an attacking creature boast a second time (but not a third).
 */
class BirgiGodOfStorytellingScenarioTest : ScenarioTestBase() {

    private val freeSpell = card("Birgi Test Tale") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(1) }
    }

    private val boaster = card("Birgi Test Skald") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Human Berserker"
        power = 2
        toughness = 2
        activatedAbility {
            isBoast = true
            cost = Costs.Free
            effect = Effects.GainLife(3)
        }
    }

    private fun TestGame.keptRed(): Int =
        state.getEntity(player1Id)!!.get<ManaPoolComponent>()!!.restrictedMana
            .count { it.color == Color.RED && it.expiry == ManaExpiry.KEPT_UNTIL_END_OF_TURN }

    private fun TestGame.boast() = execute(
        ActivateAbility(
            player1Id,
            findPermanent("Birgi Test Skald")!!,
            cardRegistry.getCard("Birgi Test Skald")!!.script.activatedAbilities[0].id
        )
    )

    private fun baseScenario() = scenario()
        .withPlayers("Birgi", "Opponent")
        .withCardOnBattlefield(1, "Birgi, God of Storytelling")
        .withCardOnBattlefield(1, "Birgi Test Skald")
        .also { b -> repeat(3) { b.withCardInLibrary(1, "Mountain") } }
        .also { b -> repeat(3) { b.withCardInLibrary(2, "Mountain") } }
        .withActivePlayer(1)

    init {
        cardRegistry.register(freeSpell)
        cardRegistry.register(boaster)

        test("casting a spell adds {R} that survives the end of the phase") {
            val game = baseScenario()
                .withCardInHand(1, "Birgi Test Tale")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Birgi Test Tale").error shouldBe null
            game.resolveStack()
            game.keptRed() shouldBe 1

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            withClue("the {R} is kept as steps and phases end") { game.keptRed() shouldBe 1 }
        }

        test("an attacking creature can boast twice during your turn, but not three times") {
            val game = baseScenario().inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS).build()
            game.declareAttackers(mapOf("Birgi Test Skald" to 2))
            val life = game.getLifeTotal(1)

            game.boast().error shouldBe null
            game.resolveStack()
            withClue("the second boast is allowed by Birgi") { game.boast().error shouldBe null }
            game.resolveStack()
            game.getLifeTotal(1) shouldBe life + 6

            withClue("twice, not three times") {
                game.boast().error shouldBe "This ability can only be activated once each turn"
            }
            game.getLifeTotal(1) shouldBe life + 6
        }
    }
}
