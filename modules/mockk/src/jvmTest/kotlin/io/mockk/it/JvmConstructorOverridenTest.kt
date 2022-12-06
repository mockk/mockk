package io.mockk.it

import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.jupiter.api.Test

/**
 * The mocked ::class.java constructor is overriden in the JVM.
 * Verifies issue #129.
 */
@Suppress("UNUSED_VARIABLE")
class JvmConstructorOverridenTest {
    @Test
    fun testConstructorGetParametersMocked() {
        var parameters = DataClass::class.java.constructors[0].parameters

        assert(parameters.map { it.type }.contains(String::class.java))
        assert(parameters.map { it.type }.contains(Int::class.java))

        // inject a mockk object to alter native constructor.getParameters
        val instance: DataClass = mockk()

        clearAllMocks()
        unmockkAll()

        parameters = DataClass::class.java.constructors[0].parameters

        assert(parameters.map { it.type }.contains(String::class.java))
        assert(parameters.map { it.type }.contains(Int::class.java))
    }

    @Test
    fun testConstructorEqualityAfterClearMocks() {
        val javaClass = DataClass::class.java

        val constructor = DataClass::class.java.constructors[0]

        assert(javaClass.constructors.size == 1)

        // inject a mockk object to alter native constructor.getParameters
        val instance: DataClass = mockk()

        clearAllMocks()
        unmockkAll()

        assert(javaClass === DataClass::class.java)
        assert(constructor.parameterCount == DataClass::class.java.constructors[0].parameterCount)
        assert(constructor == DataClass::class.java.constructors[0])
    }

}

data class DataClass(val name: String, val age: Int)
