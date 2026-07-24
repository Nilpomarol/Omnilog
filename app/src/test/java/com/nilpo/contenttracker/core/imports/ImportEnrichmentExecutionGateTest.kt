package com.nilpo.contenttracker.core.imports

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportEnrichmentExecutionGateTest {
    @Test
    fun `serializes concurrent enrichment work`() = runBlocking {
        val activeCount = AtomicInteger(0)
        val maximumActiveCount = AtomicInteger(0)

        coroutineScope {
            List(2) {
                async {
                    ImportEnrichmentExecutionGate.run {
                        val active = activeCount.incrementAndGet()
                        maximumActiveCount.updateAndGet { maxOf(it, active) }
                        delay(20)
                        activeCount.decrementAndGet()
                    }
                }
            }.awaitAll()
        }

        assertEquals(1, maximumActiveCount.get())
        assertEquals(0, activeCount.get())
    }
}
