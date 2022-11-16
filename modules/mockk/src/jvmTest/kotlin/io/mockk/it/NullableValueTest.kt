package io.mockk.it

import io.mockk.impl.JvmMockKGateway
import io.mockk.impl.instantiation.JvmAnyValueGenerator
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

class NullableValueTest {
    class NullableValueGenerator(
        voidInstance: Any
    ) : JvmAnyValueGenerator(voidInstance) {
        override fun anyValue(cls: KClass<*>, isNullable: Boolean, orInstantiateVia: () -> Any?): Any? {
            if (isNullable) return null
            return super.anyValue(cls, isNullable, orInstantiateVia)
        }
    }

    @Test
    fun testRelaxedMockReturnsNull() {
        JvmMockKGateway.anyValueGeneratorFactory = { voidInstance ->
            NullableValueGenerator(voidInstance)
        }

        class Bar

        @Suppress("RedundantNullableReturnType", "RedundantSuspendModifier")
        class Foo {
            val property: Bar? = Bar()
            val isEnabled: Boolean? = false
            fun getSomething(): Bar? = Bar()
            suspend fun getOtherThing(): Bar? = Bar()
        }

        val mock = mockk<Foo>(relaxed = true)
        assertEquals(null, mock.property)
        assertEquals(null, mock.isEnabled)
        assertEquals(null, mock.getSomething())
        assertEquals(null, runBlocking { mock.getOtherThing() })

        JvmMockKGateway.anyValueGeneratorFactory = { voidInstance ->
            JvmAnyValueGenerator(voidInstance)
        }
    }


    /**
     * Test related to GitHub issue #323
     */
    @Ignore("Temporarily ignored because it's failing only on travis and not anywhere else")
    @Test
    fun withNullableArgMatchesAndExecutesCaptureBlockWhenArgumentIsNull() {

        class MockedClass {
            fun printNullableString(s: String?) {
                println(s)
            }
        }

        class TestedClass(private val mockedClass: MockedClass) {
            fun testNullString() {
                val testString: String? = null
                mockedClass.printNullableString(testString)
            }
        }

        val mock = mockk<MockedClass>(relaxed = true)
        val testedClass = TestedClass(mock)

        testedClass.testNullString()

        verify(exactly = 1) {
            mock.printNullableString(withNullableArg { println(it) })
        }
    }
}
