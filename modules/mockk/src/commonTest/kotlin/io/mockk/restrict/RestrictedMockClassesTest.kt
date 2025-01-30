package io.mockk.restrict

import io.mockk.impl.restrict.RestrictedMockClasses
import io.mockk.mockk
import kotlin.test.Test
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.SimpleFormatter
import java.util.logging.StreamHandler
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
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
    fun `should log warning when attempting to mock restricted class`() {
        val logOutput = captureLogs {
            RestrictedMockClasses.warnIfRestricted(File::class.java)
        }

        assertTrue { "Warning: Attempting to mock a restricted class (java.io.File)" in logOutput }
    }
}
