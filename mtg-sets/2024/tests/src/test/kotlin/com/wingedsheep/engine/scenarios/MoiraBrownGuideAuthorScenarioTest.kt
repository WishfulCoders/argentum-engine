package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Moira Brown, Guide Author (PIP #110) — {1}{R}{W} Legendary Creature — Human Citizen 2/3.
 * "When Moira Brown enters, create a colorless Book Equipment artifact token named Wasteland
 * Survival Guide with 'Equipped creature gets +1/+1 for each quest counter among permanents you
 * control' and equip {1}.
 * Whenever you attack, put a quest counter on target nonland permanent you control."
 */
class MoiraBrownGuideAuthorScenarioTest : ScenarioTestBase() {

    init {
        context("Moira Brown, Guide Author") {

            test("enters with the Guide, which equips for {1} and grows with each attack's quest counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Moira Brown, Guide Author")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Moira Brown, Guide Author").error shouldBe null
                game.resolveStack()

                val guide = game.findPermanent("Wasteland Survival Guide")
                withClue("Moira's ETB creates the Wasteland Survival Guide token") { guide shouldNotBe null }
                val guideCard = game.state.getEntity(guide!!)!!.get<CardComponent>()!!
                withClue("The Guide is a colorless Book Equipment artifact") {
                    guideCard.colors.isEmpty() shouldBe true
                    guideCard.typeLine.subtypes.map { it.value }.toSet() shouldBe setOf("Book", "Equipment")
                }

                val bears = game.findPermanent("Grizzly Bears")!!
                val equip = PredefinedTokens.WastelandSurvivalGuide.activatedAbilities.single { it.isEquipAbility }
                val equipped = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = guide,
                        abilityId = equip.id,
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                )
                withClue("Equip {1} should be payable: ${equipped.error}") { equipped.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()
                game.state.getEntity(guide)?.get<AttachedToComponent>()?.targetId shouldBe bears

                withClue("No quest counters yet: the Bears are a plain 2/2") {
                    game.state.projectedState.getPower(bears) shouldBe 2
                    game.state.projectedState.getToughness(bears) shouldBe 2
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                var guard = 0
                while (!game.hasPendingDecision() && guard++ < 20) game.resolveStack()
                withClue("Whenever you attack asks for a nonland permanent you control") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectTargets(listOf(guide)).error shouldBe null
                game.resolveStack()

                withClue("The quest counter lands on the Guide itself") {
                    game.state.getEntity(guide)?.get<CountersComponent>()?.getCount(CounterType.QUEST) shouldBe 1
                }
                withClue("One quest counter among permanents you control is +1/+1") {
                    game.state.projectedState.getPower(bears) shouldBe 3
                    game.state.projectedState.getToughness(bears) shouldBe 3
                }
            }

            test("counts quest counters on every permanent you control, but not the opponent's") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Wasteland Survival Guide", "Grizzly Bears")
                    .withCardOnBattlefield(1, "Moira Brown, Guide Author")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val moira = game.findPermanent("Moira Brown, Guide Author")!!
                val giant = game.findPermanent("Hill Giant")!!
                fun addQuest(id: EntityId, n: Int) {
                    game.state = game.state.updateEntity(id) {
                        it.with(CountersComponent().withAdded(CounterType.QUEST, n))
                    }
                }
                addQuest(moira, 2)
                addQuest(bears, 1)
                addQuest(giant, 5)

                withClue("Three quest counters on your permanents; the opponent's five don't count") {
                    game.state.projectedState.getPower(bears) shouldBe 5
                    game.state.projectedState.getToughness(bears) shouldBe 5
                }
            }
        }
    }
}
