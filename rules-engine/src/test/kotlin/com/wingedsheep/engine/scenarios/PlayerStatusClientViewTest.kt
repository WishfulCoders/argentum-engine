package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * The client view exposes status the player otherwise has to track in their head: per-card Delirium
 * progress (distinct card types in the controller's graveyard, active at 4+) and the effective
 * maximum hand size (CR 402.2).
 *
 * Delirium rides on [com.wingedsheep.engine.view.ClientCard.deliriumInfo] and is populated only on
 * cards whose definition actually gates on delirium — detected by walking the card's serialized
 * tree, so it works wherever the gate lives (static ability, activated/triggered ability, spell
 * effect, cost reduction, replacement effect), not just static abilities. The client renders it as
 * a per-card badge so the count shows up only on the cards it matters for.
 *
 * Max hand size rides on [com.wingedsheep.engine.view.ClientPlayer] and comes from the shared
 * [com.wingedsheep.engine.core.MaximumHandSize] source of truth, so what the badge shows is exactly
 * what cleanup will enforce (see CursedRackScenarioTest for the cleanup-side discard behavior).
 */
class PlayerStatusClientViewTest : ScenarioTestBase() {

    private val transformer = com.wingedsheep.engine.view.ClientStateTransformer(cardRegistry, predicateEvaluator = services.predicateEvaluator)
    private val p1 = EntityId.of("player-1")

    private fun viewSelf(state: com.wingedsheep.engine.state.GameState) =
        transformer.transform(state, p1).players.first { it.playerId == p1 }

    private fun cardNamed(state: com.wingedsheep.engine.state.GameState, name: String) =
        transformer.transform(state, p1).cards.values.first { it.name == name }

    init {
        test("a delirium card shows its progress toward the four-card-type threshold") {
            // Two distinct types in the graveyard (Creature, Instant) — delirium not yet active.
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Winter, Misanthropic Guide")
                .withCardInGraveyard(1, "Grizzly Bears")   // Creature
                .withCardInGraveyard(1, "Grizzly Bears")   // Creature (same type — counts once)
                .withCardInGraveyard(1, "Lightning Bolt")  // Instant
                .build()

            val info = cardNamed(game.state, "Winter, Misanthropic Guide").deliriumInfo
            info.shouldNotBeNull()
            info.current shouldBe 2
            info.required shouldBe 4
            info.active shouldBe false
        }

        test("a delirium card's badge turns active at four distinct card types") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Winter, Misanthropic Guide")
                .withCardInGraveyard(1, "Grizzly Bears")   // Creature
                .withCardInGraveyard(1, "Lightning Bolt")  // Instant
                .withCardInGraveyard(1, "Pacifism")        // Enchantment
                .withCardInGraveyard(1, "Divination")      // Sorcery
                .build()

            val info = cardNamed(game.state, "Winter, Misanthropic Guide").deliriumInfo
            info.shouldNotBeNull()
            info.current shouldBe 4
            info.active shouldBe true
        }

        test("cards that don't care about delirium carry no delirium badge") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Lightning Bolt")
                .withCardInGraveyard(1, "Pacifism")
                .build()

            withClue("Grizzly Bears has no delirium-gated ability") {
                cardNamed(game.state, "Grizzly Bears").deliriumInfo.shouldBeNull()
            }
        }

        test("delirium is detected outside static abilities (spell effect and activated ability)") {
            // Demonic Counsel gates a *spell effect* on delirium; Balustrade Wurm gates an
            // *activated ability* on it. Neither is a static ability, so this proves the
            // full-tree detection rather than a static-ability-only scan.
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Demonic Counsel")
                .withCardInGraveyard(1, "Balustrade Wurm")
                .build()

            withClue("Demonic Counsel's delirium gate lives in its spell effect") {
                cardNamed(game.state, "Demonic Counsel").deliriumInfo.shouldNotBeNull()
            }
            withClue("Balustrade Wurm's delirium gate lives in an activated ability") {
                cardNamed(game.state, "Balustrade Wurm").deliriumInfo.shouldNotBeNull()
            }
        }

        test("maximum hand size is the default seven with no effect in play") {
            val game = scenario().withPlayers("Player1", "Player2").build()
            viewSelf(game.state).maxHandSize shouldBe 7
        }

        test("Reliquary Tower reports no maximum hand size (null = unlimited)") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Reliquary Tower")
                .build()

            withClue("Reliquary Tower grants 'You have no maximum hand size'") {
                viewSelf(game.state).maxHandSize.shouldBeNull()
            }
        }

        test("an opponent's Reliquary Tower does not lift the viewing player's maximum") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(2, "Reliquary Tower")
                .build()

            viewSelf(game.state).maxHandSize shouldBe 7
        }
    }
}
