package com.wingedsheep.gym.exit

import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicLong

/**
 * docs/65 §3 item 3: one batching queue per JVM. Every search (one per game lane, each on its own thread) hands its
 * encoder requests here and blocks; one inference thread drains whatever is waiting, up to [maxRows] observations,
 * and runs it as one padded batch. A search makes one request at a time, so a batch holds at most one request per
 * lane, and with one lane this is the direct call plus a hand-off.
 */
class BatchingEvaluator(private val policy: GameplayPolicy, private val maxRows: Int = 64) : Evaluator, AutoCloseable {
    private class Request(val prepared: List<Prepared>, val result: CompletableFuture<List<Encoded>> = CompletableFuture())

    private val queue = LinkedBlockingQueue<Request>()
    private val stop = Request(emptyList())
    val batches = AtomicLong()
    val rows = AtomicLong()
    val inferenceNanos = AtomicLong()

    private val worker = Thread({
        val taken = ArrayList<Request>()
        while (true) {
            val first = queue.take()
            if (first === stop) return@Thread
            taken += first
            var n = first.prepared.size
            while (n < maxRows) {
                val next = queue.peek() ?: break
                if (next === stop || n + next.prepared.size > maxRows) break
                taken += queue.poll()
                n += next.prepared.size
            }
            try {
                val t0 = System.nanoTime()
                val out = policy.encode(taken.flatMap { it.prepared })
                inferenceNanos.addAndGet(System.nanoTime() - t0)
                batches.incrementAndGet()
                rows.addAndGet(n.toLong())
                var at = 0
                for (r in taken) {
                    r.result.complete(out.subList(at, at + r.prepared.size))
                    at += r.prepared.size
                }
            } catch (e: Throwable) {
                for (r in taken) r.result.completeExceptionally(e)
            }
            taken.clear()
        }
    }, "e1-inference").apply { isDaemon = true; start() }

    override fun encode(prepared: List<Prepared>): List<Encoded> {
        val request = Request(prepared)
        queue.put(request)
        return try {
            request.result.get()
        } catch (e: java.util.concurrent.ExecutionException) {
            throw e.cause ?: e
        }
    }

    override fun close() {
        queue.put(stop)
        worker.join()
    }
}
