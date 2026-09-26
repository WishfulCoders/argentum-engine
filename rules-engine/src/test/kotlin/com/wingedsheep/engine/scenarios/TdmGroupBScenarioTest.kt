package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for TDM "group B":
 *  - Defibrillating Current (#177): 4 damage to a creature/planeswalker + gain 2 life.
 *  - Seize Opportunity (#119): modal — impulse-exile top two OR pump up to two creatures.
 *  - Piercing Exhale (#151): fight-style; optional behold a Dragon → surveil 2.
 *  - Osseous Exhale (#17): 5 damage to attacking/blocking creature; behold → gain 2 life.
 *  - Dispelling Exhale (#41): counter unless pays {2}, or {4} if a Dragon was beheld.
 *
 * The three Exhale cards model the optional "you may behold a Dragon" additional cost at
 * resolution time: gather the Dragons you control or hold, then a single "choose up to one"
 * selection (no separate yes/no — declining is selecting zero) stores the chosen Dragon, and
 * a Effects.If gated on that store applies the rider. So after the spell resolves the
 * controller gets a card selection; picking a Dragon enables the bonus, picking none skips it.
 */
class TdmGroupBScenarioTest : ScenarioTestBase() {

    init {
        context("Defibrillating Current") {
            test("deals 4 damage to a creature and the caster gains 2 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Defibrillating Current")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val target = game.findPermanent("Hill Giant")!!
                val cast = game.castSpell(1, "Defibrillating Current", target)
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("Hill Giant (3/3) takes 4 damage and dies") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }
                withClue("Caster gains 2 life") {
                    game.getLifeTotal(1) shouldBe 22
                }
            }
        }

        context("Seize Opportunity") {
            test("mode 1 exiles the top two cards and lets you play them") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Seize Opportunity")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpellWithMode(1, "Seize Opportunity", 0)
                withClue("Cast (mode 1) should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("Both top cards left the library and are now in exile") {
                    game.findCardsInLibrary(1, "Grizzly Bears").size shouldBe 0
                    game.findCardsInLibrary(1, "Hill Giant").size shouldBe 0
                    game.state.getExile(game.player1Id).size shouldBe 2
                }
                withClue("The exiled cards are playable (granted a may-play permission)") {
                    val permitted = game.state.mayPlayPermissions.flatMap { it.cardIds }.toSet()
                    game.state.getExile(game.player1Id).all { it in permitted } shouldBe true
                }
            }

            test("mode 2 pumps up to two target creatures +2/+1") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Seize Opportunity")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bear = game.findPermanent("Grizzly Bears")!!
                val cast = game.castSpellWithMode(1, "Seize Opportunity", 1, bear)
                withClue("Cast (mode 2) should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val bearCard = game.getClientState(1).cards.values.first { it.name == "Grizzly Bears" }
                withClue("Grizzly Bears is pumped to 4/3") {
                    bearCard.power shouldBe 4
                    bearCard.toughness shouldBe 3
                }
            }
        }

        context("Osseous Exhale") {
            test("5 damage to an attacking creature; behold a Dragon to gain 2 life") {
                // Player 1 attacks with their own creature, then (holding priority) casts
                // Osseous Exhale at it — "attacking creature" is a valid target regardless of
                // controller.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Osseous Exhale")
                    .withCardInHand(1, "Kilnmouth Dragon") // a Dragon to behold
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(1, "Hill Giant") // 3/3 attacker
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Hill Giant" to 0))

                val attacker = game.findPermanent("Hill Giant")!!
                val cast = game.castSpell(1, "Osseous Exhale", attacker)
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                // Resolution-time behold: a "choose up to one Dragon" selection is pending.
                withClue("A behold card selection should be pending") {
                    game.hasPendingDecision() shouldBe true
                }
                val dragon = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Kilnmouth Dragon"
                }
                game.selectCards(listOf(dragon))
                game.resolveStack()

                withClue("Hill Giant (3/3) takes 5 damage and dies") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }
                withClue("Beholding a Dragon grants 2 life") {
                    game.getLifeTotal(1) shouldBe 22
                }
            }

            test("5 damage but no life gain when no Dragon is beheld") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Osseous Exhale")
                    // No Dragon in hand or play → nothing to behold.
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(1, "Hill Giant") // 3/3 attacker
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Hill Giant" to 0))

                val attacker = game.findPermanent("Hill Giant")!!
                val cast = game.castSpell(1, "Osseous Exhale", attacker)
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                // No Dragon to behold → the selection is empty/auto-skipped.
                if (game.hasPendingDecision()) {
                    game.skipSelection()
                    game.resolveStack()
                }

                withClue("Hill Giant (3/3) takes 5 damage and dies") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }
                withClue("No Dragon beheld → no life gain") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }

        context("Piercing Exhale") {
            test("my creature deals damage equal to its power; declining behold skips surveil") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Piercing Exhale")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(1, "Hill Giant")      // 3/3 attacker source
                    .withCardOnBattlefield(2, "Grizzly Bears")   // 2/2 victim
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mine = game.findPermanent("Hill Giant")!!
                val victim = game.findPermanent("Grizzly Bears")!!
                val cardId = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Piercing Exhale"
                }
                val cast = game.execute(
                    CastSpell(
                        game.player1Id,
                        cardId,
                        listOf(ChosenTarget.Permanent(mine), ChosenTarget.Permanent(victim))
                    )
                )
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                // No Dragon to behold (none in hand/play) → behold selection is empty/auto-skipped.
                if (game.hasPendingDecision()) {
                    game.skipSelection()
                    game.resolveStack()
                }

                withClue("Grizzly Bears (2/2) takes 3 damage and dies") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }

            test("beholding a Dragon enables the surveil 2 rider") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Piercing Exhale")
                    .withCardInHand(1, "Kilnmouth Dragon")       // a Dragon to behold
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(1, "Hill Giant")      // 3/3 source
                    .withCardOnBattlefield(2, "Grizzly Bears")   // 2/2 victim
                    .withCardInLibrary(1, "Mountain")            // surveil fodder (top two)
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mine = game.findPermanent("Hill Giant")!!
                val victim = game.findPermanent("Grizzly Bears")!!
                val cardId = game.findCardsInHand(1, "Piercing Exhale").first()
                val cast = game.execute(
                    CastSpell(
                        game.player1Id,
                        cardId,
                        listOf(ChosenTarget.Permanent(mine), ChosenTarget.Permanent(victim))
                    )
                )
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                // Behold the Dragon, which enables the surveil 2 rider.
                withClue("A behold selection should be pending") {
                    game.hasPendingDecision() shouldBe true
                }
                val dragon = game.findCardsInHand(1, "Kilnmouth Dragon").first()
                game.selectCards(listOf(dragon))

                // Surveil 2: a card selection over the top two library cards.
                val surveil = game.getPendingDecision()
                withClue("Surveil 2 should pause for a library look") {
                    surveil.shouldBeInstanceOf<SelectCardsDecision>()
                }
                val lookedAt = (surveil as SelectCardsDecision).options
                withClue("Surveil 2 looks at the top two cards") { lookedAt.size shouldBe 2 }
                game.selectCards(lookedAt) // put both into the graveyard
                game.resolveStack()

                withClue("Grizzly Bears (2/2) takes 3 damage and dies") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
                withClue("Surveil 2 binned both looked-at cards") {
                    game.findCardsInLibrary(1, "Mountain").size shouldBe 0
                    game.findCardsInLibrary(1, "Island").size shouldBe 0
                    game.findCardsInGraveyard(1, "Mountain").size shouldBe 1
                    game.findCardsInGraveyard(1, "Island").size shouldBe 1
                }
            }
        }

        context("Dispelling Exhale") {
            test("counters a spell unless its controller pays {2} when no Dragon is beheld") {
                // Player 2 casts Grizzly Bears (tapping out); Player 1 responds with Dispelling
                // Exhale. With no Dragon beheld the tax is {2}, which the tapped-out Player 2
                // cannot pay, so the spell is countered.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Dispelling Exhale")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val gbCast = game.castSpell(2, "Grizzly Bears")
                withClue("Player 2's Grizzly Bears cast should succeed: ${gbCast.error}") {
                    gbCast.error shouldBe null
                }
                // Player 2 holds priority after casting; pass it to Player 1.
                game.passPriority()

                val cast = game.castSpellTargetingStackSpell(1, "Dispelling Exhale", "Grizzly Bears")
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                // No Dragon to behold → selection auto-skips; counter tax is {2}.
                if (game.hasPendingDecision()) {
                    game.skipSelection()
                    game.resolveStack()
                }

                withClue("Grizzly Bears should be countered (not on battlefield)") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }

            test("beholding a Dragon raises the tax to {4}, which the controller cannot pay") {
                // Player 2 casts Grizzly Bears ({1}{G}), tapping two of five Forests and leaving
                // three untapped — enough to pay {2}. Player 1 responds with Dispelling Exhale and
                // beholds a Dragon, raising the tax to {4}, which Player 2 cannot afford, so no
                // payment is offered and the spell is countered. (A {2} tax would have let Player 2
                // pay, distinguishing this from the no-behold path above.)
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Dispelling Exhale")
                    .withCardInHand(1, "Kilnmouth Dragon") // a Dragon to behold
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 5) // 2 to cast, 3 left over (covers {2}, not {4})
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val gbCast = game.castSpell(2, "Grizzly Bears")
                withClue("Player 2's Grizzly Bears cast should succeed: ${gbCast.error}") {
                    gbCast.error shouldBe null
                }
                game.passPriority()

                val cast = game.castSpellTargetingStackSpell(1, "Dispelling Exhale", "Grizzly Bears")
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                // Resolution-time behold: choose the Dragon to raise the tax from {2} to {4}.
                withClue("A behold selection should be pending") {
                    game.hasPendingDecision() shouldBe true
                }
                val dragon = game.findCardsInHand(1, "Kilnmouth Dragon").first()
                game.selectCards(listOf(dragon))
                game.resolveStack()

                withClue("Player 2 cannot afford {4}, so no payment is offered") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("Grizzly Bears should be countered (not on battlefield)") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }
        }
    }
}
