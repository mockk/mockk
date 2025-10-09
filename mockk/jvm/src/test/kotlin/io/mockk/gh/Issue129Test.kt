package io.mockk.gh

import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.jupiter.api.Test

@Suppress("UNUSED_VARIABLE")
class Issue129Test {
    @Test
    fun testConstructorGetParametersMocked() {
        var parameters = DataClass::class.java.constructors[0].parameters

        assert(parameters.map { it.name }.contains("name"))
        assert(parameters.map { it.name }.contains("age"))

        // inject a mockk object to alter native constructor.getParameters
        val instance: DataClass = mockk()

        clearAllMocks()
        unmockkAll()

        parameters = DataClass::class.java.constructors[0].parameters

        assert(parameters.map { it.name }.contains("name"))
        assert(parameters.map { it.name }.contains("age"))
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
