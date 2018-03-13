package io.mockk.impl.instantiation

import io.mockk.JvmVarArgsCls
import io.mockk.impl.instantiation.JvmMockFactoryHelper.varArgPosition
import kotlin.test.Test
import kotlin.test.assertEquals

class JvmMockFactoryHelperTest {

    @Test
    fun kotlinVarArgPosition() {
        val clazz = SomeClass::class.java

        assertEquals(
                -1,
                clazz.getMethod("noArg").varArgPosition()
        )
        assertEquals(
                -1,
                clazz.getMethod("noVarArg", Unit.javaClass).varArgPosition()
                )
        assertEquals(
                0,
                clazz.getMethod("varArgFirst", Array<Unit>::class.java, Unit.javaClass).varArgPosition()
        )
        assertEquals(
                1,
                clazz.getMethod("varArgLast", Unit.javaClass, Array<Unit>::class.java).varArgPosition()
        )
    }

    @Test
    fun javaVarArgPosition() {
        val clazz = JvmVarArgsCls::class.java

        assertEquals(
                1,
                        clazz.getMethod("varArgsOp", Int::class.javaPrimitiveType, IntArray::class.java).varArgPosition()
        )
    }
}

internal class SomeClass {
    fun noArg() {}
    fun noVarArg(arg0: Unit) {}
    fun varArgFirst(vararg arg0: Unit, arg1: Unit) {}
    fun varArgLast(arg0: Unit, vararg arg1: Unit) {}
}