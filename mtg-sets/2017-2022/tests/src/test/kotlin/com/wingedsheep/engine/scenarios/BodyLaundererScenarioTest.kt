package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Body Launderer (SNC #68) — {2}{B}{B} Creature — Ogre Rogue, 3/3, deathtouch.
 *
 * "Whenever another nontoken creature you control dies, this creature connives.
 *  When this creature dies, return another target non-Rogue creature card with equal or lesser
 *  power from your graveyard to the battlefield."
 *
 * Pins the connive trigger off another creature's death, and that the dies trigger's power cap
 * reads Body Launderer's last-known power (ruling 2022-04-29), excludes Rogues and the Launderer
 * itself.
 */
class BodyLaundererScenarioTest : ScenarioTestBase() {

    init {
        test("another nontoken creature you control dying makes Body Launderer connive") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Body Launderer")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Murder")
                .withCardInHand(1, "Craw Wurm")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val launderer = game.findPermanent("Body Launderer")!!
            val wurm = game.findCardsInHand(1, "Craw Wurm").single()

            game.castSpell(1, "Murder", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(listOf(wurm))
            game.resolveStack()

            game.isInHand(1, "Island") shouldBe true
            game.isInGraveyard(1, "Craw Wurm") shouldBe true
            game.state.getEntity(launderer)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        }

        test("dies trigger uses last-known power and skips Rogues and itself") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Body Launderer")
                .withCardInHand(1, "Giant Growth")
                .withCardInHand(1, "Murder")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInGraveyard(1, "Craw Wurm")      // power 6 — legal only via last-known 6
                .withCardInGraveyard(1, "Grizzly Bears")  // power 2
                .withCardInGraveyard(1, "Grave Robbers")  // Rogue — never legal
                .withCardInGraveyard(1, "Scaled Wurm")    // power 7 — above the cap
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val launderer = game.findPermanent("Body Launderer")!!
            game.castSpell(1, "Giant Growth", launderer).error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Murder", launderer).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Body Launderer") shouldBe true
            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            val legal = decision.legalTargets[0].orEmpty()
            val crawWurm = game.findCardsInGraveyard(1, "Craw Wurm").single()
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()

            withClue("last-known power 6 admits the 6-power Craw Wurm and the Bears") {
                legal shouldContainAll listOf(crawWurm, bears)
            }
            withClue("Rogues, power above 6, and the Launderer itself are not legal") {
                legal shouldNotContain game.findCardsInGraveyard(1, "Grave Robbers").single()
                legal shouldNotContain game.findCardsInGraveyard(1, "Scaled Wurm").single()
                legal shouldNotContain launderer
            }

            game.selectTargets(listOf(crawWurm)).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Craw Wurm") shouldBe true
        }
    }
}
