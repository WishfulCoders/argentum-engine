package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * `StatePredicate.WasDealtDamageBySourceYouControlledThisTurn` through the replacement that
 * motivates it — Etching of Kumano's "If a creature dealt damage this turn by a source you
 * controlled would die, exile it instead."
 *
 * Rules pinned here:
 *  - the source's controller is read **as the damage is dealt** (CR 608.2h): a burn spell that has
 *    left the stack still counts, and an opponent's source never does;
 *  - "would die" is any death (CR 700.4), not only death from that damage — a creature first
 *    damaged and then destroyed the same turn is exiled;
 *  - combat damage counts the same as noncombat damage (CR 120.2a/b);
 *  - "a creature" means any creature, your own included;
 *  - the record belongs to the damaged object: one that changed zones is a new object that was never
 *    dealt that damage (CR 400.7);
 *  - the replacement needs to be in force only when the creature would die, not when it was damaged.
 */
class DealtDamageBySourceYouControlledTest : ScenarioTestBase() {

    private val etching = card("Test Etching") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        replacementEffect(
            RedirectZoneChange(
                newDestination = Zone.EXILE,
                appliesTo = EventPattern.ZoneChangeEvent(
                    filter = GameObjectFilter.Creature.wasDealtDamageBySourceYouControlledThisTurn(),
                    from = Zone.BATTLEFIELD,
                    to = Zone.GRAVEYARD,
                ),
            )
        )
    }

    private val burnThree = card("Test Burn Three") {
        manaCost = "{0}"; typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.DealDamage(3, t)
        }
    }

    private val burnOne = card("Test Burn One") {
        manaCost = "{0}"; typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.DealDamage(1, t)
        }
    }

    private val destroy = card("Test Destroy Creature") {
        manaCost = "{0}"; typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.Destroy(t)
        }
    }

    private val blink = card("Test Blink Creature") {
        manaCost = "{0}"; typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.Exile(t) then Effects.PutOntoBattlefieldUnderYourControl(t)
        }
    }

    init {
        cardRegistry.register(listOf(etching, burnThree, burnOne, destroy, blink))

        fun board(
            vararg p1Hand: String,
            etchingInPlay: Boolean = true,
            p2Hand: List<String> = emptyList(),
            active: Int = 1,
        ) =
            scenario()
                .withPlayers()
                .apply { if (etchingInPlay) withCardOnBattlefield(1, "Test Etching") }
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Craw Wurm")
                .apply { p1Hand.forEach { withCardInHand(1, it) } }
                .apply { p2Hand.forEach { withCardInHand(2, it) } }
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(active)
                .withPriorityPlayer(active)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

        test("a creature killed by your burn spell is exiled, though the spell has left the stack") {
            val game = board("Test Burn Three")
            game.castSpell(1, "Test Burn Three", game.findPermanent("Grizzly Bears")).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Grizzly Bears") shouldBe true
            game.isInGraveyard(2, "Grizzly Bears") shouldBe false
        }

        test("a creature killed by an opponent's source dies normally") {
            val game = board(p2Hand = listOf("Test Burn Three"), active = 2)
            game.castSpell(2, "Test Burn Three", game.findPermanent("Grizzly Bears")).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.isInExile(2, "Grizzly Bears") shouldBe false
        }

        test("damaged by your source, then destroyed the same turn: exiled (any death counts)") {
            val game = board("Test Burn One", "Test Destroy Creature")
            val wurm = game.findPermanent("Craw Wurm")
            game.castSpell(1, "Test Burn One", wurm).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Craw Wurm") shouldBe true

            game.castSpell(1, "Test Destroy Creature", wurm).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Craw Wurm") shouldBe true
        }

        test("your own creature damaged by your own source is exiled too") {
            val game = board("Test Burn Three")
            game.castSpell(1, "Test Burn Three", game.findPermanent("Hill Giant")).error shouldBe null
            game.resolveStack()

            game.isInExile(1, "Hill Giant") shouldBe true
        }

        test("an object that changed zones after the damage is a new object and dies normally") {
            val game = board("Test Burn One", "Test Blink Creature", "Test Destroy Creature")
            game.castSpell(1, "Test Burn One", game.findPermanent("Craw Wurm")).error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Test Blink Creature", game.findPermanent("Craw Wurm")).error shouldBe null
            game.resolveStack()

            game.castSpell(1, "Test Destroy Creature", game.findPermanent("Craw Wurm")).error shouldBe null
            game.resolveStack()

            withClue("CR 400.7 — the blinked Wurm has no memory of the damage") {
                game.isInGraveyard(2, "Craw Wurm") shouldBe true
            }
        }

        test("the replacement applies if it is in force when the creature would die, not when damaged") {
            val game = board("Test Burn One", "Test Etching", "Test Destroy Creature", etchingInPlay = false)
            val wurm = game.findPermanent("Craw Wurm")
            game.castSpell(1, "Test Burn One", wurm).error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Test Etching").error shouldBe null
            game.resolveStack()

            game.castSpell(1, "Test Destroy Creature", wurm).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Craw Wurm") shouldBe true
        }

        test("without the replacement, the same kill goes to the graveyard") {
            val game = board("Test Burn Three", etchingInPlay = false)
            game.castSpell(1, "Test Burn Three", game.findPermanent("Grizzly Bears")).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
        }

        test("combat damage from your attacker counts; the blocker is exiled") {
            val game = board()
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            game.isInExile(2, "Grizzly Bears") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
        }
    }
}
