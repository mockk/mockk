package io.mockk.it

import io.mockk.every
import io.mockk.spyk
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A spy produced by `spyk` with an `objectToCopy` has to behave like one produced by
 * `spyk<T>()`, which instantiates the object through the constructor (#1262).
 *
 * With an `objectToCopy`, the proxy is created without running the constructor and the
 * field values are copied over afterwards. Copying a `by lazy` delegate field verbatim
 * means the copied `Lazy` instance is bound to the original object, so once it computes,
 * it reads the original's properties and never sees the stubs installed on the spy.
 */
class SpykObjectToCopyPropertyTest {
    class Person {
        val firstName: String = "Alan"
        val surname: String = "Turing"

        val fullname by lazy {
            "$firstName $surname"
        }
    }

    @Test
    fun defaultConstructorPathUsesMockedProperties() {
        val mockedPerson =
            spyk<Person> {
                every { firstName } returns "John"
                every { surname } returns "Doe"
            }

        assertEquals("John", mockedPerson.firstName)
        assertEquals("John Doe", mockedPerson.fullname)
    }

    @Test
    fun objectToCopyPathUsesMockedProperties() {
        val mockedPerson =
            spyk(Person()) {
                every { firstName } returns "John"
                every { surname } returns "Doe"
            }

        assertEquals("John", mockedPerson.firstName)
        assertEquals("John Doe", mockedPerson.fullname)
    }

    class Counter {
        var hits: Int = 0

        val report by lazy {
            "hits=$hits"
        }
    }

    @Test
    fun objectToCopyPathLazyComputesFromCopiedState() {
        val mockedCounter = spyk(Counter().apply { hits = 42 })

        assertEquals("hits=42", mockedCounter.report)
    }

    @Test
    fun objectToCopyPathLazyUsesMockedProperties() {
        val mockedCounter =
            spyk(Counter().apply { hits = 42 }) {
                every { hits } returns 7
            }

        assertEquals(7, mockedCounter.hits)
        assertEquals("hits=7", mockedCounter.report)
    }
}
