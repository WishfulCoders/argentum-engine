package com.wingedsheep.gameserver.matchmaking

import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.UUID

class MatchmakingQueueTest : FunSpec({

    val casualLimited = QueueKey(MatchmakingMode.RANDOM_DECK, format = null, ranked = false)
    val rankedLimited = QueueKey(MatchmakingMode.RANDOM_DECK, format = null, ranked = true)
    val everyone: (EntityId) -> Boolean = { true }

    fun entry(
        name: String,
        key: QueueKey = casualLimited,
        rating: Double = 1200.0,
        joinedAt: Long = 0,
        userId: UUID? = UUID.nameUUIDFromBytes(name.toByteArray()),
    ) = QueueEntry(EntityId(name), userId, name, key, rating, joinedAt)

    fun queue(): MatchmakingQueue {
        var n = 0
        return MatchmakingQueue(acceptWindowMs = 15_000, newMatchId = { "m${++n}" })
    }

    fun List<MatchmakingEvent>.found() = filterIsInstance<MatchmakingEvent.Found>()

    test("casual pairs the two longest-waiting players in the same queue") {
        val q = queue()
        q.join(entry("late", joinedAt = 300))
        q.join(entry("first", joinedAt = 100))
        q.join(entry("second", joinedAt = 200))

        val found = q.tick(now = 1_000, everyone).found().single().match
        found.players.map { it.playerName } shouldContainExactly listOf("first", "second")
        q.counts() shouldBe mapOf(casualLimited to 1)
    }

    test("different queues never pair, including casual and ranked of the same format") {
        val q = queue()
        q.join(entry("casual", key = casualLimited))
        q.join(entry("ranked", key = rankedLimited))
        q.join(entry("standard", key = QueueKey(MatchmakingMode.CONSTRUCTED, DeckFormat.STANDARD, ranked = false)))
        q.join(entry("momir", key = QueueKey(MatchmakingMode.MOMIR_BASIC, format = null, ranked = false)))
        q.join(entry("jumpin", key = QueueKey(MatchmakingMode.JUMP_IN, format = null, ranked = false)))

        q.tick(now = 0, everyone).found().shouldBeEmpty()
        q.counts().values.sum() shouldBe 5
    }

    test("a join request resolves to one consistent queue key") {
        QueueKey.of(MatchmakingMode.JUMP_IN, DeckFormat.PAUPER, ranked = false) shouldBe
            QueueKey.Result.Valid(QueueKey(MatchmakingMode.JUMP_IN, format = null, ranked = false))
        QueueKey.of(MatchmakingMode.MOMIR_BASIC, null, ranked = true).shouldBeInstanceOf<QueueKey.Result.Invalid>()
        QueueKey.of(MatchmakingMode.CONSTRUCTED, null, ranked = false).shouldBeInstanceOf<QueueKey.Result.Invalid>()
        QueueKey.of(MatchmakingMode.RANDOM_DECK, null, ranked = true) shouldBe QueueKey.Result.Valid(rankedLimited)
        // An older client sends no mode: a format is Constructed, no format the Random deck queue.
        QueueKey.of(null, DeckFormat.PAUPER, ranked = false) shouldBe
            QueueKey.Result.Valid(QueueKey(MatchmakingMode.CONSTRUCTED, DeckFormat.PAUPER, ranked = false))
        QueueKey.of(null, null, ranked = false) shouldBe QueueKey.Result.Valid(casualLimited)
    }

    test("the same account in two tabs never plays itself") {
        val q = queue()
        val account = UUID.randomUUID()
        q.join(entry("tab1", userId = account))
        q.join(entry("tab2", userId = account))

        q.tick(now = 0, everyone).found().shouldBeEmpty()
    }

    test("a blocked pair never meets, and each still pairs with someone else") {
        val q = queue()
        q.join(entry("alice", joinedAt = 0))
        q.join(entry("bob", joinedAt = 10))
        q.join(entry("carol", joinedAt = 20))
        val blocked = { a: QueueEntry, b: QueueEntry -> setOf(a.playerName, b.playerName) == setOf("alice", "bob") }

        val found = q.tick(now = 1_000, everyone, blocked).found().single().match
        found.players.map { it.playerName } shouldContainExactly listOf("alice", "carol")
        q.entryOf(EntityId("bob")).shouldNotBeNull()
    }

    test("two guests (no account) can pair") {
        val q = queue()
        q.join(entry("guest1", userId = null))
        q.join(entry("guest2", userId = null))

        q.tick(now = 0, everyone).found() shouldHaveSize 1
    }

    test("ranked waits for ratings to come within the band, which widens with time") {
        val q = queue()
        q.join(entry("low", key = rankedLimited, rating = 1000.0, joinedAt = 0))
        q.join(entry("high", key = rankedLimited, rating = 1300.0, joinedAt = 0))

        // 300 apart: outside the starting 100 band.
        q.tick(now = 0, everyone).found().shouldBeEmpty()
        // After 40s the band is 100 + 4 × 50 = 300.
        q.tick(now = 39_999, everyone).found().shouldBeEmpty()
        q.tick(now = 40_000, everyone).found() shouldHaveSize 1
    }

    test("ranked opens fully after two minutes") {
        MatchmakingQueue.ratingBand(119_999) shouldBe 100.0 + 50.0 * 11
        MatchmakingQueue.ratingBand(120_000) shouldBe Double.POSITIVE_INFINITY

        val q = queue()
        q.join(entry("novice", key = rankedLimited, rating = 800.0))
        q.join(entry("master", key = rankedLimited, rating = 2400.0))
        q.tick(now = 120_000, everyone).found() shouldHaveSize 1
    }

    test("ranked pairs the longest waiter with the closest rating, not the next in line") {
        val q = queue()
        q.join(entry("anchor", key = rankedLimited, rating = 1200.0, joinedAt = 0))
        q.join(entry("far", key = rankedLimited, rating = 1290.0, joinedAt = 1))
        q.join(entry("near", key = rankedLimited, rating = 1210.0, joinedAt = 2))

        val match = q.tick(now = 2, everyone).found().single().match
        match.players.map { it.playerName } shouldContainExactly listOf("anchor", "near")
    }

    test("both accepting confirms the match") {
        val q = queue()
        q.join(entry("a"))
        q.join(entry("b"))
        val match = q.tick(now = 0, everyone).found().single().match

        q.respond(EntityId("a"), match.matchId, accept = true).single()
            .shouldBeInstanceOf<MatchmakingEvent.Accepted>()
        q.respond(EntityId("b"), match.matchId, accept = true).single()
            .shouldBeInstanceOf<MatchmakingEvent.Confirmed>()
        q.pendingMatchOf(EntityId("a")).shouldBeNull()
        q.counts() shouldBe emptyMap()
    }

    test("a decline drops the decliner and requeues an opponent at their original place") {
        val q = queue()
        q.join(entry("a", joinedAt = 10))
        q.join(entry("b", joinedAt = 20))
        val match = q.tick(now = 100, everyone).found().single().match
        q.respond(EntityId("a"), match.matchId, accept = true)

        val events = q.respond(EntityId("b"), match.matchId, accept = false)
        events.filterIsInstance<MatchmakingEvent.Idle>().single().playerId shouldBe EntityId("b")
        val requeued = events.filterIsInstance<MatchmakingEvent.Searching>().single()
        requeued.entry.playerId shouldBe EntityId("a")
        requeued.notice.shouldNotBeNull()
        q.entryOf(EntityId("a"))?.joinedAt shouldBe 10
        q.entryOf(EntityId("b")).shouldBeNull()
    }

    test("leaving while a prompt is open declines it") {
        val q = queue()
        q.join(entry("a"))
        q.join(entry("b"))
        q.tick(now = 0, everyone)

        q.leave(EntityId("a"))
        q.pendingMatchOf(EntityId("b")).shouldBeNull()
        q.entryOf(EntityId("b")).shouldNotBeNull()
    }

    test("an unanswered prompt expires; only the player who accepted is requeued") {
        val q = queue()
        q.join(entry("a"))
        q.join(entry("b"))
        val match = q.tick(now = 0, everyone).found().single().match
        q.respond(EntityId("a"), match.matchId, accept = true)

        q.tick(now = 14_999, everyone).shouldBeEmpty()
        val events = q.tick(now = 15_000, everyone)
        events.filterIsInstance<MatchmakingEvent.Idle>().single().playerId shouldBe EntityId("b")
        q.entryOf(EntityId("a")).shouldNotBeNull()
    }

    test("an unanswered prompt drops both players when neither accepted") {
        val q = queue()
        q.join(entry("a"))
        q.join(entry("b"))
        q.tick(now = 0, everyone)

        q.tick(now = 15_000, everyone).filterIsInstance<MatchmakingEvent.Idle>() shouldHaveSize 2
        q.counts() shouldBe emptyMap()
    }

    test("unavailable players leave the queue and their pending matches") {
        val q = queue()
        q.join(entry("a"))
        q.join(entry("b"))
        q.tick(now = 0, everyone)
        q.join(entry("c"))

        val gone = setOf(EntityId("b"), EntityId("c"))
        val events = q.tick(now = 1, isAvailable = { it !in gone })
        events.filterIsInstance<MatchmakingEvent.Idle>().map { it.playerId }.toSet() shouldBe gone
        q.entryOf(EntityId("a")).shouldNotBeNull()
        q.pendingMatchOf(EntityId("a")).shouldBeNull()
    }

    test("joining while a prompt is open is refused") {
        val q = queue()
        q.join(entry("a"))
        q.join(entry("b"))
        q.tick(now = 0, everyone)

        q.join(entry("a", key = rankedLimited)).shouldBeEmpty()
        q.pendingMatchOf(EntityId("a")).shouldNotBeNull()
    }

    test("joining again switches queues") {
        val q = queue()
        q.join(entry("a", key = casualLimited))
        q.join(entry("a", key = rankedLimited))
        q.counts() shouldBe mapOf(rankedLimited to 1)
    }

    test("a stale or foreign match id is ignored") {
        val q = queue()
        q.join(entry("a"))
        q.join(entry("b"))
        val match = q.tick(now = 0, everyone).found().single().match

        q.respond(EntityId("a"), "nope", accept = true).shouldBeEmpty()
        q.respond(EntityId("outsider"), match.matchId, accept = false).shouldBeEmpty()
        q.pendingMatchOf(EntityId("a")).shouldNotBeNull()
    }
})
