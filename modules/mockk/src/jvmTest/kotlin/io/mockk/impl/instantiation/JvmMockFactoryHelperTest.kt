package io.mockk.impl.instantiation

import io.mockk.JvmVarArgsCls
import io.mockk.impl.instantiation.JvmMockFactoryHelper.toDescription
import kotlin.test.Test
import kotlin.test.assertEquals

class JvmMockFactoryHelperTest {

    @Test
    fun kotlinVarArgPosition() {
        val clazz = SomeClass::class.java

        assertEquals(
                -1,
            clazz.getMethod("noArg").toDescription().varArgsArg
        )
        assertEquals(
                -1,
            clazz.getMethod("noVarArg", Unit.javaClass).toDescription().varArgsArg
                )
        assertEquals(
                0,
            clazz.getMethod("varArgFirst", Array<Unit>::class.java, Unit.javaClass).toDescription().varArgsArg
        )
        assertEquals(
                1,
            clazz.getMethod("varArgLast", Unit.javaClass, Array<Unit>::class.java).toDescription().varArgsArg
        )
    }

    @Test
    fun javaVarArgPosition() {
        val clazz = JvmVarArgsCls::class.java

        assertEquals(
                1,
            clazz.getMethod("varArgsOp", Int::class.javaPrimitiveType, IntArray::class.java).toDescription().varArgsArg
        )
    }
}

@Suppress("UNUSED_PARAMETER", "unused")
internal class SomeClass {
    fun noArg() {}
    fun noVarArg(arg0: Unit) {}
    fun varArgFirst(vararg arg0: Unit, arg1: Unit) {}
    fun varArgLast(arg0: Unit, vararg arg1: Unit) {}
}
