package io.mockk.it

import io.mockk.MockKException
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

/**
 * GitHub issue #1538: on JDK 24 creating a mock of a class whose `<clinit>` fails (an analytics
 * SDK class with dependencies missing on the JVM) died with
 * `InternalError: class redefinition failed: invalid class` from the revert retransformation of
 * the class left in an erroneous state, while older JDKs surfaced the raw
 * `ExceptionInInitializerError`. Allocating a mock instance initializes the class, so mocking
 * such a class cannot work; the reported failure must be the initialization failure itself,
 * not an internal error.
 */
class Issue1538FailingClinitMockTest {
    @Test
    fun `mock of a class with failing clinit fails with the initialization failure`() {
        val failure =
            assertThrows<MockKException> {
                mockk<ClinitFailingClass>()
            }

        val chain = generateSequence<Throwable>(failure) { it.cause }.toList()
        assertTrue(
            chain.none { it is InternalError },
            "expected the initialization failure to be reported, got: $failure",
        )

        val initializationFailure = chain.filterIsInstance<LinkageError>().first()
        assertEquals(
            "simulated failing <clinit>, see #1538",
            initializationFailure.cause?.message,
        )
    }
}
