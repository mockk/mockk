package io.mockk.restrict

import io.mockk.MockKSettings
import io.mockk.impl.annotations.MockkRestricted
import io.mockk.impl.annotations.MockkRestrictedMode
import io.mockk.impl.restrict.MockingRestrictedExtension
import io.mockk.impl.restrict.RestrictedMockClasses
import io.mockk.mockk
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.SimpleFormatter
import java.util.logging.StreamHandler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(MockingRestrictedExtension::class)
class RestrictedMockClassesTest {

    private fun captureLogs(action: () -> Unit): String {
        val logger = Logger.getLogger(RestrictedMockClasses::class.java.name)
        val logCapture = StringBuilder()
        val handler = object : StreamHandler() {
            override fun publish(record: java.util.logging.LogRecord) {
                logCapture.append(SimpleFormatter().format(record))
            }
        }

        logger.level = Level.WARNING
        logger.addHandler(handler)

        action()

        handler.flush()
        logger.removeHandler(handler)
        return logCapture.toString()
    }

    @Test
    fun `should detect default restricted types`() {
        assertTrue { RestrictedMockClasses.isRestricted(System::class.java) }

        assertTrue { RestrictedMockClasses.isRestricted(ArrayList::class.java) }
        assertTrue { RestrictedMockClasses.isRestricted(HashSet::class.java) }
        assertTrue { RestrictedMockClasses.isRestricted(TreeMap::class.java) }
        assertTrue { RestrictedMockClasses.isRestricted(LinkedList::class.java) }

        assertTrue { RestrictedMockClasses.isRestricted(File::class.java) }
        assertTrue { RestrictedMockClasses.isRestricted(Path::class.java) }
    }

    @Test
    fun `should log warning when attempting to mock restricted class if setting is default`() {
        assertDoesNotThrow { mockk<File>() }
    }

    @Test
    fun `If the annotation is not used, a warning log is recorded`() {
        assertDoesNotThrow { mockk<File>() }
    }

    @Test
    @MockkRestricted(mode = MockkRestrictedMode.WARN)
    fun `WARN value of the annotation is a warning log`() {
        assertDoesNotThrow {
            mockk<File>()
        }
    }

    @Test
    @MockkRestricted(mode = MockkRestrictedMode.EXCEPTION)
    fun `If the annotation's option is set, it should throw an IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> {
            mockk<File>()
        }
    }

    @Test
    @MockkRestricted(mode = MockkRestrictedMode.EXCEPTION, restricted = [UUID::class, RestrictedMockAtomicTest.Foo::class])
    fun `custom classes can be configured`() {

        assertDoesNotThrow {
            mockk<Date>()
        }

        assertThrows<IllegalArgumentException> {
            mockk<RestrictedMockAtomicTest.Foo>()
        }
    }

    @Test
    fun `should throws an exception when attempting to mock restricted class if setting is true`() {
        MockKSettings.setDisallowMockingRestrictedClasses(true)

        val ex = assertThrows<IllegalArgumentException> {
            RestrictedMockClasses.handleRestrictedMocking(File::class.java)
        }

        assertEquals(ex.message, "Cannot mock restricted class: java.io.File")
    }
}
