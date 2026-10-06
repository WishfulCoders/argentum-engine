package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dmc.cards.TorstenFounderOfBenalia
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Torsten, Founder of Benalia (DMC #47) — {5}{G}{W} Legendary Creature — Human Soldier 7/7.
 *
 *   When Torsten enters, reveal the top seven cards of your library. Put any number of creature
 *   and/or land cards from among them into your hand and the rest on the bottom of your library in
 *   a random order.
 *   When Torsten dies, create seven 1/1 white Soldier creature tokens.
 *
 * Pins the dig's eligibility (creatures and lands only, any number including none), that only seven
 * cards are touched, that everything not kept goes under the untouched rest of the library, and that
 * the death trigger mints seven Soldiers carrying the borrowed Dominaria United token art.
 */
class TorstenFounderOfBenaliaScenarioTest : ScenarioTestBase() {

    private val torsten = "Torsten, Founder of Benalia"

    init {
        cardRegistry.register(TorstenFounderOfBenalia)

        fun TestGame.libraryNames(): List<String> =
            state.getZone(ZoneKey(player1Id, Zone.LIBRARY))
                .mapNotNull { state.getEntity(it)?.get<CardComponent>()?.name }

        fun TestGame.handNames(): List<String> =
            state.getHand(player1Id).mapNotNull { state.getEntity(it)?.get<CardComponent>()?.name }

        fun TestGame.nameOf(id: EntityId): String? = state.getEntity(id)?.get<CardComponent>()?.name

        fun TestGame.castTorstenToSelection(): SelectCardsDecision {
            castSpell(1, torsten).error shouldBe null
            var guard = 0
            while (getPendingDecision() !is SelectCardsDecision && guard++ < 10) resolveStack()
            return getPendingDecision() as? SelectCardsDecision
                ?: error("expected a SelectCardsDecision; got ${getPendingDecision()}")
        }

        fun digScenario(): TestGame = scenario()
            .withPlayers("Benalish", "Opponent")
            .withCardInHand(1, torsten)
            .withLandsOnBattlefield(1, "Forest", 6)
            .withLandsOnBattlefield(1, "Plains", 1)
            // Library, top to bottom: the seven revealed cards, then an eighth that is never seen.
            .withCardInLibrary(1, "Centaur Courser")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(1, "Lightning Bolt")
            .withCardInLibrary(1, "Savannah Lions")
            .withCardInLibrary(1, "Giant Growth")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(1, "Counterspell")
            .withCardInLibrary(1, "Goblin Guide")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("enters: reveals seven, keeps any creatures and lands, bottoms the rest under the library") {
            val game = digScenario()
            val decision = game.castTorstenToSelection()

            withClue("only the creature and land cards among the seven are selectable") {
                decision.options.map { game.nameOf(it) } shouldContainExactlyInAnyOrder
                    listOf("Centaur Courser", "Forest", "Savannah Lions", "Plains")
                decision.nonSelectableOptions.map { game.nameOf(it) } shouldContainExactlyInAnyOrder
                    listOf("Lightning Bolt", "Giant Growth", "Counterspell")
                decision.minSelections shouldBe 0
            }

            game.selectCards(
                decision.options.filter { game.nameOf(it) in setOf("Centaur Courser", "Forest", "Plains") }
            ).error shouldBe null
            game.resolveStack()

            withClue("the chosen creature and lands went to hand") {
                game.handNames() shouldContainExactlyInAnyOrder listOf("Centaur Courser", "Forest", "Plains")
            }
            val library = game.libraryNames()
            withClue("the unseen eighth card is now on top; the four unkept cards are beneath it") {
                library.first() shouldBe "Goblin Guide"
                library.drop(1) shouldContainExactlyInAnyOrder
                    listOf("Lightning Bolt", "Savannah Lions", "Giant Growth", "Counterspell")
            }
            game.findPermanents(torsten).size shouldBe 1
        }

        test("enters: keeping nothing bottoms all seven") {
            val game = digScenario()
            game.castTorstenToSelection()
            game.skipSelection().error shouldBe null
            game.resolveStack()

            game.handNames() shouldBe emptyList()
            val library = game.libraryNames()
            library.first() shouldBe "Goblin Guide"
            library.size shouldBe 8
        }

        test("dies: creates seven 1/1 white Soldiers with Dominaria United's Soldier art") {
            val game = scenario()
                .withPlayers("Benalish", "Opponent")
                .withCardOnBattlefield(1, torsten)
                .withCardInHand(1, "Doom Blade")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Doom Blade", game.findPermanent(torsten)!!).error shouldBe null
            game.resolveStack()

            withClue("Torsten died") {
                game.isInGraveyard(1, torsten) shouldBe true
            }
            val soldiers = game.findPermanents("Soldier Token")
            soldiers.size shouldBe 7
            for (id in soldiers) {
                val card = game.state.getEntity(id)!!.get<CardComponent>()!!
                card.baseStats?.basePower shouldBe 1
                card.baseStats?.baseToughness shouldBe 1
                card.colors shouldBe setOf(Color.WHITE)
                withClue("the DMU Soldier (tdmu) borrowed through DominariaUnitedCommanderSet.tokenArt") {
                    card.imageUri!! shouldContain "8c4b0257-2ca5-4015-9d63-d7cf6e87ab9d"
                }
            }
        }
    }
}
