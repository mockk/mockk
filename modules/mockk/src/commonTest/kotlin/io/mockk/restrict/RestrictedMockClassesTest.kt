package io.mockk.restrict

import io.mockk.MockKSettings
import io.mockk.impl.restrict.RestrictedMockClasses
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.SimpleFormatter
import java.util.logging.StreamHandler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
    fun `should allow adding and removing custom restricted types`() {
        val customClass = String::class.java

        RestrictedMockClasses.addRestrictedType(customClass)
        assertTrue { RestrictedMockClasses.isRestricted(customClass) }

        RestrictedMockClasses.removeRestrictedType(customClass)
        assertFalse { RestrictedMockClasses.isRestricted(customClass) }
    }

    @Test
    fun `should clear user-defined restricted types while keeping defaults`() {
        val customClass = UUID::class.java

        RestrictedMockClasses.addRestrictedType(customClass)
        RestrictedMockClasses.clearUserDefinedRestrictions()

        assertFalse { RestrictedMockClasses.isRestricted(customClass) }
        assertTrue { RestrictedMockClasses.isRestricted(System::class.java) }
    }

    @Test
    fun `should log warning when attempting to mock restricted class if setting is default`() {
        val logOutput = captureLogs {
            RestrictedMockClasses.handleRestrictedMocking(File::class.java)
        }

        assertTrue { "Warning: Attempting to mock a restricted class (java.io.File)" in logOutput }
    }

    @Test
    fun `should log warning when mocking restricted class if setting is disabled`() {
        val logOutput = captureLogs {
            RestrictedMockClasses.handleRestrictedMocking(File::class.java)
        }

        MockKSettings.setDisallowMockingRestrictedClasses(false)

        RestrictedMockClasses.handleRestrictedMocking(File::class.java)
        assertTrue { "Warning: Attempting to mock a restricted class (java.io.File)" in logOutput }
    }

    @Test
    fun `should throws an exception when attempting to mock restricted class if setting is true`() {
        MockKSettings.setDisallowMockingRestrictedClasses(true)

        val ex = assertThrows<IllegalArgumentException> {
            RestrictedMockClasses.handleRestrictedMocking(File::class.java)
        }

        assertEquals(ex.message, "Cannot mock restricted class: java.io.File")
    }

    @Test
    fun `should throw an exception when mocking a user-defined restricted class if setting is enabled`() {
        MockKSettings.setDisallowMockingRestrictedClasses(true)
        val customClass = UUID::class.java
        RestrictedMockClasses.addRestrictedType(customClass)

        val ex = assertThrows<IllegalArgumentException> {
            RestrictedMockClasses.handleRestrictedMocking(customClass)
        }

        assertEquals(ex.message, "Cannot mock restricted class: java.util.UUID")
    }
}
