package io.mockk.restrict

import io.mockk.impl.annotations.MockkRestricted
import io.mockk.impl.annotations.MockkRestrictedMode
import io.mockk.impl.restrict.MockingRestrictedExtension
import io.mockk.mockk
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import kotlin.test.Test

@ExtendWith(MockingRestrictedExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class RestrictedMockAtomicTest {
    @Test
    @Order(1)
    @MockkRestricted(mode = MockkRestrictedMode.EXCEPTION)
    fun test1() {
        assertThrows<IllegalArgumentException> {
            mockk<File>()
        }
    }

    @Test
    @Order(2)
    @MockkRestricted
    fun test2() {
        mockk<File>()
    }

    @Test
    @Order(3)
    @MockkRestricted(mode = MockkRestrictedMode.EXCEPTION, restricted = [Foo::class])
    fun test3() {
        assertThrows<IllegalArgumentException>{
            mockk<Foo>()
        }
    }

    @Test
    @Order(4)
    fun test4() {
        assertDoesNotThrow {
            mockk<Foo>()
        }
    }

    @Test
    @Order(5)
    @MockkRestricted(mode = MockkRestrictedMode.EXCEPTION)
    fun test5() {
        assertThrows<IllegalArgumentException> {
            mockk<File>()
        }
    }

    @Test
    @Order(6)
    fun test6() {
        assertDoesNotThrow {
            mockk<File>()
        }
    }

    class Foo
}
