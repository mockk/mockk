package io.mockk.impl.annotations

import io.mockk.MockKException
import io.mockk.mockk
import kotlin.reflect.KClass
import kotlin.reflect.full.cast
import kotlin.test.*

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

        val instance = inject(
            InjectTarget::class,
            InjectionLookupType.BY_TYPE,
            declaration
        )

        assertSame(declaration.mockCls, instance.param)
    }

    @Test
    fun primaryConstructorInjectionByName() {
        class InjectTarget(val mockCls: MockCls)

        class InjectDeclaration {
            val mockCls = mockk<MockCls>()
        }

        val declaration = InjectDeclaration()

        val instance = inject(
            InjectTarget::class,
            InjectionLookupType.BY_NAME,
            declaration
        )

        assertSame(declaration.mockCls, instance.mockCls)
    }

    @Test
    fun primaryConstructorInjectionBoth() {
        class InjectTarget(val mockCls: MockCls)

        class InjectDeclaration {
            val mockCls = mockk<MockCls>()
        }

        val declaration = InjectDeclaration()

        val instance = inject(
            InjectTarget::class,
            InjectionLookupType.BY_NAME,
            declaration
        )

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
            inject(
                InjectTarget::class,
                InjectionLookupType.BY_NAME,
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

        val instance = inject(
            InjectTarget::class,
            InjectionLookupType.BY_TYPE,
            declaration
        )

        assertSame(declaration.mockCls, instance.param)
    }

    @Test
    fun secondaryConstructorInjection() {
        class InjectTarget constructor(val param: MockIf)

        class InjectDeclaration {
            val mockCls = mockk<MockCls>()
        }

        val declaration = InjectDeclaration()

        val instance = inject(
            InjectTarget::class,
            InjectionLookupType.BY_TYPE,
            declaration
        )

        assertSame(declaration.mockCls, instance.param)
    }

    @Test
    fun biggestSecondaryConstructorInjection() {
        class InjectTarget {
            var results = mutableListOf<MockCls>()

            constructor(param: MockCls) {
                results.add(param)
            }

            constructor(param1: MockCls, param2: MockCls) {
                results.add(param1)
                results.add(param2)
            }
        }

        class InjectDeclaration {
            val mockCls = mockk<MockCls>()
        }

        val declaration = InjectDeclaration()

        val instance = inject(
            InjectTarget::class,
            InjectionLookupType.BY_TYPE,
            declaration
        )

        assertEquals(2, instance.results.size)
        assertSame(declaration.mockCls, instance.results[0])
        assertSame(declaration.mockCls, instance.results[1])
    }

    @Test
    fun biggestMatchingSecondaryConstructorInjection() {
        class InjectTarget {
            val results = mutableListOf<MockCls>()
            var ctor = 0

            constructor(param: MockCls) {
                results.add(param)
                ctor = 1
            }

            constructor(param: MockCls, param2: Int) {
                results.add(param)
                ctor = 2
            }
        }

        class InjectDeclaration {
            val mockCls = mockk<MockCls>()
        }

        val declaration = InjectDeclaration()

        val instance = inject(
            InjectTarget::class,
            InjectionLookupType.BY_TYPE,
            declaration
        )

        assertEquals(1, instance.results.size)
        assertEquals(1, instance.ctor)
        assertSame(declaration.mockCls, instance.results[0])
    }

    @Test
    fun matchingSecondaryNonMatchingPrimaryConstructorInjection() {
        class InjectTarget(val x: Int) {
            val results = mutableListOf<MockCls>()
            var ctor = x

            constructor(param: MockCls) : this(3) {
                results.add(param)
                ctor = 1
            }

            constructor(param: MockCls, param2: Int) : this(4) {
                results.add(param)
                ctor = 2
            }
        }

        class InjectDeclaration {
            val mockCls = mockk<MockCls>()
        }

        val declaration = InjectDeclaration()

        val instance = inject(
            InjectTarget::class,
            InjectionLookupType.BY_TYPE,
            declaration
        )

        assertEquals(1, instance.results.size)
        assertEquals(1, instance.ctor)
        assertSame(declaration.mockCls, instance.results[0])
    }

    @Test
    fun matchingSecondaryConstructorInjection() {
        class InjectTarget(val type: MockCls) {
            val results = mutableListOf<MockCls>()
            var ctor = 1

            constructor(param1: MockCls, param2: MockCls) : this(param1) {
                results.add(param1)
                results.add(param2)
                ctor = 2
            }
        }

        class InjectDeclaration {
            val mockCls = mockk<MockCls>()
        }

        val declaration = InjectDeclaration()

        val instance = inject(
            InjectTarget::class,
            InjectionLookupType.BY_TYPE,
            declaration
        )

        assertEquals(2, instance.results.size)
        assertEquals(2, instance.ctor)
        assertSame(declaration.mockCls, instance.results[0])
        assertSame(declaration.mockCls, instance.results[1])
    }

    @Test
    fun nonMatchingSecondaryConstructorInjection() {
        class InjectTarget(val type: MockCls) {
            val results = mutableListOf<MockCls>()
            var ctor = 1

            constructor(param1: MockCls, param2: Int) : this(param1) {
                results.add(param1)
                ctor = 2
            }
        }

        class InjectDeclaration {
            val mockCls = mockk<MockCls>()
        }

        val declaration = InjectDeclaration()

        val instance = inject(
            InjectTarget::class,
            InjectionLookupType.BY_TYPE,
            declaration
        )

        assertEquals(1, instance.ctor)
        assertSame(declaration.mockCls, instance.type)
    }

    @Test
    fun constructorInjectionLateInit() {
        class InjectTarget() {
            var param: MockCls? = null

            constructor(param: MockCls) : this() {
                this.param = param
            }
        }

        class InjectDeclaration {
            lateinit var mockCls: MockCls
        }

        val declaration = InjectDeclaration()

        val instance = inject(
            InjectTarget::class,
            InjectionLookupType.BY_TYPE,
            declaration
        )

        assertEquals(null, instance.param)
    }

    @Test
    fun otherNameConstructorInjection() {
        class InjectTarget(val otherName: MockCls, val name: MockCls) {
        }

        class InjectDeclaration {
            val name = MockCls()
        }

        val declaration = InjectDeclaration()

        assertFailsWith<MockKException> {
            inject(
                InjectTarget::class,
                InjectionLookupType.BY_NAME,
                declaration
            )
        }
    }

    @Test
    fun defaultConstructor() {
        class InjectTarget

        class InjectDeclaration {
            val obj = MockCls()
        }

        val declaration = InjectDeclaration()

        val instance = inject(
            InjectTarget::class,
            InjectionLookupType.BOTH,
            declaration
        )

        assertNotNull(instance)
    }

    @Test
    fun simplePropertyInjection() {
        class InjectTarget {
            lateinit var property: MockCls
        }

        class InjectDeclaration {
            val obj = MockCls()
        }

        val declaration = InjectDeclaration()

        val instance = inject(
            InjectTarget::class,
            InjectionLookupType.BY_TYPE,
            declaration,
            true
        )

        assertSame(declaration.obj, instance.property)
    }

    @Test
    fun byNamePropertyInjection() {
        class InjectTarget {
            var property1: MockCls? = null
            var property2: MockCls? = null
        }

        class InjectDeclaration {
            val property1 = MockCls()
        }

        val declaration = InjectDeclaration()

        val instance = inject(
            InjectTarget::class,
            InjectionLookupType.BY_NAME,
            declaration,
            true
        )

        assertSame(declaration.property1, instance.property1)
        assertNull(instance.property2)
    }

    @Test
    fun bySubTypePropertyInjection() {
        class InjectTarget {
            var property1: MockIf? = null
            var property2: MockIf? = null
        }

        class InjectDeclaration {
            val property = MockCls()
        }

        val declaration = InjectDeclaration()

        val instance = inject(
            InjectTarget::class,
            InjectionLookupType.BY_TYPE,
            declaration,
            true
        )

        assertSame(declaration.property, instance.property1)
        assertSame(declaration.property, instance.property2)
    }

    @Test
    fun privatePropertyInjection() {
        class InjectTarget {
            private lateinit var property: MockCls

            fun access() = property
        }

        class InjectDeclaration {
            val obj = MockCls()
        }

        val declaration = InjectDeclaration()

        val instance = inject(
            InjectTarget::class,
            InjectionLookupType.BY_TYPE,
            declaration,
            true
        )

        assertSame(declaration.obj, instance.access())
    }

    @Test
    fun noReadOnlyPropertyInjection() {
        class InjectTarget {
            val property = MockCls()
        }

        class InjectDeclaration {
            val obj = MockCls()
        }

        val declaration = InjectDeclaration()

        val instance = inject(
            InjectTarget::class,
            InjectionLookupType.BY_TYPE,
            declaration,
            true,
            false
        )

        assertNotSame(declaration.obj, instance.property)
    }

    @Test
    fun readOnlyPropertyInjection() {
        class InjectTarget {
            val property = MockCls()
        }

        class InjectDeclaration {
            val obj = MockCls()
        }

        val declaration = InjectDeclaration()

        val instance = inject(
            InjectTarget::class,
            InjectionLookupType.BY_TYPE,
            declaration,
            true,
            true,
            true
        )

        assertSame(declaration.obj, instance.property)
    }

    private fun <T : Any> inject(
        typeToCreate: KClass<T>,
        lookupType: InjectionLookupType,
        declaration: Any,
        propertyInjection: Boolean = false,
        injectImmutable: Boolean = false,
        overrideValues: Boolean = false
    ): T {
        val injector = MockInjector(declaration, lookupType, injectImmutable, overrideValues)
        val instance = injector.constructorInjection(typeToCreate)
        assertTrue(typeToCreate.isInstance(instance))
        if (propertyInjection) {
            injector.propertiesInjection(instance)
        }
        return typeToCreate.cast(instance)
    }
}