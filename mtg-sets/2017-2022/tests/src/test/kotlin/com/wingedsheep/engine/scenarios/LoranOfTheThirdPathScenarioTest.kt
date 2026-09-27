package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.bro.cards.LoranOfTheThirdPath
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Loran of the Third Path (BRO #12) — {2}{W} Legendary Creature — Human Artificer, 2/1.
 *
 * "Vigilance
 *  When Loran enters, destroy up to one target artifact or enchantment.
 *  {T}: You and target opponent each draw a card."
 *
 * Covers the optional ETB destruction (chosen and declined) and the tap ability's symmetric draw,
 * which targets an opponent even though it benefits the controller too.
 */
class LoranOfTheThirdPathScenarioTest : ScenarioTestBase() {

    private val tapAbilityId = LoranOfTheThirdPath.activatedAbilities[0].id

    init {
        context("Loran of the Third Path") {

            test("entering destroys a chosen target artifact") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Loran of the Third Path")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardOnBattlefield(2, "Magnifying Glass")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val glass = game.findPermanent("Magnifying Glass")!!

                game.castSpell(1, "Loran of the Third Path").error shouldBe null
                game.resolveStack() // Loran enters -> ETB trigger asks for up to one target

                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(glass)).error shouldBe null
                game.resolveStack()

                withClue("the targeted artifact is destroyed") {
                    game.isInGraveyard(2, "Magnifying Glass") shouldBe true
                }
                withClue("Loran is on the battlefield with vigilance") {
                    game.isOnBattlefield("Loran of the Third Path") shouldBe true
                }
            }

            test("\"up to one\" — declining the target destroys nothing") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Loran of the Third Path")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardOnBattlefield(2, "Magnifying Glass")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Loran of the Third Path").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                game.skipTargets().error shouldBe null
                game.resolveStack()

                withClue("declining the optional target leaves the artifact alone") {
                    game.isOnBattlefield("Magnifying Glass") shouldBe true
                }
            }

            test("{T}: you and target opponent each draw a card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Loran of the Third Path", summoningSickness = false)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val loran = game.findPermanent("Loran of the Third Path")!!
                val handBefore1 = game.handSize(1)
                val handBefore2 = game.handSize(2)

                val activate = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = loran,
                        abilityId = tapAbilityId,
                        targets = listOf(ChosenTarget.Player(game.player2Id)),
                    )
                )
                withClue("activating the tap ability should succeed: ${activate.error}") {
                    activate.error shouldBe null
                }
                if (game.getPendingDecision() is SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }
                game.resolveStack()

                withClue("both players draw exactly one card") {
                    game.handSize(1) shouldBe handBefore1 + 1
                    game.handSize(2) shouldBe handBefore2 + 1
                }
            }
        }
    }
}
