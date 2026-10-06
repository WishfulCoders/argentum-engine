package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.OpponentsCantMakeYouDiscard
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** "Spells and abilities your opponents control can't cause you to discard cards." */
private val DiscardWard = card("Test Discard Ward") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "Spells and abilities your opponents control can't cause you to discard cards."
    staticAbility { ability = OpponentsCantMakeYouDiscard }
    metadata { rarity = Rarity.RARE; collectorNumber = "T1" }
}

/** Mogis-style punisher: "Target opponent loses 5 life unless they sacrifice a creature." */
private val SacrificeToll = card("Test Sacrifice Toll") {
    manaCost = "{0}"
    typeLine = "Sorcery"
    oracleText = "Target opponent loses 5 life unless they sacrifice a creature."
    spell {
        val t = target(Targets.Opponent)
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.Sacrifice(GameObjectFilter.Creature),
            suffer = Effects.LoseLife(5, target = t),
            player = t,
        )
    }
    metadata { rarity = Rarity.COMMON; collectorNumber = "T2" }
}

/** A pipeline sacrifice: "Each player sacrifices all creatures." */
private val CreatureCull = card("Test Creature Cull") {
    manaCost = "{0}"
    typeLine = "Sorcery"
    oracleText = "Each player sacrifices all creatures they control."
    spell { effect = Patterns.Group.sacrificeAllPipeline(GameObjectFilter.Creature) }
    metadata { rarity = Rarity.COMMON; collectorNumber = "T3" }
}

/**
 * [OpponentsCantMakeYouDiscard] (and, for the sites the two share, Sigarda's
 * `OpponentsCantMakeYouSacrifice`): a player-scoped "can't" (CR 101.2) — an opponent's instruction to
 * discard does nothing, the rest of that spell or ability still happens, and an option to discard or
 * sacrifice it offers can't be taken (the Tamiyo, Collector of Tales rulings). The player's own
 * spells are unaffected.
 */
class OpponentsCantMakeYouDiscardTest : ScenarioTestBase() {

    init {
        cardRegistry.register(listOf(DiscardWard, SacrificeToll, CreatureCull))

        fun base() = scenario()
            .withPlayers("Player1", "Player2")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("an opponent's \"target player discards two cards\" does nothing") {
            val game = base()
                .withCardOnBattlefield(2, "Test Discard Ward")
                .withCardInHand(1, "Mind Rot")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Hill Giant")
                .build()

            game.castSpellTargetingPlayer(1, "Mind Rot", 2).error shouldBe null
            game.resolveStack()
            game.handSize(2) shouldBe 2
            game.findCardsInGraveyard(2, "Grizzly Bears").size shouldBe 0
        }

        test("a random discard does nothing either") {
            val game = base()
                .withCardOnBattlefield(2, "Test Discard Ward")
                .withCardInHand(1, "Hymn to Tourach")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Hill Giant")
                .build()

            game.castSpellTargetingPlayer(1, "Hymn to Tourach", 2).error shouldBe null
            game.resolveStack()
            game.handSize(2) shouldBe 2
        }

        test("the rest of the spell still happens: Thoughtseize reveals and costs its caster 2 life") {
            val game = base()
                .withCardOnBattlefield(2, "Test Discard Ward")
                .withCardInHand(1, "Thoughtseize")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardInHand(2, "Shock")
                .withLifeTotal(1, 20)
                .build()

            game.castSpellTargetingPlayer(1, "Thoughtseize", 2).error shouldBe null
            game.resolveStack()
            if (game.hasPendingDecision()) {
                game.selectCards(game.findCardsInHand(2, "Shock"))
                game.resolveStack()
            }
            withClue("the chosen card is not discarded") { game.isInHand(2, "Shock") shouldBe true }
            game.getLifeTotal(1) shouldBe 18
        }

        test("the controller's own spell still makes them discard") {
            val game = base()
                .withActivePlayer(2)
                .withCardOnBattlefield(2, "Test Discard Ward")
                .withCardInHand(2, "Mind Rot")
                .withLandsOnBattlefield(2, "Swamp", 3)
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Hill Giant")
                .build()

            game.castSpellTargetingPlayer(2, "Mind Rot", 2).error shouldBe null
            game.resolveStack()
            if (game.hasPendingDecision()) {
                game.selectCards(game.state.getHand(game.player2Id).take(2))
                game.resolveStack()
            }
            game.handSize(2) shouldBe 0
        }

        test("\"unless they discard a card\" can't be chosen, so the consequence happens (Painful Quandary)") {
            val game = base()
                .withActivePlayer(2)
                .withCardOnBattlefield(1, "Painful Quandary")
                .withCardOnBattlefield(2, "Test Discard Ward")
                .withCardInHand(2, "Shock")
                .withCardInHand(2, "Grizzly Bears")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withLifeTotal(2, 20)
                .build()

            game.castSpellTargetingPlayer(2, "Shock", 1).error shouldBe null
            game.resolveStack()
            withClue("no discard prompt was offered") { game.isInHand(2, "Grizzly Bears") shouldBe true }
            game.getLifeTotal(2) shouldBe 15
        }

        test("\"unless they sacrifice a creature\" can't be chosen under Sigarda") {
            val game = base()
                .withCardOnBattlefield(2, "Sigarda, Host of Herons")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Test Sacrifice Toll")
                .withLifeTotal(2, 20)
                .build()

            game.castSpellTargetingPlayer(1, "Test Sacrifice Toll", 2).error shouldBe null
            game.resolveStack()
            (game.findPermanent("Grizzly Bears") != null) shouldBe true
            game.getLifeTotal(2) shouldBe 15
        }

        test("a pipeline sacrifice skips the protected player but not the caster") {
            val game = base()
                .withCardOnBattlefield(2, "Sigarda, Host of Herons")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Test Creature Cull")
                .build()

            game.castSpell(1, "Test Creature Cull").error shouldBe null
            game.resolveStack()
            withClue("player 2's creatures stay") {
                (game.findPermanent("Hill Giant") != null) shouldBe true
                (game.findPermanent("Sigarda, Host of Herons") != null) shouldBe true
            }
            withClue("the caster's own creature is sacrificed") { game.findPermanent("Grizzly Bears") shouldBe null }
        }
    }
}
