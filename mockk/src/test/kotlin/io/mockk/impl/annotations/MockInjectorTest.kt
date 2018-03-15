package io.mockk.impl.annotations

import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MockInjectorTest {
    class MockCls {

    }

    class InjectTarget(val param: MockCls)


    class InjectDeclaration {
        val mockCls = mockk<MockCls>()

        @InjectMockKs
        lateinit var target: InjectTarget
    }

    @Test
    fun primaryConstructorInjection() {
        val declaration = InjectDeclaration()

        val injector = MockInjector(declaration, InjectType.BY_TYPE)

        val instance = injector.constructorInjection(InjectTarget::class)

        assertTrue(instance is InjectTarget)
        assertSame(declaration.mockCls, (instance as InjectTarget).param)
    }
}