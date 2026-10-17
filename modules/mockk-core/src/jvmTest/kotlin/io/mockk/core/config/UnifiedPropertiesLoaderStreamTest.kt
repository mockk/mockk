package io.mockk.core.config

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class UnifiedPropertiesLoaderStreamTest {
    @Test
    fun `concurrent initial callers receive complete independent results`() =
        IsolatedPropertiesLoader().use { fixture ->
            val workerCount = 8
            val ready = CountDownLatch(workerCount)
            val start = CountDownLatch(1)
            val firstLookup = CountDownLatch(1)
            val releaseLookup = CountDownLatch(1)
            fixture.unifiedStreamFactory = {
                firstLookup.countDown()
                releaseLookup.awaitChecked()
                ByteArrayInputStream(bytes("name=complete\nsecond=value"))
            }
            val loader = fixture.propertiesLoader
            val executor = Executors.newFixedThreadPool(workerCount)
            try {
                val results =
                    List(workerCount) {
                        executor.submit(
                            Callable {
                                ready.countDown()
                                start.awaitChecked()
                                loader.loadProperties()
                            },
                        )
                    }
                ready.awaitChecked()
                start.countDown()
                firstLookup.awaitChecked()
                assertTrue(results.none { it.isDone })
                releaseLookup.countDown()
                val properties = results.map { it.get(10, TimeUnit.SECONDS) }
                properties.forEach {
                    assertEquals("complete", it.getProperty("name"))
                    assertEquals("value", it.getProperty("second"))
                }
                properties.first().setProperty("second", "changed")
                properties.drop(1).forEach { assertEquals("value", it.getProperty("second")) }
                assertEquals(1, fixture.resourceReads)
            } finally {
                start.countDown()
                releaseLookup.countDown()
                executor.shutdownNow()
                assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "Test workers did not terminate")
            }
        }

    @Test
    fun `a successful resource is read and closed once`() =
        IsolatedPropertiesLoader().use { fixture ->
            val closes = AtomicInteger()
            fixture.unifiedStreamFactory = {
                object : ByteArrayInputStream(bytes("name=value")) {
                    override fun close() {
                        closes.incrementAndGet()
                    }
                }
            }
            repeat(2) { assertEquals("value", fixture.propertiesLoader.loadProperties().getProperty("name")) }
            assertEquals(1, closes.get())
            assertEquals(1, fixture.resourceReads)
        }

    @Test
    fun `a read failure is propagated closed and retryable`() =
        IsolatedPropertiesLoader().use { fixture ->
            val failure = IOException("read failed")
            val closes = AtomicInteger()
            fixture.unifiedStreamFactory = { PrefixThenFailure(bytes("partial=value\n"), failure, closes) }
            assertSame(failure, assertFailsWith<IOException> { fixture.propertiesLoader.loadProperties() })
            assertEquals(1, closes.get())
            fixture.unifiedStreamFactory = { ByteArrayInputStream(bytes("name=recovered")) }
            val recovered = fixture.propertiesLoader.loadProperties()
            assertEquals("recovered", recovered.getProperty("name"))
            assertNull(recovered.getProperty("partial"))
            assertEquals(2, fixture.resourceReads)
        }

    @Test
    fun `a close failure is propagated and a later read can succeed`() =
        IsolatedPropertiesLoader().use { fixture ->
            val failure = IOException("close failed")
            val closes = AtomicInteger()
            fixture.unifiedStreamFactory = { CloseFailureStream(bytes("name=value"), failure, closes) }
            assertSame(failure, assertFailsWith<IOException> { fixture.propertiesLoader.loadProperties() })
            assertEquals(1, closes.get())
            fixture.unifiedStreamFactory = { ByteArrayInputStream(bytes("name=recovered")) }
            assertEquals("recovered", fixture.propertiesLoader.loadProperties().getProperty("name"))
            assertEquals(2, fixture.resourceReads)
        }
}

private fun bytes(contents: String): ByteArray = contents.toByteArray(Charsets.ISO_8859_1)

private fun CountDownLatch.awaitChecked() {
    check(await(10, TimeUnit.SECONDS)) { "Timed out waiting for controlled stream" }
}

private class PrefixThenFailure(
    contents: ByteArray,
    private val failure: IOException,
    private val closes: AtomicInteger,
) : InputStream() {
    private val prefix = ByteArrayInputStream(contents)

    override fun read(): Int = prefix.read().also { if (it == -1) throw failure }

    override fun read(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ): Int {
        if (length == 0) return 0
        return prefix.read(buffer, offset, length).also { if (it == -1) throw failure }
    }

    override fun close() {
        closes.incrementAndGet()
    }
}

private class CloseFailureStream(
    contents: ByteArray,
    private val failure: IOException,
    private val closes: AtomicInteger,
) : ByteArrayInputStream(contents) {
    override fun close() {
        closes.incrementAndGet()
        throw failure
    }
}
