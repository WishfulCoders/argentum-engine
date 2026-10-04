package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Touch the Spirit Realm (NEO #40) — {2}{W} Enchantment.
 *
 * When this enchantment enters, exile up to one target artifact or creature until this enchantment
 * leaves the battlefield.
 * Channel — {1}{W}, Discard this card: Exile target artifact or creature. Return it to the
 * battlefield under its owner's control at the beginning of the next end step.
 */
class TouchTheSpiritRealmScenarioTest : ScenarioTestBase() {

    private fun channelAbilityId() = cardRegistry.getCard("Touch the Spirit Realm")!!
        .activatedAbilities.first { it.activateFromZone == Zone.HAND }.id

    init {
        context("Touch the Spirit Realm") {

            test("enters: exiles a creature until the enchantment leaves") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Touch the Spirit Realm")
                    .withCardInHand(1, "Disenchant")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withCardOnBattlefield(2, "Serra Angel")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val angel = game.findPermanent("Serra Angel")!!
                game.castSpell(1, "Touch the Spirit Realm").error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(angel)).error shouldBe null
                game.resolveStack()

                withClue("Serra Angel is exiled") {
                    game.isOnBattlefield("Serra Angel") shouldBe false
                    game.isInExile(2, "Serra Angel") shouldBe true
                }

                val touch = game.findPermanent("Touch the Spirit Realm")!!
                game.castSpell(1, "Disenchant", touch).error shouldBe null
                game.resolveStack()

                withClue("the enchantment leaving returns the Angel") {
                    game.isOnBattlefield("Serra Angel") shouldBe true
                }
            }

            test("channel: exiles a creature and returns it at the next end step") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Touch the Spirit Realm")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Serra Angel")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val angel = game.findPermanent("Serra Angel")!!
                val handCard = game.findCardsInHand(1, "Touch the Spirit Realm").first()
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = handCard,
                        abilityId = channelAbilityId(),
                        targets = listOf(ChosenTarget.Permanent(angel)),
                    )
                )
                withClue("channel activates: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("the card was discarded as a cost and the Angel is exiled") {
                    game.isInGraveyard(1, "Touch the Spirit Realm") shouldBe true
                    game.isInExile(2, "Serra Angel") shouldBe true
                    game.isOnBattlefield("Serra Angel") shouldBe false
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("the Angel returns at the beginning of the end step") {
                    game.isOnBattlefield("Serra Angel") shouldBe true
                }
            }
        }
    }
}
