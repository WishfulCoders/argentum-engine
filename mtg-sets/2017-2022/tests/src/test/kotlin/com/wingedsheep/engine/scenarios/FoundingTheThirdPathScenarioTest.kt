package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.woe.cards.YennaRedtoothRegent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Founding the Third Path (DMU #50) — and through it, read ahead (CR 702.155).
 *
 *   Read ahead
 *   I — You may cast an instant or sorcery spell with mana value 1 or 2 from your hand without
 *       paying its mana cost.
 *   II — Target player mills four cards.
 *   III — Exile target instant or sorcery card from your graveyard. Copy it. You may cast the copy.
 *
 * Read ahead: the Saga enters with the chosen number of lore counters (CR 714.3b), and the turn it
 * entered only the chapter equal to that number triggers (CR 702.155a). Later lore counters trigger
 * chapters normally.
 */
class FoundingTheThirdPathScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.lore(): Int {
            val saga = findPermanent("Founding the Third Path") ?: return -1
            return state.getEntity(saga)?.get<CountersComponent>()?.getCount(CounterType.LORE) ?: 0
        }

        fun castAndChoose(game: TestGame, chapter: Int) {
            game.castSpell(1, "Founding the Third Path").error shouldBe null
            game.resolveStack()
            withClue("read ahead asks for a chapter number as the Saga enters") {
                val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseNumberDecision>()
                decision.minValue shouldBe 1
                decision.maxValue shouldBe 3
            }
            game.chooseNumber(chapter)
        }

        context("Founding the Third Path") {

            test("choosing I: chapter I casts a mana value 1-2 instant from hand for free") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Founding the Third Path")
                    .withCardInHand(1, "Raise the Alarm") // {1}{W} instant, MV 2
                    .withCardInHand(1, "Divination")      // {2}{U} sorcery, MV 3 — not eligible
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                castAndChoose(game, 1)
                withClue("the Saga entered with exactly one lore counter") { game.lore() shouldBe 1 }

                game.resolveStack()
                val select = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                val alarm = game.findCardsInHand(1, "Raise the Alarm").first()
                withClue("only the mana value 1-2 instant or sorcery is offered") {
                    select.options shouldContainExactly listOf(alarm)
                }
                game.selectCards(listOf(alarm))
                game.resolveStack()

                withClue("Raise the Alarm resolved for free: two Soldier tokens") {
                    game.findPermanents("Soldier Token").size shouldBe 2
                }
                withClue("only the Saga's own two mana was spent") {
                    game.findPermanents("Island").count { game.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe 2
                }
                withClue("the Saga stays on the battlefield") {
                    game.findPermanent("Founding the Third Path") shouldNotBe null
                }
            }

            test("choosing II: only chapter II triggers, and chapter III follows next turn") {
                val builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Founding the Third Path")
                    .withCardInHand(1, "Raise the Alarm")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(12) {
                    builder.withCardInLibrary(1, "Island")
                    builder.withCardInLibrary(2, "Island")
                }
                val game = builder.build()

                castAndChoose(game, 2)
                withClue("the Saga entered with two lore counters") { game.lore() shouldBe 2 }

                // Chapter II's target: the opponent. Chapter I must not have triggered.
                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(game.player2Id))
                game.resolveStack()

                withClue("the opponent milled four") {
                    game.state.getZone(game.player2Id, Zone.GRAVEYARD).size shouldBe 4
                }
                withClue("chapter I was skipped: Raise the Alarm is still in hand") {
                    game.findCardsInHand(1, "Raise the Alarm").size shouldBe 1
                    game.findPermanents("Soldier Token").size shouldBe 0
                }

                // Next turn of the controller: lore 3 triggers chapter III normally. No instant or
                // sorcery in the graveyard, so it has no target and is removed; the Saga is then
                // sacrificed.
                val startTurn = game.state.turnNumber
                var guard = 0
                while (guard++ < 8) {
                    game.passUntilPhase(Phase.ENDING, Step.END)
                    game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    if (game.state.activePlayerId == game.player1Id && game.state.turnNumber > startTurn) break
                }
                withClue("the controller's next precombat main added the third lore counter") {
                    game.state.turnNumber shouldBe startTurn + 2
                }
                game.resolveStack()
                withClue("after chapter III the Saga is sacrificed") {
                    game.findPermanent("Founding the Third Path") shouldBe null
                }
            }

            test("choosing III: only chapter III triggers — exile, copy, cast the copy paying its cost") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Founding the Third Path")
                    .withCardInHand(1, "Raise the Alarm")
                    .withCardInGraveyard(1, "Divination")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                castAndChoose(game, 3)
                withClue("the Saga entered with three lore counters") { game.lore() shouldBe 3 }

                val divination = game.findCardsInGraveyard(1, "Divination").first()
                if (game.getPendingDecision() is ChooseTargetsDecision) {
                    game.selectTargets(listOf(divination))
                }
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true)
                game.resolveStack()

                withClue("the original Divination was exiled") {
                    game.state.getZone(game.player1Id, Zone.EXILE) shouldContainExactly listOf(divination)
                }
                withClue("the copy resolved: two cards drawn (plus Raise the Alarm still in hand)") {
                    game.state.getZone(game.player1Id, Zone.HAND).size shouldBe 3
                    game.state.getZone(game.player2Id, Zone.GRAVEYARD).size shouldBe 0
                }
                withClue("the copy was paid for: two Islands for the Saga plus three for the copy") {
                    game.findPermanents("Island").count { game.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe 5
                }
                withClue("chapters I and II were skipped") {
                    game.findPermanents("Soldier Token").size shouldBe 0
                }
                withClue("the Saga is sacrificed after its final chapter") {
                    game.findPermanent("Founding the Third Path") shouldBe null
                }
            }

            test("choosing III and declining the copy leaves the card exiled") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Founding the Third Path")
                    .withCardInGraveyard(1, "Divination")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                castAndChoose(game, 3)
                val divination = game.findCardsInGraveyard(1, "Divination").first()
                if (game.getPendingDecision() is ChooseTargetsDecision) {
                    game.selectTargets(listOf(divination))
                }
                game.resolveStack()
                game.answerYesNo(false)
                game.resolveStack()

                withClue("Divination is in exile, no cards drawn") {
                    game.state.getZone(game.player1Id, Zone.EXILE) shouldContainExactly listOf(divination)
                    game.state.getZone(game.player1Id, Zone.HAND).size shouldBe 0
                }
            }
            test("a token copy chooses its own chapter as it enters (CR 702.155b)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Yenna, Redtooth Regent")
                    .withCardOnBattlefield(1, "Founding the Third Path")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val yenna = game.findPermanent("Yenna, Redtooth Regent")!!
                val original = game.findPermanent("Founding the Third Path")!!
                val copyAbilityId = YennaRedtoothRegent.activatedAbilities.first { !it.isManaAbility }.id
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = yenna,
                        abilityId = copyAbilityId,
                        targets = listOf(ChosenTarget.Permanent(original)),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("the token copy asks for a chapter number as it enters") {
                    val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseNumberDecision>()
                    decision.minValue shouldBe 1
                    decision.maxValue shouldBe 3
                }
                game.chooseNumber(2)

                val token = game.findAllPermanents("Founding the Third Path").single { it != original }
                game.state.getEntity(token)?.has<TokenComponent>() shouldBe true
                withClue("the token entered with the chosen two lore counters") {
                    game.state.getEntity(token)?.get<CountersComponent>()?.getCount(CounterType.LORE) shouldBe 2
                }
                withClue("only chapter II triggers: it targets a player") {
                    game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                }
                game.selectTargets(listOf(game.player2Id))
                game.resolveStack()
                withClue("chapter I didn't trigger — no free-cast offer — and nothing else is pending") {
                    game.hasPendingDecision() shouldBe false
                }
            }

            test("a Saga put onto the battlefield by an effect chooses its chapter as it enters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Squirming Emergence")
                    .withCardInGraveyard(1, "Founding the Third Path")
                    .withCardInGraveyard(1, "Island")
                    .withCardInGraveyard(1, "Divination")
                    .withLandsOnBattlefield(1, "Bayou", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val saga = game.findCardsInGraveyard(1, "Founding the Third Path").first()
                game.castSpellTargetingGraveyardCard(1, "Squirming Emergence", listOf(saga)).error shouldBe null
                game.resolveStack()

                withClue("the returned Saga asks for a chapter number as it enters") {
                    val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseNumberDecision>()
                    decision.maxValue shouldBe 3
                }
                game.chooseNumber(3)
                withClue("the Saga entered with three lore counters") { game.lore() shouldBe 3 }

                val divination = game.findCardsInGraveyard(1, "Divination").first()
                withClue("only chapter III triggers: it targets the instant or sorcery in the graveyard") {
                    game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                }
                game.selectTargets(listOf(divination))
                game.resolveStack()
                game.answerYesNo(false)
                game.resolveStack()
                withClue("chapter III exiled Divination; the Saga is sacrificed after its final chapter") {
                    game.state.getZone(game.player1Id, Zone.EXILE) shouldContainExactly listOf(divination)
                    game.findPermanent("Founding the Third Path") shouldBe null
                }
            }
            test("an enchantment entering as a copy of the Saga chooses its own chapter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Copy Enchantment")
                    .withCardOnBattlefield(1, "Founding the Third Path")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val original = game.findPermanent("Founding the Third Path")!!
                game.castSpell(1, "Copy Enchantment").error shouldBe null
                game.resolveStack()
                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(listOf(original))

                withClue("the copy asks for a chapter number as it enters") {
                    game.getPendingDecision().shouldBeInstanceOf<ChooseNumberDecision>().maxValue shouldBe 3
                }
                game.chooseNumber(2)
                val copy = game.findAllPermanents("Founding the Third Path").single { it != original }
                withClue("the copy entered with the chosen two lore counters") {
                    game.state.getEntity(copy)?.get<CountersComponent>()?.getCount(CounterType.LORE) shouldBe 2
                }
                withClue("only chapter II triggers: it targets a player") {
                    game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                }
            }
        }
    }
}
