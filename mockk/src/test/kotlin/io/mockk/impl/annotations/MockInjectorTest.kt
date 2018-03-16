package io.mockk.impl.annotations

import io.mockk.MockKException
import io.mockk.mockk
import kotlin.reflect.KClass
import kotlin.reflect.full.cast
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MockInjectorTest {
    interface MockIf
    class MockCls : MockIf


    @Test
    fun primaryConstructorInjectionByType() {
        class InjectTarget(val param: MockCls)

        class InjectDeclaration {
            val mockCls = mockk<MockCls>()
        }

        val declaration = InjectDeclaration()

        val instance = constructorInjection(
            InjectTarget::class,
            InjectType.BY_TYPE,
            declaration)

        assertSame(declaration.mockCls, instance.param)
    }

    @Test
    fun primaryConstructorInjectionByName() {
        class InjectTarget(val mockCls: MockCls)

        class InjectDeclaration {
            val mockCls = mockk<MockCls>()
        }

        val declaration = InjectDeclaration()

        val instance = constructorInjection(
            InjectTarget::class,
            InjectType.BY_NAME,
            declaration)

        assertSame(declaration.mockCls, instance.mockCls)
    }

    @Test
    fun primaryConstructorInjectionBoth() {
        class InjectTarget(val mockCls: MockCls)

        class InjectDeclaration {
            val mockCls = mockk<MockCls>()
        }

        val declaration = InjectDeclaration()

        val instance = constructorInjection(
            InjectTarget::class,
            InjectType.BY_NAME,
            declaration)

        assertSame(declaration.mockCls, instance.mockCls)
    }

    @Test
    fun primaryConstructorInjectionByNameNonMatchingType() {
        class InjectTarget(val mockCls: Int)

        class InjectDeclaration {
            val mockCls = mockk<MockCls>()
        }

        val declaration = InjectDeclaration()

        assertFailsWith<MockKException> {
            constructorInjection(
                InjectTarget::class,
                InjectType.BY_NAME,
                declaration
            )
        }
    }

    @Test
    fun primaryConstructorInjectionByTypeSubclass() {
        class InjectTarget(val param: MockIf)

        class InjectDeclaration {
            val mockCls = mockk<MockCls>()
        }

        val declaration = InjectDeclaration()

        val instance = constructorInjection(
            InjectTarget::class,
            InjectType.BY_TYPE,
            declaration)

        assertSame(declaration.mockCls, instance.param)
    }

    private fun <T : Any>constructorInjection(
        typeToCreate: KClass<T>,
        injectType: InjectType,
        declaration: Any
    ): T {
        val injector = MockInjector(declaration, injectType)
        val instance = injector.constructorInjection(typeToCreate)
        assertTrue(typeToCreate.isInstance(instance))
        return typeToCreate.cast(instance)
    }
}