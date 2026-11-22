package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Rivaz of the Claw (DMU #215) — {1}{B}{R} Legendary Creature — Lizard Warlock, 3/3.
 *
 * "Once during each of your turns, you may cast a Dragon creature spell from your graveyard.
 *  Whenever you cast a Dragon creature spell from your graveyard, it gains 'When this creature
 *  dies, exile it.'"
 *
 * The cast trigger grants the dies-exile ability to the *spell*; the test pins that it survives the
 * spell becoming a permanent, that a Dragon cast from hand gets nothing, and the once-per-turn
 * allowance. Rivaz starts tapped so the auto-payer never reaches for its restricted mana.
 */
class RivazOfTheClawScenarioTest : ScenarioTestBase() {

    private val dragon = card("Rivaz Test Dragon") {
        manaCost = "{R}"
        typeLine = "Creature — Dragon"
        power = 2
        toughness = 2
    }
    private val otherDragon = card("Rivaz Test Wyrm") {
        manaCost = "{R}"
        typeLine = "Creature — Dragon"
        power = 2
        toughness = 2
    }

    init {
        cardRegistry.register(dragon)
        cardRegistry.register(otherDragon)

        fun builder() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Rivaz of the Claw", tapped = true)
            .withLandsOnBattlefield(1, "Mountain", 2)
            .withLandsOnBattlefield(1, "Swamp", 2)
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("a Dragon cast from the graveyard gains the dies-exile ability and is exiled when it dies") {
            val game = builder().withCardInGraveyard(1, "Rivaz Test Dragon").withCardInHand(1, "Doom Blade").build()

            game.castSpellFromGraveyard(1, "Rivaz Test Dragon").error shouldBe null
            game.resolveStack()
            val dragonId = game.findPermanent("Rivaz Test Dragon")
            dragonId shouldNotBe null

            game.castSpell(1, "Doom Blade", dragonId).error shouldBe null
            game.resolveStack()

            withClue("the granted 'When this creature dies, exile it.' fired") {
                game.isInExile(1, "Rivaz Test Dragon") shouldBe true
                game.isInGraveyard(1, "Rivaz Test Dragon") shouldBe false
            }
        }

        test("a Dragon cast from hand gains nothing and stays in the graveyard when it dies") {
            val game = builder().withCardInHand(1, "Rivaz Test Dragon").withCardInHand(1, "Doom Blade").build()

            game.castSpell(1, "Rivaz Test Dragon").error shouldBe null
            game.resolveStack()
            val dragonId = game.findPermanent("Rivaz Test Dragon")!!

            game.castSpell(1, "Doom Blade", dragonId).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Rivaz Test Dragon") shouldBe true
            game.isInExile(1, "Rivaz Test Dragon") shouldBe false
        }

        test("only one Dragon may be cast from the graveyard each turn, and only a Dragon") {
            val game = builder()
                .withCardInGraveyard(1, "Rivaz Test Dragon")
                .withCardInGraveyard(1, "Rivaz Test Wyrm")
                .withCardInGraveyard(1, "Grizzly Bears")
                .build()

            withClue("a non-Dragon creature card is not covered") {
                game.castSpellFromGraveyard(1, "Grizzly Bears").error shouldNotBe null
            }
            game.castSpellFromGraveyard(1, "Rivaz Test Dragon").error shouldBe null
            game.resolveStack()

            withClue("the turn's allowance is spent") {
                game.castSpellFromGraveyard(1, "Rivaz Test Wyrm").error shouldNotBe null
            }
        }
    }
}
