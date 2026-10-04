package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.GameLimits
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.chk.cards.KikiJikiMirrorBreaker
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * [GameLimits.MAX_TOKENS_ON_BATTLEFIELD]: once the battlefield holds that many tokens, an effect that
 * would create another creates nothing (and the effect otherwise resolves normally).
 */
class TokenBattlefieldCapScenarioTest : ScenarioTestBase() {

    private val kikiAbility = KikiJikiMirrorBreaker.activatedAbilities.single().id

    private fun boardWithTokens(n: Int): TestGame {
        var b = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Kiki-Jiki, Mirror Breaker")
            .withCardOnBattlefield(1, "Pestermite")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        // Split between the players: the cap counts every token on the battlefield.
        repeat(n) { i -> b = b.withCardOnBattlefield(1 + i % 2, "Grizzly Bears", isToken = true) }
        return b.build()
    }

    private fun TestGame.tokens(): Int = state.getBattlefield().count { state.getEntity(it)?.has<TokenComponent>() == true }

    private fun TestGame.copyPestermite() {
        val kiki = findPermanent("Kiki-Jiki, Mirror Breaker")!!
        val pestermite = findPermanents("Pestermite").first { state.getEntity(it)?.has<TokenComponent>() != true }
        execute(ActivateAbility(player1Id, kiki, kikiAbility, targets = listOf(ChosenTarget.Permanent(pestermite)))).error shouldBe null
        resolveStack()
    }

    init {
        test("one below the cap, a token is still created") {
            val game = boardWithTokens(GameLimits.MAX_TOKENS_ON_BATTLEFIELD - 1)
            game.copyPestermite()
            game.tokens() shouldBe GameLimits.MAX_TOKENS_ON_BATTLEFIELD
        }

        test("at the cap, the copy is not created and the ability still resolves") {
            val game = boardWithTokens(GameLimits.MAX_TOKENS_ON_BATTLEFIELD)
            game.copyPestermite()
            withClue("no token past the cap") {
                game.tokens() shouldBe GameLimits.MAX_TOKENS_ON_BATTLEFIELD
                game.findPermanents("Pestermite").size shouldBe 1
            }
            game.state.stack.isEmpty() shouldBe true
        }
    }
}
