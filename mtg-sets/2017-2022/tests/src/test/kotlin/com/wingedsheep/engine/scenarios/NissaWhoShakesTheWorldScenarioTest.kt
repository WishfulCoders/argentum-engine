package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Nissa, Who Shakes the World (WAR #169, loyalty 5).
 *
 * Pins the Forest mana doubler (one Forest pays {G}{G}), the +1 animation (counters, untap, a
 * vigilance/haste 0/0 Elemental land — so 3/3), and the −8 emblem plus the any-number Forest
 * search.
 */
class NissaWhoShakesTheWorldScenarioTest : ScenarioTestBase() {

    init {
        context("Nissa, Who Shakes the World") {
            test("a Forest you tap for mana adds an additional {G}") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Nissa, Who Shakes the World")
                    .withCardOnBattlefield(1, "Forest")
                    .withCardInHand(1, "People of the Woods")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "People of the Woods")
                withClue("one Forest pays {G}{G}: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()
                game.isOnBattlefield("People of the Woods") shouldBe true
            }

            test("+1 untaps a land and makes it a 3/3 vigilance, haste Elemental land, permanently") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Nissa, Who Shakes the World")
                    .withCardOnBattlefield(1, "Mountain", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val nissa = game.findPermanent("Nissa, Who Shakes the World")!!
                val mountain = game.findPermanent("Mountain")!!
                setLoyalty(game, nissa, 5)
                activate(game, nissa, index = 0, targets = listOf(mountain))
                game.resolveStack()

                val projected = game.state.projectedState
                withClue("the Mountain is an untapped 3/3 Elemental creature land with vigilance and haste") {
                    game.state.getEntity(mountain)?.has<TappedComponent>() shouldBe false
                    projected.isCreature(mountain) shouldBe true
                    projected.hasType(mountain, "LAND") shouldBe true
                    projected.hasSubtype(mountain, "Elemental") shouldBe true
                    projected.getPower(mountain) shouldBe 3
                    projected.getToughness(mountain) shouldBe 3
                    projected.hasKeyword(mountain, Keyword.VIGILANCE) shouldBe true
                    projected.hasKeyword(mountain, Keyword.HASTE) shouldBe true
                }
                loyalty(game, nissa) shouldBe 6

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                withClue("the animation doesn't wear off at cleanup") {
                    game.state.projectedState.isCreature(mountain) shouldBe true
                }
            }

            test("−8 gives lands indestructible and puts any number of Forests onto the battlefield tapped") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Nissa, Who Shakes the World")
                    .withCardOnBattlefield(1, "Mountain")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val nissa = game.findPermanent("Nissa, Who Shakes the World")!!
                setLoyalty(game, nissa, 8)
                activate(game, nissa, index = 1)
                game.resolveStack()

                val forests = game.findCardsInLibrary(1, "Forest")
                forests.size shouldBe 3
                game.selectCards(forests).error shouldBe null
                game.resolveStack()

                val onBattlefield = game.findAllPermanents("Forest")
                withClue("all three Forests entered tapped") {
                    onBattlefield.size shouldBe 3
                    onBattlefield.all { game.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe true
                }
                val projected = game.state.projectedState
                withClue("lands you control have indestructible — old and new") {
                    projected.hasKeyword(game.findPermanent("Mountain")!!, Keyword.INDESTRUCTIBLE) shouldBe true
                    onBattlefield.all { projected.hasKeyword(it, Keyword.INDESTRUCTIBLE) } shouldBe true
                }
                game.isInHand(1, "Grizzly Bears") shouldBe false
            }
        }
    }

    private fun activate(game: TestGame, source: EntityId, index: Int, targets: List<EntityId> = emptyList()) {
        val ability = cardRegistry.getCard("Nissa, Who Shakes the World")!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = source,
                abilityId = ability.id,
                targets = targets.map { ChosenTarget.Permanent(it) }
            )
        ).error shouldBe null
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }
}
