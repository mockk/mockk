package io.mockk.it

import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MockTypesTest {
    enum class MockType {
        REGULAR,
        SPY,
        OBJECT,
        STATIC,
        CONSTRUCTOR
    }

    class TestCls

    @Test
    fun regularMock() {
        assertOnlyOfType(mockk(), MockType.REGULAR)
    }

    @Test
    fun spyMock() {
        assertOnlyOfType(spyk(), MockType.SPY)
    }

    @Test
    fun objectMock() {
        val test = Any()
        mockkObject(test) {
            assertOnlyOfType(test, MockType.OBJECT)
        }
    }

    @Test
    fun staticMock() {
        mockkStatic(TestCls::class) {
            assertOnlyOfType(TestCls::class, MockType.STATIC)
        }
    }

    @Test
    fun constructorMock() {
        mockkConstructor(TestCls::class) {
            assertOnlyOfType(TestCls::class, MockType.CONSTRUCTOR)
        }
    }

    fun assertOnlyOfType(mock: Any, singleType: MockType) {
        for (type in MockType.values()) {
            if (singleType == type) {
                assertTrue(isOfMockType(mock, type), "mock is not of type $singleType")
            } else {
                assertFalse(isOfMockType(mock, type), "mock should be of type $singleType, but it is as well $type")
            }
        }
    }

    fun isOfMockType(mock: Any, type: MockType) = when (type) {
        MockType.REGULAR -> isMockKMock(mock)
        MockType.SPY -> isMockKMock(mock, regular = false, spy = true)
        MockType.OBJECT -> isMockKMock(mock, regular = false, objectMock = true)
        MockType.STATIC -> isMockKMock(mock, regular = false, staticMock = true)
        MockType.CONSTRUCTOR -> isMockKMock(mock, regular = false, constructorMock = true)
    }
}
