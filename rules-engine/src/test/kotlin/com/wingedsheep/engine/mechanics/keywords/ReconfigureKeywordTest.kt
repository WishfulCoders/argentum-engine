package com.wingedsheep.engine.mechanics.keywords

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.reconfigure
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Reconfigure (CR 702.151).
 *
 * - CR 702.151a: two activated abilities — attach to another target creature you control, and
 *   unattach (only while attached to a creature) — both sorcery-speed.
 * - CR 702.151b: attaching an Equipment with reconfigure to another creature makes it stop being a
 *   creature until it becomes unattached from that creature (and with no creature type it has no
 *   creature subtypes — Neon Dynasty release-notes ruling). Applies to any attach, not just the
 *   reconfigure ability, and outlives the Equipment losing its abilities.
 * - CR 301.5c: an Equipment that's also a creature can equip a creature only if it has reconfigure;
 *   an Equipment can't equip itself.
 * - CR 704.5n: an Equipment attached to a permanent that stops being a creature (the reconfigured
 *   Equipment itself) becomes unattached.
 */
class ReconfigureKeywordTest : ScenarioTestBase() {

    private val cat = card("Reconfigure Test Cat") {
        manaCost = "{1}{W}"
        typeLine = "Artifact Creature — Equipment Cat"
        power = 2
        toughness = 2
        staticAbility { ability = ModifyStats(1, 1) }
        reconfigure("{2}")
    }
    private val attachId = cat.activatedAbilities[0].id
    private val unattachId = cat.activatedAbilities[1].id

    private fun TestGame.catId(): EntityId = findPermanent("Reconfigure Test Cat")!!

    private fun TestGame.reconfigureOnto(host: EntityId) =
        execute(ActivateAbility(player1Id, catId(), attachId, targets = listOf(ChosenTarget.Permanent(host))))

    private fun TestGame.unattach() = execute(ActivateAbility(player1Id, catId(), unattachId))

    private fun TestGame.isCreature(id: EntityId) = state.projectedState.isCreature(id)

    private fun board() = scenario().withPlayers()
        .withCardOnBattlefield(1, "Reconfigure Test Cat")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Hill Giant")
        .withLandsOnBattlefield(1, "Plains", 6)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        cardRegistry.register(cat)
        cardRegistry.register(card("Reconfigure Test Removal") {
            manaCost = "{0}"
            typeLine = "Instant"
            spell { effect = Effects.Destroy(target(TargetFilter.Permanent)) }
        })
        cardRegistry.register(card("Reconfigure Test Squire") {
            manaCost = "{0}"
            typeLine = "Instant"
            spell {
                val equipment = target(TargetFilter(GameObjectFilter.Artifact.withSubtype("Equipment").youControl()))
                val creature = target(TargetFilter.CreatureYouControl)
                effect = Effects.AttachTargetEquipmentToCreature(equipment, creature)
            }
        })
        cardRegistry.register(card("Reconfigure Test Strip") {
            manaCost = "{0}"
            typeLine = "Instant"
            spell { effect = Effects.RemoveAllAbilities(target(TargetFilter.Permanent)) }
        })
        cardRegistry.register(card("Reconfigure Test Sword") {
            manaCost = "{0}"
            typeLine = "Artifact — Equipment"
            staticAbility { ability = ModifyStats(2, 0) }
            equipAbility("{0}")
        })

        test("the card has the reconfigure keyword and two abilities, neither an equip ability") {
            cat.keywords shouldContain Keyword.RECONFIGURE
            cat.activatedAbilities.size shouldBe 2
            cat.activatedAbilities.none { it.isEquipAbility } shouldBe true
        }

        test("attaching stops it being a creature, strips its creature types and pumps the host") {
            val game = board().build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val r = game.reconfigureOnto(bears)
            withClue("${r.error}") { r.error shouldBe null }
            game.resolveStack()

            val cat = game.catId()
            game.state.getEntity(cat)!!.get<AttachedToComponent>()!!.targetId shouldBe bears
            game.isCreature(cat) shouldBe false
            game.state.projectedState.hasType(cat, "ARTIFACT") shouldBe true
            game.state.projectedState.hasSubtype(cat, "Equipment") shouldBe true
            game.state.projectedState.hasSubtype(cat, "Cat") shouldBe false
            game.state.projectedState.getPower(bears) shouldBe 3
            game.state.projectedState.getToughness(bears) shouldBe 3
        }

        test("it can't target itself") {
            val game = board().build()
            game.reconfigureOnto(game.catId()).error shouldNotBe null
            val attachAction = game.getLegalActions(1)
                .single { (it.action as? ActivateAbility)?.abilityId == attachId }
            attachAction.validTargets!! shouldNotContain game.catId()
            attachAction.validTargets!! shouldContain game.findPermanent("Grizzly Bears")!!
        }

        test("an opponent's creature isn't a legal target") {
            val game = board().withCardOnBattlefield(2, "Savannah Lions").build()
            game.reconfigureOnto(game.findPermanent("Savannah Lions")!!).error shouldNotBe null
        }

        test("unattach is only offered while attached, and makes it a creature again") {
            val game = board().build()
            fun unattachOffered() = game.getLegalActions(1).any { (it.action as? ActivateAbility)?.abilityId == unattachId }
            unattachOffered() shouldBe false
            game.unattach().error shouldNotBe null

            val bears = game.findPermanent("Grizzly Bears")!!
            game.reconfigureOnto(bears).error shouldBe null
            game.resolveStack()
            unattachOffered() shouldBe true

            val r = game.unattach()
            withClue("${r.error}") { r.error shouldBe null }
            game.resolveStack()
            val cat = game.catId()
            game.state.getEntity(cat)!!.get<AttachedToComponent>() shouldBe null
            game.isCreature(cat) shouldBe true
            game.state.projectedState.hasSubtype(cat, "Cat") shouldBe true
            game.state.projectedState.getPower(bears) shouldBe 2
        }

        test("both abilities are sorcery-speed") {
            val game = board().withActivePlayer(2).withPriorityPlayer(1).build()
            game.reconfigureOnto(game.findPermanent("Grizzly Bears")!!).error shouldNotBe null
        }

        test("moving to another creature keeps it a non-creature and moves the bonus") {
            val game = board().build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            game.reconfigureOnto(bears).error shouldBe null
            game.resolveStack()
            game.reconfigureOnto(giant).error shouldBe null
            game.resolveStack()

            game.isCreature(game.catId()) shouldBe false
            game.state.projectedState.getPower(bears) shouldBe 2
            game.state.projectedState.getPower(giant) shouldBe 4
        }

        test("when the host leaves, it becomes unattached and is a creature again") {
            val game = board().withCardInHand(1, "Reconfigure Test Removal").build()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.reconfigureOnto(bears).error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Reconfigure Test Removal", bears).error shouldBe null
            game.resolveStack()

            val cat = game.catId()
            game.state.getEntity(cat)!!.get<AttachedToComponent>() shouldBe null
            game.isCreature(cat) shouldBe true
            game.state.projectedState.hasSubtype(cat, "Cat") shouldBe true
        }

        test("attaching it by another effect also stops it being a creature") {
            val game = board().withCardInHand(1, "Reconfigure Test Squire").build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val squire = game.findCardsInHand(1, "Reconfigure Test Squire").single()
            val r = game.execute(
                CastSpell(game.player1Id, squire, listOf(ChosenTarget.Permanent(game.catId()), ChosenTarget.Permanent(bears)))
            )
            withClue("${r.error}") { r.error shouldBe null }
            game.resolveStack()

            game.state.getEntity(game.catId())!!.get<AttachedToComponent>()?.targetId shouldBe bears
            game.isCreature(game.catId()) shouldBe false
            game.state.projectedState.getPower(bears) shouldBe 3
        }

        test("losing all abilities while attached doesn't make it a creature again") {
            val game = board().withCardInHand(1, "Reconfigure Test Strip").build()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.reconfigureOnto(bears).error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Reconfigure Test Strip", game.catId()).error shouldBe null
            game.resolveStack()

            game.state.projectedState.hasKeyword(game.catId(), Keyword.RECONFIGURE) shouldBe false
            game.isCreature(game.catId()) shouldBe false
            game.state.getEntity(game.catId())!!.get<AttachedToComponent>()?.targetId shouldBe bears
        }

        test("an Equipment attached to it falls off once it stops being a creature") {
            val game = board().withCardOnBattlefield(1, "Reconfigure Test Sword").build()
            val sword = game.findPermanent("Reconfigure Test Sword")!!
            val swordEquip = cardRegistry.getCard("Reconfigure Test Sword")!!.activatedAbilities.single().id
            game.execute(
                ActivateAbility(game.player1Id, sword, swordEquip, targets = listOf(ChosenTarget.Permanent(game.catId())))
            ).error shouldBe null
            game.resolveStack()
            game.state.getEntity(sword)!!.get<AttachedToComponent>()?.targetId shouldBe game.catId()

            game.reconfigureOnto(game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()
            game.state.getEntity(sword)!!.get<AttachedToComponent>() shouldBe null
        }

        test("an Equipment creature without reconfigure can't be attached by an equip-style effect") {
            cardRegistry.register(card("Reconfigure Test Plain Golem") {
                manaCost = "{2}"
                typeLine = "Artifact Creature — Equipment Golem"
                power = 2
                toughness = 2
                activatedAbility {
                    cost = Costs.Mana("{0}")
                    val host = target(TargetFilter.OtherCreatureYouControl)
                    effect = Effects.AttachEquipment(host)
                }
            })
            val game = board().withCardOnBattlefield(1, "Reconfigure Test Plain Golem").build()
            val golem = game.findPermanent("Reconfigure Test Plain Golem")!!
            val abilityId = cardRegistry.getCard("Reconfigure Test Plain Golem")!!.activatedAbilities.single().id
            game.execute(
                ActivateAbility(game.player1Id, golem, abilityId, targets = listOf(ChosenTarget.Permanent(game.findPermanent("Grizzly Bears")!!)))
            ).error shouldBe null
            game.resolveStack()
            // CR 301.5c: it can't equip while it's a creature, so it ends up unattached (CR 704.5n)
            // and is still a creature — no reconfigure effect began.
            game.state.getEntity(golem)!!.get<AttachedToComponent>() shouldBe null
            game.isCreature(golem) shouldBe true
        }
    }
}
