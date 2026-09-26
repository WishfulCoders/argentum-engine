package com.wingedsheep.replay

import com.wingedsheep.engine.core.CardEntityFactory
import com.wingedsheep.engine.handlers.effects.ZoneEntryOptions
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * C2 (`docs/27` §5): rewrites an engine state's public part to a recorded end-of-turn snapshot, so
 * that a game whose half-turn could not be rebuilt carries on replaying from the next one.
 *
 * What the snapshot shows is made to match: each side's non-token permanents by name, the number of
 * tokens, life totals, the user's hand and the opponent's hand size. A control change
 * drops any floating control effect on the permanent. Cards move through
 * [ZoneTransitionService] (no triggers fire: its events are dropped), from where a card of that name
 * most plausibly is. What the snapshot does not show is guessed and not searched over: permanents
 * enter untapped with no counters beyond their intrinsic ones, an Aura goes on the first creature of
 * the side it most likely enchants, a removed permanent goes to its owner's graveyard, a missing token
 * is a copy of one already on the battlefield (else a vanilla creature made a token). A card its owner
 * has nowhere (a copy, a card from outside the listed deck) is made: a token when its side already
 * controls one of that name, else a new card ([lastMade]). [patch] returns null, with [lastError] set,
 * when it cannot make the snapshot match.
 */
class SnapshotPatcher(
    private val registry: CardRegistry,
    private val snapshotter: Snapshotter,
    /** The [Reconstructor]'s hidden-slot writer: gives an opponent's unseen library card a name. */
    private val materialize: (GameState, Map<EntityId, String>) -> GameState?,
    /** A vanilla creature card, made a token when a side needs one and has none to copy. */
    private val tokenFiller: () -> String?,
) {
    /** The zone-move pipeline over this registry (upstream made it an instance per engine). */
    private val zones = ZoneTransitionService(registry, com.wingedsheep.engine.handlers.PredicateEvaluator(registry))

    var lastError: String? = null
        private set
    /** What the last successful [patch] changed, for the trace and the result. */
    var lastEdits: List<String> = emptyList()
        private set
    /** Cards the last successful [patch] made because their owner had none ("user:battlefield:Name token", with where else the name was). */
    var lastMade: List<String> = emptyList()
        private set
    private val made = mutableListOf<String>()
    /** For the trace: the edits and the entities of each differing name, when the last [patch] still differed. */
    var lastDetail: String? = null
        private set

    fun patch(given: GameState, seats: Seats, eot: EotSpec): GameState? {
        lastError = null
        lastDetail = null
        made.clear()
        val edits = mutableListOf<String>()
        var s = given
        fun fail(why: String): GameState? { lastError = why; return null }

        // Permanents the record lacks go to the graveyard (the last of each name); Auras first, since
        // an Aura that steals its host gives it back as it goes.
        fun removeExtras(auras: Boolean): GameState? {
            for (side in SIDES) {
                for ((name, k) in extra(s, seats, eot, side)) {
                    if (auras && !isAura(name)) continue
                    for (id in named(s, s.controlledBattlefield(seats.of(side)), name).takeLast(k)) {
                        s = move(s, id, Zone.GRAVEYARD) ?: return fail("resync: could not remove $side's $name")
                        edits += "-$side:$name"
                    }
                }
            }
            return s
        }
        removeExtras(auras = true) ?: return null
        // 1. Control changes: a name one side has too many of and the other too few is a stolen permanent.
        for (side in SIDES) {
            val other = other(side)
            for (name in extra(s, seats, eot, side).keys.intersect(missing(s, seats, eot, other).keys)) {
                val k = minOf(extra(s, seats, eot, side)[name] ?: 0, missing(s, seats, eot, other)[name] ?: 0)
                for (id in named(s, s.controlledBattlefield(seats.of(side)), name).take(k)) {
                    // a steal is a floating control effect, which would override the component
                    s = s.copy(floatingEffects = s.floatingEffects.filterNot { it.effect.layer == Layer.CONTROL && id in it.effect.affectedEntities })
                        .updateEntity(id) { it.with(ControllerComponent(seats.of(other))) }
                    edits += "control $name -> $other"
                }
            }
        }
        // 2. The other permanents the record lacks.
        removeExtras(auras = false) ?: return null
        // 3. Permanents the record has that the engine lacks enter, non-Auras first so an Aura finds its host.
        for (side in SIDES) {
            val player = seats.of(side)
            val wanted = missing(s, seats, eot, side).flatMap { (n, k) -> List(k) { n } }
                .sortedBy { if (isAura(it)) 1 else 0 }
            for (name in wanted) {
                val (found, card) = findCard(s, seats, side, name, eot) ?: makeCard(s, seats, side, name, Zone.BATTLEFIELD)
                    ?: return fail("resync: no $name for $side's battlefield")
                // a made token is on the battlefield already
                s = if (card in found.getBattlefield()) found else move(found, card, Zone.BATTLEFIELD, player) ?: return fail("resync: could not put $side's $name onto the battlefield")
                if (isAura(name)) s = attach(s, seats, side, card) ?: return fail("resync: no host for $side's $name")
                edits += "+$side:$name"
            }
        }
        // 4. Tokens: extras cease to exist, missing ones are copies of a token already there.
        for (side in SIDES) {
            val player = seats.of(side)
            val tokens = s.controlledBattlefield(player).filter { snapshotter.isToken(s, it) }
            // a token named like a card counts among the named permanents ([Snapshotter.diff])
            val want = (eot.tokens[side] ?: 0) + namedTokens(s, seats, eot, side)
            if (tokens.size > want) {
                // keep the tokens that stand for a named permanent of the record (a copy, possibly just made)
                val stands = missingNamed(s, seats, eot, side).toMutableMap()
                val keep = tokens.filter { id ->
                    val n = snapshotter.name(s, id)
                    val left = stands[n] ?: 0
                    if (left > 0) stands[n!!] = left - 1
                    left > 0
                }.toSet()
                for (id in (tokens.filterNot { it in keep } + tokens.filter { it in keep }).take(tokens.size - want)) {
                    s = s.removeEntity(id)
                    edits += "-$side:token"
                }
            } else if (tokens.size < want) {
                val model = tokens.lastOrNull() ?: s.getBattlefield().lastOrNull { snapshotter.isToken(s, it) && !isFaceDown(s, it) }
                repeat(want - tokens.size) {
                    s = (if (model != null) copyToken(s, model, player) else fillerToken(s, player))
                        ?: return fail("resync: ${want - tokens.size} $side token(s) missing and no stand-in")
                    edits += "+$side:token ${model?.let { snapshotter.name(s, it) } ?: "stand-in"}"
                }
            }
        }
        // 5. Life totals.
        for (side in SIDES) {
            val life = eot.life[side]?.toInt() ?: continue
            val player = seats.of(side)
            if (s.getEntity(player)?.get<LifeTotalComponent>()?.life != life) {
                s = s.updateEntity(player) { it.with(LifeTotalComponent(life)) }
                edits += "life.$side=$life"
            }
        }
        // 6. The user's hand: extras to the graveyard (cast or discarded), missing cards from the
        // library's bottom (its top holds the recorded draws), the graveyard or exile.
        val handNow = Snapshotter.counts(s.getHand(seats.user).mapNotNull { snapshotter.name(s, it) })
        val handWant = Snapshotter.counts(eot.userHand)
        for ((name, k) in handNow) {
            val over = k - (handWant[name] ?: 0)
            if (over <= 0) continue
            for (id in named(s, s.getHand(seats.user), name).takeLast(over)) {
                s = move(s, id, Zone.GRAVEYARD) ?: return fail("resync: could not discard $name")
                edits += "-hand:$name"
            }
        }
        for ((name, k) in handWant) {
            val short = k - (handNow[name] ?: 0)
            repeat(short) {
                val (found, id) = (named(s, s.getGraveyard(seats.user), name) + named(s, s.getExile(seats.user), name) +
                    named(s, s.getLibrary(seats.user), name).asReversed()).firstOrNull()?.let { s to it }
                    ?: makeCard(s, seats, "user", name, Zone.HAND)
                    ?: return fail("resync: no $name for the user's hand")
                s = move(found, id, Zone.HAND) ?: return fail("resync: could not return $name to hand")
                edits += "+hand:$name"
            }
        }
        // 7. The opponent's hand size: filler goes to the bottom of their library, or comes off its top.
        val oppoHand = s.getHand(seats.oppo)
        if (oppoHand.size > eot.oppoHand) {
            val basics = Reconstructor.BASICS.values.toSet()
            val out = oppoHand.sortedBy { if (snapshotter.name(s, it) in basics) 0 else 1 }.take(oppoHand.size - eot.oppoHand)
            for (id in out) {
                s = move(s, id, Zone.LIBRARY, placeBottom = true) ?: return fail("resync: could not shrink the opponent's hand")
            }
            edits += "oppo_hand ${oppoHand.size}->${eot.oppoHand}"
        } else if (oppoHand.size < eot.oppoHand) {
            val take = s.getLibrary(seats.oppo).filter { s.getEntity(it)?.has<RevealedToComponent>() != true }
                .take(eot.oppoHand - oppoHand.size)
            if (take.size < eot.oppoHand - oppoHand.size) return fail("resync: the opponent's library is too short")
            for (id in take) s = move(s, id, Zone.HAND) ?: return fail("resync: could not grow the opponent's hand")
            edits += "oppo_hand ${oppoHand.size}->${eot.oppoHand}"
        }
        val diff = snapshotter.diff(snapshotter.take(s, seats), eot)
        if (diff.isNotEmpty()) {
            val snap = snapshotter.take(s, seats)
            val off = SIDES.flatMap { side ->
                val recorded = Snapshotter.counts(eot.battlefield[side].orEmpty())
                val now = snap.battlefield[side].orEmpty()
                (recorded.keys + now.keys).filter { recorded[it] != now[it] }
            }.distinct()
            lastDetail = "edits ${edits.joinToString(", ")}; " + off.joinToString("; ") { n -> "$n: " + describe(s, seats, n) }
            return fail("resync: still differs: ${diff.joinToString("; ")}")
        }
        lastEdits = edits
        lastMade = made.toList()
        return s
    }

    private fun other(side: String) = if (side == "user") "oppo" else "user"

    /** Non-token permanents by name, as [Snapshotter.diff] counts them (a card-named token counts too). */
    private fun board(s: GameState, seats: Seats, eot: EotSpec, side: String): Map<String, Int> {
        val snap = snapshotter.take(s, seats)
        val recorded = Snapshotter.counts(eot.battlefield[side].orEmpty())
        val named = snap.battlefield[side].orEmpty().toMutableMap()
        for ((name, k) in snap.tokenNames[side].orEmpty()) {
            val move = minOf(k, maxOf(0, (recorded[name] ?: 0) - (named[name] ?: 0)))
            if (move > 0) named[name] = (named[name] ?: 0) + move
        }
        return named
    }

    private fun namedTokens(s: GameState, seats: Seats, eot: EotSpec, side: String): Int {
        val snap = snapshotter.take(s, seats)
        val short = missingNamed(s, seats, eot, side)
        return snap.tokenNames[side].orEmpty().entries.sumOf { (name, k) -> minOf(k, short[name] ?: 0) }
    }

    /** Record names [side]'s non-token permanents fall short of, which a token of that name stands for. */
    private fun missingNamed(s: GameState, seats: Seats, eot: EotSpec, side: String): Map<String, Int> {
        val named = snapshotter.take(s, seats).battlefield[side].orEmpty()
        return Snapshotter.counts(eot.battlefield[side].orEmpty())
            .mapValues { (n, k) -> k - (named[n] ?: 0) }.filterValues { it > 0 }
    }

    private fun extra(s: GameState, seats: Seats, eot: EotSpec, side: String): Map<String, Int> {
        val recorded = Snapshotter.counts(eot.battlefield[side].orEmpty())
        return board(s, seats, eot, side).mapValues { (n, k) -> k - (recorded[n] ?: 0) }.filterValues { it > 0 }
    }

    private fun missing(s: GameState, seats: Seats, eot: EotSpec, side: String): Map<String, Int> {
        val now = board(s, seats, eot, side)
        return Snapshotter.counts(eot.battlefield[side].orEmpty())
            .mapValues { (n, k) -> k - (now[n] ?: 0) }.filterValues { it > 0 }
    }

    /** Non-token cards among [ids] called [name]. */
    private fun named(s: GameState, ids: List<EntityId>, name: String): List<EntityId> =
        ids.filter { snapshotter.name(s, it) == name && !snapshotter.isToken(s, it) }

    /**
     * A card called [name] that [side] owns, to put onto the battlefield: from the hand when the
     * record's hand has fewer of it (it was cast), the graveyard, exile, the library (bottom first:
     * the top holds recorded draws), the hand anyway. For the opponent, an unseen library card is
     * given the name if none is found.
     */
    private fun findCard(s: GameState, seats: Seats, side: String, name: String, eot: EotSpec): Pair<GameState, EntityId>? {
        val player = seats.of(side)
        val inHand = named(s, s.getHand(player), name)
        val handWant = if (side == "user") eot.userHand.count { it == name } else 0
        val order = (if (side == "oppo" || inHand.size > handWant) inHand else emptyList()) +
            named(s, s.getGraveyard(player), name) + named(s, s.getExile(player), name) +
            named(s, s.getLibrary(player), name).asReversed() + inHand
        order.firstOrNull()?.let { return s to it }
        // the other player's card: stolen, or reanimated or played from their graveyard or exile
        val theirs = seats.of(other(side))
        (named(s, s.getGraveyard(theirs), name) + named(s, s.getExile(theirs), name)).firstOrNull()?.let { return s to it }
        if (side != "oppo") return null
        val slot = s.getLibrary(player).lastOrNull { s.getEntity(it)?.has<RevealedToComponent>() != true } ?: return null
        val written = materialize(s, mapOf(slot to name)) ?: return null
        return written to slot
    }

    /**
     * A card called [name] that [side] has nowhere, made for a move to [zone]: for the battlefield, a
     * token when [side] already controls a permanent of that name (a copy), else a new card in its owner's
     * exile, to be moved from there. Null if the engine has no such card.
     */
    private fun makeCard(s: GameState, seats: Seats, side: String, name: String, zone: Zone): Pair<GameState, EntityId>? {
        val def = registry.getCard(snapshotter.engineName(name)) ?: return null
        val player = seats.of(side)
        val token = zone == Zone.BATTLEFIELD && s.controlledBattlefield(player).any { snapshotter.name(s, it) == name }
        val (id, next) = s.newEntity()
        var container = CardEntityFactory.create(def, player).with(OwnerComponent(player)).with(ControllerComponent(player))
        if (token) container = container.with(TokenComponent)
        val where = locate(s, seats, name)
        made += "$side:${zone.name.lowercase()}:$name${if (token) " token" else ""}${if (where.isEmpty()) "" else " (elsewhere: $where)"}"
        // a token is created where it is wanted; a card waits in exile for [move]
        val home = if (token) ZoneKey(player, Zone.BATTLEFIELD) else ZoneKey(player, Zone.EXILE)
        val placed = next.withEntity(id, container).addToZone(home, id)
        return placed to id
    }

    /** Each entity called [name]: zone, owner, controller (component / projected), token, copy, face down. */
    private fun describe(s: GameState, seats: Seats, name: String): String =
        (s.zones.flatMap { (key, ids) -> ids.map { key to it } }).filter { snapshotter.name(s, it.second) == name }.joinToString(" | ") { (key, id) ->
            val e = s.getEntity(id)
            val base = e?.get<ControllerComponent>()?.playerId?.let(seats::sideOf)
            val projected = s.projectedState.getController(id)?.let(seats::sideOf)
            val control = s.floatingEffects.filter { it.effect.layer == Layer.CONTROL && id in it.effect.affectedEntities }
            "${key.zoneType.name.lowercase()} owner=${seats.sideOf(key.ownerId)} ctl=$base/$projected" +
                (if (e?.has<TokenComponent>() == true) " token" else "") +
                (if (e?.has<com.wingedsheep.engine.state.components.identity.CopyOfComponent>() == true) " copy" else "") +
                (if (isFaceDown(s, id)) " facedown" else "") +
                (if (control.isNotEmpty()) " floating-control=${control.size}" else "") +
                " card=${e?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name}"
        }

    /** Where cards called [name] are, for [lastMade]: "user.graveyard stack …". */
    private fun locate(s: GameState, seats: Seats, name: String): String =
        (s.zones.flatMap { (key, ids) -> ids.filter { snapshotter.name(s, it) == name }.map { "${seats.sideOf(key.ownerId)}.${key.zoneType.name.lowercase()}" } } +
            s.stack.filter { snapshotter.name(s, it) == name }.map { "stack" })
            .groupingBy { it }.eachCount().entries.joinToString(" ") { (k, n) -> if (n > 1) "$k×$n" else k }

    private fun move(s: GameState, id: EntityId, zone: Zone, controller: EntityId? = null, placeBottom: Boolean = false): GameState? {
        val options = ZoneEntryOptions(
            controllerId = controller,
            skipZoneChangeRedirect = true,
            libraryPlacement = if (placeBottom) com.wingedsheep.engine.handlers.effects.LibraryPlacement.Bottom
            else com.wingedsheep.engine.handlers.effects.LibraryPlacement.Top,
        )
        val r = try {
            zones.moveToZone(s, id, zone, options)
        } catch (e: Exception) {
            lastError = "exception ${e::class.simpleName}: ${e.message?.take(160)}"
            return null
        }
        return r.state.takeIf { r.actualDestination == null || r.actualDestination == zone }
    }

    private fun isAura(name: String): Boolean =
        registry.getCard(snapshotter.engineName(name))?.typeLine?.subtypes?.any { it.value == "Aura" } == true

    private fun isFaceDown(s: GameState, id: EntityId) =
        s.getEntity(id)?.has<com.wingedsheep.engine.state.components.identity.FaceDownComponent>() == true

    /**
     * Attaches the Aura [aura] to a creature: for an Aura that steals its host, one [side] controls
     * but the other player owns (the record already shows it on [side]); an opponent's when its text
     * restrains or shrinks the enchanted creature; else one of [side]'s own; the other side's if that
     * side has none.
     */
    private fun attach(s: GameState, seats: Seats, side: String, aura: EntityId): GameState? {
        val text = registry.getCard(snapshotter.name(s, aura)?.let(snapshotter::engineName) ?: return null)?.oracleText.orEmpty()
        fun creatures(sd: String) = s.controlledBattlefield(seats.of(sd)).filter { it != aura && s.projectedState.hasType(it, "CREATURE") }
        val stolen = if (STEALS.containsMatchIn(text)) {
            creatures(side).firstOrNull { s.getEntity(it)?.get<OwnerComponent>()?.playerId == seats.of(other(side)) }
        } else null
        val hostile = HOSTILE_AURA.containsMatchIn(text)
        val sides = if (hostile) listOf(other(side), side) else listOf(side, other(side))
        val host = stolen ?: sides.firstNotNullOfOrNull { sd -> creatures(sd).firstOrNull() } ?: return null
        return s.updateEntity(aura) { it.with(AttachedToComponent(host)) }
            .updateEntity(host) { c ->
                c.with(AttachmentsComponent(c.get<AttachmentsComponent>()?.attachedIds.orEmpty() + aura))
            }
    }

    /** A new token that is [tokenFiller]'s vanilla creature, under [player]'s control; the record never names eot tokens. */
    private fun fillerToken(s: GameState, player: EntityId): GameState? {
        val def = tokenFiller()?.let { registry.getCard(it) } ?: return null
        val (id, next) = s.newEntity()
        val container = CardEntityFactory.create(def, player)
            .with(TokenComponent).with(ControllerComponent(player)).with(OwnerComponent(player))
        return next.withEntity(id, container).addToZone(ZoneKey(player, Zone.BATTLEFIELD), id)
    }

    /** A new token entity like [model], under [player]'s control. */
    private fun copyToken(s: GameState, model: EntityId, player: EntityId): GameState {
        val (id, next) = s.newEntity()
        val container = s.requireEntity(model)
            .without<AttachedToComponent>().without<AttachmentsComponent>()
            .with(ControllerComponent(player)).with(OwnerComponent(player))
        return next.withEntity(id, container).addToZone(ZoneKey(player, Zone.BATTLEFIELD), id)
    }

    companion object {
        private val SIDES = listOf("user", "oppo")
        private val STEALS = Regex("""(?i)\byou control enchanted (?:creature|permanent)""")
        private val HOSTILE_AURA = Regex("""(?i)enchanted (?:creature|permanent) (?:can't|doesn't|gets -|loses|has base power)""")
    }
}
