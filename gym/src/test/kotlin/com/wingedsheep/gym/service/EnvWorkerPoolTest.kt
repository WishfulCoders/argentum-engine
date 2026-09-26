package com.wingedsheep.gym.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/**
 * The pool must propagate a task's *own* exception, not the `ExecutionException` its `Future` wraps
 * it in.
 *
 * `GymExceptionHandler` maps `IllegalArgumentException` → 400 and `IllegalStateException` → 409, and
 * an `ExecutionException` matches neither, so before the unwrap a rejected action inside
 * `POST /envs/step-batch` surfaced as a 500 while the identical action on `POST /envs/{id}/step`
 * returned 400 — and so did a *batch of one*, which takes the single-task fast path. Engine
 * rejections are a routine result now that a mis-declared attack raises instead of silently
 * no-opping, so the two paths have to agree.
 */
class EnvWorkerPoolTest : FunSpec({

    val pool = EnvWorkerPool(parallelism = 2)
    afterSpec { pool.close() }

    test("results come back in submission order") {
        val tasks = (1..4).map { n -> Callable { n * n } }
        pool.invokeAll(tasks) shouldBe listOf(1, 4, 9, 16)
    }

    test("a failing task propagates its own exception type, not ExecutionException") {
        val boom = Callable<Int> { throw IllegalArgumentException("rejected by the engine") }

        withClue("a batch of one — the fast path, which never wrapped") {
            shouldThrow<IllegalArgumentException> { pool.invokeAll(listOf(boom)) }
        }
        withClue("a real multi-task batch, which submits to the pool and unwraps the future") {
            val thrown = shouldThrow<IllegalArgumentException> {
                pool.invokeAll(listOf(Callable { 1 }, boom, Callable { 3 }))
            }
            // The *type* is what the exception handler dispatches on, and it survives. The message
            // picks up a class-name prefix on the way: `ForkJoinTask` reconstructs an exception that
            // crosses threads from the original's `toString()`, which is its behaviour, not ours.
            thrown.message shouldContain "rejected by the engine"
        }
    }

    test("an IllegalStateException keeps its identity too, so it can still map to 409") {
        shouldThrow<IllegalStateException> {
            pool.invokeAll(listOf(Callable { 1 }, Callable<Int> { throw IllegalStateException("nope") }))
        }
    }

    test("a failing batch waits for every submitted task to settle before propagating") {
        val testPool = EnvWorkerPool(parallelism = 1)
        val laterTaskStarted = CountDownLatch(1)
        val releaseLaterTask = CountDownLatch(1)
        val callFinished = CountDownLatch(1)
        val thrown = AtomicReference<Throwable?>()

        val caller = thread(name = "env-worker-pool-settle-test") {
            try {
                testPool.invokeAll(
                    listOf(
                        Callable<Int> { throw IllegalArgumentException("first task failed") },
                        Callable {
                            laterTaskStarted.countDown()
                            releaseLaterTask.await()
                            2
                        }
                    )
                )
            } catch (error: Throwable) {
                thrown.set(error)
            } finally {
                callFinished.countDown()
            }
        }

        try {
            laterTaskStarted.await(1, TimeUnit.SECONDS) shouldBe true

            // The first task has already failed and the second is deliberately blocked. The batch
            // call must therefore still be waiting rather than reporting failure early while a
            // submitted task can continue mutating state behind the caller's back.
            callFinished.await(100, TimeUnit.MILLISECONDS) shouldBe false

            releaseLaterTask.countDown()
            callFinished.await(1, TimeUnit.SECONDS) shouldBe true
            (thrown.get() is IllegalArgumentException) shouldBe true
            thrown.get()?.message.orEmpty() shouldContain "first task failed"
        } finally {
            releaseLaterTask.countDown()
            caller.join(1_000)
            testPool.close()
        }
    }

    test("an interrupted batch settles every submitted task before restoring interruption") {
        val testPool = EnvWorkerPool(parallelism = 1)
        val firstTaskStarted = CountDownLatch(1)
        val releaseFirstTask = CountDownLatch(1)
        val callFinished = CountDownLatch(1)
        val thrown = AtomicReference<Throwable?>()
        val interruptedAtCatch = AtomicReference(false)

        val caller = thread(name = "env-worker-pool-interrupt-test") {
            try {
                testPool.invokeAll(
                    listOf(
                        Callable {
                            firstTaskStarted.countDown()
                            releaseFirstTask.await()
                            1
                        },
                        Callable<Int> { throw IllegalStateException("worker failed while draining") }
                    )
                )
            } catch (error: Throwable) {
                thrown.set(error)
                interruptedAtCatch.set(Thread.currentThread().isInterrupted)
            } finally {
                callFinished.countDown()
            }
        }

        try {
            firstTaskStarted.await(1, TimeUnit.SECONDS) shouldBe true
            caller.interrupt()

            // Interruption must not publish an abandoned-batch boundary while the first worker can
            // still mutate state and the second worker has not even run yet.
            callFinished.await(100, TimeUnit.MILLISECONDS) shouldBe false

            releaseFirstTask.countDown()
            callFinished.await(1, TimeUnit.SECONDS) shouldBe true

            val interruption = thrown.get()
            (interruption is InterruptedException) shouldBe true
            interruptedAtCatch.get() shouldBe true
            interruption?.suppressed?.any {
                it is IllegalStateException && it.message.orEmpty().contains("worker failed while draining")
            } shouldBe true
        } finally {
            releaseFirstTask.countDown()
            caller.join(1_000)
            testPool.close()
        }
    }
})
