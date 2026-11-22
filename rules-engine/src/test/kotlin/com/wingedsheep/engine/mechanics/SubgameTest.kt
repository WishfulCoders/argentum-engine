package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.Concede
import com.wingedsheep.engine.core.KeepHand
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SubgameEndedEvent
import com.wingedsheep.engine.core.SubgameStartedEvent
import com.wingedsheep.engine.core.TakeMulligan
import com.wingedsheep.engine.core.subgameDepth
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.player.MulliganStateComponent
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Magic subgames (CR 729) — Shahrazad, here as a test card: "Players play a Magic subgame, using
 * their libraries as their decks. Each player who doesn't win the subgame loses half their life,
 * rounded up."
 */
class SubgameTest : FunSpec({

    val shahrazad = card("Test Shahrazad") {
        manaCost = "{W}{W}"
        typeLine = "Sorcery"
        oracleText = "Players play a Magic subgame, using their libraries as their decks. Each player " +
            "who doesn't win the subgame loses half their life, rounded up."
        spell {
            effect = Effects.PlaySubgame { nonWinners ->
                Effects.ForEachPlayer(nonWinners.asPlayers, Effects.LoseHalfLife(roundUp = true))
            }
        }
    }

    val deckSize = 40

    fun driver(playerOneDeck: Int = deckSize, playerTwoDeck: Int = deckSize): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + shahrazad)
        initGame(Deck.of("Island" to playerOneDeck), Deck.of("Forest" to playerTwoDeck))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** [caster] casts the test Shahrazad, and both players pass so it resolves. */
    fun GameTestDriver.castShahrazad(caster: EntityId): EntityId {
        val spell = putCardInHand(caster, "Test Shahrazad")
        giveMana(caster, Color.WHITE, 2)
        castSpell(caster, spell).outcome shouldBe Outcome.Done
        bothPass()
        return spell
    }

    fun GameTestDriver.namesIn(playerId: EntityId, zone: Zone) =
        state.getZone(ZoneKey(playerId, zone)).mapNotNull { state.getEntity(it)?.get<CardComponent>()?.name }

    fun GameTestDriver.mainGame() = state.mainGame!!

    test("CR 729.2: the subgame starts with each player's library as their deck, and the main game waits") {
        val d = driver()
        val bears = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        d.putCardInGraveyard(d.player1, "Lightning Bolt")
        d.setLifeTotal(d.player1, 13)
        val libraryBefore = mapOf(
            d.player1 to d.state.getLibrary(d.player1).size,
            d.player2 to d.state.getLibrary(d.player2).size,
        )
        val handBefore = d.getHandSize(d.player2)

        val spell = d.castShahrazad(d.player1)

        val started = d.events.filterIsInstance<SubgameStartedEvent>().single()
        started.depth shouldBe 1
        started.sourceName shouldBe "Test Shahrazad"
        d.state.subgameDepth shouldBe 1
        val main = d.mainGame()

        withClue("A fresh game: turn 1, starting life, empty battlefield and graveyards (CR 729.2)") {
            d.state.turnNumber shouldBe 1
            d.getLifeTotal(d.player1) shouldBe 20
            d.getLifeTotal(d.player2) shouldBe 20
            d.state.getBattlefield().shouldBeEmpty()
            d.getGraveyard(d.player1).shouldBeEmpty()
            d.state.stack.shouldBeEmpty()
        }
        for (player in listOf(d.player1, d.player2)) {
            withClue("Each subgame deck is exactly that player's main-game library") {
                d.getHandSize(player) shouldBe 7
                d.getHandSize(player) + d.state.getLibrary(player).size shouldBe libraryBefore.getValue(player)
            }
            withClue("…which has left the main game") {
                main.getLibrary(player).shouldBeEmpty()
            }
        }
        withClue("The first player is chosen at random from the players in the game") {
            d.state.activePlayerId shouldBe started.startingPlayerId
        }
        withClue("The main game is discontinued exactly where it was, Shahrazad still resolving") {
            main.stack shouldBe listOf(spell)
            main.getBattlefield() shouldBe listOf(bears)
            main.getGraveyard(d.player1).size shouldBe 1
            main.getEntity(d.player1)?.get<com.wingedsheep.engine.state.components.identity.LifeTotalComponent>()?.life shouldBe 13
            main.getHand(d.player2).size shouldBe handBefore
        }
    }

    test("CR 729.5: when the subgame ends, its cards go home, and each player who didn't win loses half their life, rounded up") {
        val d = driver()
        d.setLifeTotal(d.player2, 15)
        val libraryBefore = d.state.getLibrary(d.player2).size
        val spell = d.castShahrazad(d.player1)

        d.concede(d.player2).outcome shouldBe Outcome.Done

        val ended = d.events.filterIsInstance<SubgameEndedEvent>().single()
        ended.winnerIds shouldBe listOf(d.player1)
        ended.nonWinnerIds shouldBe listOf(d.player2)
        withClue("Back in the main game, which isn't over") {
            d.state.mainGame shouldBe null
            d.state.subgameDepth shouldBe 0
            d.state.gameOver shouldBe false
            d.state.getEntity(d.player2)?.has<PlayerLostComponent>() shouldBe false
        }
        withClue("The loser loses half of 15, rounded up; the winner loses nothing") {
            d.getLifeTotal(d.player2) shouldBe 7
            d.getLifeTotal(d.player1) shouldBe 20
        }
        withClue("Every card of the subgame, hand included, is back in its owner's main-game library") {
            d.state.getLibrary(d.player2).size shouldBe libraryBefore
            d.namesIn(d.player2, Zone.LIBRARY).all { it == "Forest" } shouldBe true
        }
        withClue("Shahrazad finished resolving only now, and is in its owner's graveyard") {
            d.state.stack.shouldBeEmpty()
            d.state.continuationStack.shouldBeEmpty()
            d.getGraveyardCardNames(d.player1) shouldBe listOf("Test Shahrazad")
            d.getGraveyard(d.player1) shouldBe listOf(spell)
        }
        withClue("The main game carries on in the step it was in") {
            d.state.step shouldBe Step.PRECOMBAT_MAIN
            d.state.priorityPlayerId shouldBe d.player1
        }
    }

    test("CR 729.5: cards on the subgame battlefield, in its graveyard and exile go to their owners' main libraries; tokens cease to exist") {
        val d = driver()
        val libraryBefore = d.state.getLibrary(d.player1).size
        d.castShahrazad(d.player1)

        val lands = d.state.getHand(d.player1).take(3)
        var sub = d.state
        val (onField, inGraveyard, inExile) = lands
        sub = sub.removeFromZone(ZoneKey(d.player1, Zone.HAND), onField).addToZone(ZoneKey(d.player1, Zone.BATTLEFIELD), onField)
        sub = sub.removeFromZone(ZoneKey(d.player1, Zone.HAND), inGraveyard).addToZone(ZoneKey(d.player1, Zone.GRAVEYARD), inGraveyard)
        sub = sub.removeFromZone(ZoneKey(d.player1, Zone.HAND), inExile).addToZone(ZoneKey(d.player1, Zone.EXILE), inExile)
        d.replaceState(sub)
        d.addComponent(onField, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
        val token = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")
        d.addComponent(token, TokenComponent)

        d.concede(d.player2).outcome shouldBe Outcome.Done

        d.state.mainGame shouldBe null
        d.state.getLibrary(d.player1).size shouldBe libraryBefore
        d.namesIn(d.player1, Zone.LIBRARY).count { it == "Savannah Lions" } shouldBe 0
        withClue("Nothing of the subgame survives in the main game") {
            d.state.getBattlefield().shouldBeEmpty()
            d.getExile(d.player1).shouldBeEmpty()
            d.state.entities.values.none { it.has<CountersComponent>() } shouldBe true
        }
    }

    test("CR 729.3: a player with fewer than seven cards in their library loses the subgame at its first upkeep") {
        // Seven cards go to the opening hand, so player 2's main-game library holds only five.
        val d = driver(playerTwoDeck = 12)
        d.setLifeTotal(d.player2, 9)
        d.castShahrazad(d.player1)

        withClue("The subgame was over before anyone got priority in it") {
            d.state.mainGame shouldBe null
        }
        d.events.filterIsInstance<SubgameEndedEvent>().single().winnerIds shouldBe listOf(d.player1)
        d.getLifeTotal(d.player2) shouldBe 4
        d.getLifeTotal(d.player1) shouldBe 20
        d.state.getLibrary(d.player2).size shouldBe 5
    }

    test("a drawn subgame has no winner, so every player loses half their life") {
        val d = driver(playerOneDeck = 12, playerTwoDeck = 12)
        d.setLifeTotal(d.player1, 20)
        d.setLifeTotal(d.player2, 1)
        d.castShahrazad(d.player1)

        val ended = d.events.filterIsInstance<SubgameEndedEvent>().single()
        ended.winnerIds.shouldBeEmpty()
        ended.nonWinnerIds.shouldContainExactlyInAnyOrder(d.player1, d.player2)
        d.getLifeTotal(d.player1) shouldBe 10
        withClue("Half of 1, rounded up, is 1: the main game's state-based actions then end it") {
            d.getLifeTotal(d.player2) shouldBe 0
            d.state.gameOver shouldBe true
            d.state.winnerId shouldBe d.player1
        }
    }

    test("CR 103.5: the subgame has its own mulligans") {
        val d = driver()
        for (player in listOf(d.player1, d.player2)) {
            d.addComponent(player, d.state.getEntity(player)!!.get<MulliganStateComponent>()!!.copy(skipped = false))
        }
        d.castShahrazad(d.player1)

        d.state.mainGame shouldNotBe null
        for (player in listOf(d.player1, d.player2)) {
            d.state.getEntity(player)?.get<MulliganStateComponent>()?.hasKept shouldBe false
        }
        d.submit(TakeMulligan(d.player2)).outcome shouldBe Outcome.Done
        d.submit(KeepHand(d.player1)).outcome shouldBe Outcome.Done
        d.submit(KeepHand(d.player2)).outcome shouldBe Outcome.Done
        d.submit(com.wingedsheep.engine.core.BottomCards(d.player2, d.state.getHand(d.player2).take(1))).outcome shouldBe Outcome.Done
        d.getHandSize(d.player2) shouldBe 6
        d.state.step shouldBe Step.UPKEEP
        withClue("The main game keeps its own record: its opening hands were kept long ago") {
            d.mainGame().getEntity(d.player2)?.get<MulliganStateComponent>()?.mulligansTaken shouldBe 0
            d.mainGame().getEntity(d.player2)?.get<MulliganStateComponent>()?.hasKept shouldBe true
        }
    }

    test("CR 104.3a: conceding the subgame and conceding the whole game are different") {
        val d = driver()
        d.castShahrazad(d.player1)

        d.submit(Concede(d.player2, includingMainGames = true)).outcome shouldBe Outcome.Done

        withClue("The subgame ended, then the main game too, with player 2 out of both") {
            d.state.mainGame shouldBe null
            d.state.gameOver shouldBe true
            d.state.winnerId shouldBe d.player1
        }
    }

    test("CR 729.6: a subgame can be created within a subgame") {
        val d = driver()
        d.castShahrazad(d.player1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val inSubgame = d.state.activePlayerId!!
        val libraryInSubgame = d.state.getLibrary(d.player2).size

        d.castShahrazad(inSubgame)

        d.state.subgameDepth shouldBe 2
        d.events.filterIsInstance<SubgameStartedEvent>().last().depth shouldBe 2
        withClue("The inner subgame's decks are the libraries of the first subgame") {
            d.getHandSize(d.player2) + d.state.getLibrary(d.player2).size shouldBe libraryInSubgame
        }

        d.concede(d.player2).outcome shouldBe Outcome.Done
        withClue("Back in the first subgame, which goes on; player 2 lost half its 20 life there") {
            d.state.subgameDepth shouldBe 1
            d.getLifeTotal(d.player2) shouldBe 10
            d.state.gameOver shouldBe false
        }
        d.concede(d.player2).outcome shouldBe Outcome.Done
        d.state.subgameDepth shouldBe 0
        d.getLifeTotal(d.player2) shouldBe 10
    }

    test("the subgame round-trips through serialization with the main game inside it") {
        val d = driver()
        d.castShahrazad(d.player1)
        val json = kotlinx.serialization.json.Json {
            serializersModule = com.wingedsheep.engine.core.engineSerializersModule; allowStructuredMapKeys = true
        }
        val encoded = json.encodeToString(com.wingedsheep.engine.state.GameState.serializer(), d.state)
        val decoded = json.decodeFromString(com.wingedsheep.engine.state.GameState.serializer(), encoded)
        decoded.mainGame shouldBe d.state.mainGame
        decoded.mainGame?.pendingSubgame shouldBe d.state.mainGame?.pendingSubgame
    }
})
