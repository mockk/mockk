package io.mockk.proxy.common.transformation

import io.mockk.proxy.MockKAgentLogger
import io.mockk.proxy.common.transformation.TransformationType.SIMPLE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.ArrayList

class RetransformInlineInstrumentationTest {
    @Test
    fun `class stays known to the transformer until it has been retransformed back`() {
        val instrumentation = RecordingInstrumentation()
        val cancellation = instrumentation.execute(request())

        assertTrue(instrumentation.specMap.shouldTransform(ArrayList::class.java))

        cancellation()

        // The transformers of the JVM and of the Android agent look the class up while the runtime
        // asks for its new bytecode. The Android agent only reverts a class the transformer knows
        // about, so the record has to survive the retransformation (#1630).
        assertEquals(listOf(ArrayList::class.java), instrumentation.retransformedBack)
        val specDuringRevert = instrumentation.specsSeenWhileRetransformingBack[ArrayList::class.java]
        assertNotNull(specDuringRevert, "the class was no longer known to its transformer")
        assertFalse(specDuringRevert!!.shouldDoSomething)

        // Once the class is reverted nothing may linger in the registry (#1289).
        assertNull(instrumentation.specMap[ArrayList::class.java])
        assertFalse(instrumentation.specMap.shouldTransform(ArrayList::class.java))
    }

    @Test
    fun `record outlives a class that is still intercepted`() {
        val instrumentation = RecordingInstrumentation()
        val cancellation = instrumentation.execute(request())
        instrumentation.execute(request())

        cancellation()

        assertTrue(instrumentation.retransformedBack.isEmpty())
        assertNotNull(instrumentation.specMap[ArrayList::class.java])
        assertTrue(instrumentation.specMap.shouldTransform(ArrayList::class.java))
    }

    @Test
    fun `record is dropped even if reverting the class fails`() {
        val instrumentation = RecordingInstrumentation()
        val cancellation = instrumentation.execute(request())
        instrumentation.failureWhileRetransformingBack = IllegalStateException("boom")

        assertThrows(IllegalStateException::class.java) { cancellation() }

        assertNull(instrumentation.specMap[ArrayList::class.java])
        assertFalse(instrumentation.specMap.shouldTransform(ArrayList::class.java))
    }

    @Test
    fun `rollback does not link a class that only exists on a desktop JVM`() {
        // Android has no java.lang.instrument.UnmodifiableClassException. Checking an exception
        // against that type makes its runtime resolve the class and fail with
        // NoClassDefFoundError, which replaces the error that actually happened (#1630).
        val linkedClasses = linkedClassesOf(RetransformInlineInstrumentation::class.java)

        assertFalse(linkedClasses.contains("java/lang/instrument/UnmodifiableClassException"))
    }

    private fun request() = TransformationRequest(setOf(ArrayList::class.java), SIMPLE)

    private fun linkedClassesOf(type: Class<*>): Set<String> {
        val resourceName = type.name.replace('.', '/') + ".class"
        val bytes = type.classLoader.getResourceAsStream(resourceName).use { it!!.readBytes() }

        var offset = 8 // magic, minor_version, major_version

        fun u2(): Int {
            val value = ((bytes[offset].toInt() and 0xff) shl 8) or (bytes[offset + 1].toInt() and 0xff)
            offset += 2
            return value
        }
        val count = u2()
        val utf8 = arrayOfNulls<String>(count)
        val classNameIndexes = mutableListOf<Int>()

        var index = 1
        while (index < count) {
            when (val tag = bytes[offset].toInt() and 0xff) {
                1 -> { // Utf8
                    offset++
                    val length = u2()
                    utf8[index] = String(bytes, offset, length, Charsets.UTF_8)
                    offset += length
                }
                7 -> { // Class
                    offset++
                    classNameIndexes.add(u2())
                }
                3, 4 -> offset += 5 // Integer, Float
                5, 6 -> { // Long, Double: occupy two slots
                    offset += 9
                    index++
                }
                8 -> offset += 3 // String
                9, 10, 11, 12 -> offset += 5 // Field, Method, InterfaceMethod, NameAndType
                15 -> offset += 4 // MethodHandle
                16, 19, 20 -> offset += 3 // MethodType, Module, Package
                17, 18 -> offset += 7 // Dynamic, InvokeDynamic
                else -> error("Unknown constant pool tag $tag in $resourceName at index $index")
            }
            index++
        }
        return classNameIndexes.mapTo(mutableSetOf()) { utf8[it]!! }
    }

    private class RecordingInstrumentation(
        val specMap: ClassTransformationSpecMap = ClassTransformationSpecMap(),
    ) : RetransformInlineInstrumentation(NoopLogger, specMap) {
        var failureWhileRetransformingBack: Throwable? = null
        val retransformedBack = mutableListOf<Class<*>>()
        val specsSeenWhileRetransformingBack = mutableMapOf<Class<*>, ClassTransformationSpec?>()

        private var reverting = false

        override fun execute(request: TransformationRequest): () -> Unit {
            val cancellation = super.execute(request)
            reverting = true
            return cancellation
        }

        override fun retransform(classesToTransform: Collection<Class<*>>) {
            if (!reverting) {
                return
            }
            for (cls in classesToTransform) {
                retransformedBack.add(cls)
                // The same lookup both real transformers do while the class is being retransformed.
                specsSeenWhileRetransformingBack[cls] = specMap[cls]
            }
            failureWhileRetransformingBack?.let { throw it }
        }
    }

    private object NoopLogger : MockKAgentLogger {
        override fun debug(msg: String) = Unit

        override fun trace(msg: String) = Unit

        override fun trace(
            ex: Throwable,
            msg: String,
        ): Unit = Unit

        override fun warn(msg: String) = Unit

        override fun warn(
            ex: Throwable,
            msg: String,
        ): Unit = Unit
    }
}
