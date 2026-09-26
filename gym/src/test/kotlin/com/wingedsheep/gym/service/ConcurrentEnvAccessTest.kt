package com.wingedsheep.gym.service

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Regression coverage for the service-level single-threaded-per-environment contract. */
class ConcurrentEnvAccessTest : FunSpec({
    fun registry(): CardRegistry = CardRegistry().apply {
        register(PortalSet.cards)
        register(PortalSet.basicLands)
    }

    fun config(): EnvConfig {
        val deck = DeckSpec.Explicit(mapOf("Mountain" to 17, "Raging Goblin" to 3))
        return EnvConfig(
            players = listOf(
                PlayerSpec("Alice", deck),
                PlayerSpec("Bob", deck)
            ),
            skipMulligans = true,
            startingPlayerIndex = 0
        )
    }

    fun envMonitor(service: MultiEnvService, envId: EnvId): Any {
        // Reflection is deliberate: these tests need a deterministic way to hold the exact
        // synchronization boundary rather than relying on a scheduler race that could make the old
        // unsafe implementation pass intermittently.
        val envsField = MultiEnvService::class.java.getDeclaredField("envs").apply {
            isAccessible = true
        }
        @Suppress("UNCHECKED_CAST")
        val envs = envsField.get(service) as ConcurrentHashMap<EnvId, Any>
        return envs.getValue(envId)
    }

    test("operations on the same env are serialized at the service boundary") {
        val service = MultiEnvService(registry())
        val executor = Executors.newSingleThreadExecutor()

        try {
            val envId = service.create(config()).envId
            val monitor = envMonitor(service, envId)
            val started = CountDownLatch(1)
            val finished = CountDownLatch(1)

            synchronized(monitor) {
                executor.submit {
                    started.countDown()
                    service.observe(envId)
                    finished.countDown()
                }

                started.await(1, TimeUnit.SECONDS).shouldBeTrue()
                finished.await(100, TimeUnit.MILLISECONDS).shouldBeFalse()
            }

            finished.await(2, TimeUnit.SECONDS).shouldBeTrue()
        } finally {
            executor.shutdownNow()
            service.workerPool.close()
        }
    }

    test("dispose waits for an in-flight env operation and makes later access fail") {
        val service = MultiEnvService(registry())
        val executor = Executors.newSingleThreadExecutor()

        try {
            val envId = service.create(config()).envId
            val monitor = envMonitor(service, envId)
            val started = CountDownLatch(1)
            val finished = CountDownLatch(1)

            synchronized(monitor) {
                executor.submit {
                    started.countDown()
                    service.dispose(listOf(envId))
                    finished.countDown()
                }

                started.await(1, TimeUnit.SECONDS).shouldBeTrue()
                finished.await(100, TimeUnit.MILLISECONDS).shouldBeFalse()
                service.listEnvs().contains(envId).shouldBeTrue()
            }

            finished.await(2, TimeUnit.SECONDS).shouldBeTrue()
            service.listEnvs().contains(envId).shouldBeFalse()
            shouldThrow<NoSuchElementException> { service.observe(envId) }
        } finally {
            executor.shutdownNow()
            service.workerPool.close()
        }
    }
})
