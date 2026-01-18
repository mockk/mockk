package io.mockk.it

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * See issue #47
 */
class InjectMocksTest {
    interface IFoo

    class Foo : IFoo {
        fun method() {
        }
    }

    abstract class AbstractBar<T : IFoo> {
        lateinit var foo: T
    }

    class Bar : AbstractBar<Foo>() {
        fun call() {
            foo.method()
        }
    }

    @MockK
    lateinit var foo: Foo

    @InjectMockKs
    lateinit var bar: Bar

    @BeforeTest
    fun setUp() = MockKAnnotations.init(this)

    @Test
    fun test() {
        every { foo.method() } answers { nothing }

        bar.call()

        verify { foo.method() }
    }
}

/**
 * See issue #1356
 * Test for injecting mocks as a List
 */
class InjectMocksListTest {
    interface IFoo {
        fun method()
    }

    class Foo1 : IFoo {
        override fun method() {}
    }

    class Foo2 : IFoo {
        override fun method() {}
    }

    class Bar(
        val foos: List<IFoo>,
    )

    @MockK
    lateinit var foo1: Foo1

    @MockK
    lateinit var foo2: Foo2

    @InjectMockKs
    lateinit var bar: Bar

    @BeforeTest
    fun setUp() = MockKAnnotations.init(this)

    @Test
    fun testListInjection() {
        kotlin.test.assertEquals(2, bar.foos.size)
        kotlin.test.assertTrue(bar.foos.contains(foo1))
        kotlin.test.assertTrue(bar.foos.contains(foo2))
    }
}

/**
 * See issue #1496
 * Test for @InjectMockKs dependency order resolution using topological sort.
 *
 * Problem: MockK previously processed @InjectMockKs fields in reflection order,
 * which failed when dependency order differed from field declaration order.
 *
 * Solution: Use dependency order (via MockKAnnotations.init(useDependencyOrder = true))
 * to resolve dependencies in correct order.
 */
class InjectMocksDependencyOrderTest {
    // ========== Domain Classes ==========

    interface Repository {
        fun getData(): String
    }

    // Two-level: A depends on B
    class ServiceB(
        val repository: Repository,
    )

    class ServiceA(
        val b: ServiceB,
    )

    // Three-level chain: A -> B -> C (reverse alphabetical)
    class LevelC(
        val repository: Repository,
    )

    class LevelB(
        val c: LevelC,
    )

    class LevelA(
        val b: LevelB,
    )

    // Diamond dependency: D -> (B, C), B -> A, C -> A
    class DiamondA(
        val repository: Repository,
    )

    class DiamondB(
        val a: DiamondA,
    )

    class DiamondC(
        val a: DiamondA,
    )

    class DiamondD(
        val b: DiamondB,
        val c: DiamondC,
    )

    // Circular dependency (intentionally no "Circular" in class name for test accuracy)
    class NodeX(
        val y: NodeY,
    )

    class NodeY(
        val x: NodeX,
    )

    // ========== Test 1: Two-level dependency ==========

    class TwoLevelTestTarget {
        @MockK
        lateinit var repository: Repository

        @InjectMockKs
        lateinit var a: ServiceA // Depends on B

        @InjectMockKs
        lateinit var b: ServiceB
    }

    @Test
    fun twoLevelDependencyWithoutTopologicalSort() {
        val obj = TwoLevelTestTarget()

        val exception =
            kotlin.test.assertFailsWith<io.mockk.MockKException> {
                MockKAnnotations.init(obj)
            }

        kotlin.test.assertTrue(
            exception.message?.contains("No matching constructors found") == true,
            "Error should mention 'No matching constructors found', but was: ${exception.message}",
        )
    }

    @Test
    fun twoLevelDependency() {
        val obj = TwoLevelTestTarget()
        MockKAnnotations.init(obj, useDependencyOrder = true)

        kotlin.test.assertNotNull(obj.b, "ServiceB should be created")
        kotlin.test.assertNotNull(obj.a, "ServiceA should be created")
        kotlin.test.assertSame(obj.b, obj.a.b, "ServiceA.b should be the injected ServiceB")
    }

    // ========== Test 2: Three-level chain ==========

    class ThreeLevelTestTarget {
        @MockK
        lateinit var repository: Repository

        @InjectMockKs
        lateinit var a: LevelA // Depends on B

        @InjectMockKs
        lateinit var b: LevelB // Depends on C

        @InjectMockKs
        lateinit var c: LevelC // Independent (only needs @MockK)
    }

    @Test
    fun threeLevelChainDependencyWithoutTopologicalSort() {
        val obj = ThreeLevelTestTarget()

        val exception =
            kotlin.test.assertFailsWith<io.mockk.MockKException> {
                MockKAnnotations.init(obj)
            }

        kotlin.test.assertTrue(
            exception.message?.contains("No matching constructors found") == true,
            "Error should mention 'No matching constructors found', but was: ${exception.message}",
        )
    }

    @Test
    fun threeLevelChainDependency() {
        val obj = ThreeLevelTestTarget()
        MockKAnnotations.init(obj, useDependencyOrder = true)

        kotlin.test.assertNotNull(obj.c, "LevelC should be created")
        kotlin.test.assertNotNull(obj.b, "LevelB should be created")
        kotlin.test.assertNotNull(obj.a, "LevelA should be created")
        kotlin.test.assertSame(obj.c, obj.b.c, "LevelB.c should be the injected LevelC")
        kotlin.test.assertSame(obj.b, obj.a.b, "LevelA.b should be the injected LevelB")
    }

    // ========== Test 3: Diamond dependency ==========

    class DiamondTestTarget {
        @MockK
        lateinit var repository: Repository

        @InjectMockKs
        lateinit var alpha: DiamondD // 'alpha' is first, but needs beta & gamma

        @InjectMockKs
        lateinit var beta: DiamondB // Needs zeta

        @InjectMockKs
        lateinit var gamma: DiamondC // Needs zeta

        @InjectMockKs
        lateinit var zeta: DiamondA // 'zeta' is last, but is independent (leaf)
    }

    @Test
    fun diamondDependencyWithoutTopologicalSort() {
        val obj = DiamondTestTarget()

        val exception =
            kotlin.test.assertFailsWith<io.mockk.MockKException> {
                MockKAnnotations.init(obj)
            }

        kotlin.test.assertTrue(
            exception.message?.contains("No matching constructors found") == true,
            "Error should mention 'No matching constructors found', but was: ${exception.message}",
        )
    }

    @Test
    fun diamondDependency() {
        val obj = DiamondTestTarget()
        MockKAnnotations.init(obj, useDependencyOrder = true)

        kotlin.test.assertNotNull(obj.zeta, "DiamondA (zeta) should be created")
        kotlin.test.assertNotNull(obj.beta, "DiamondB (beta) should be created")
        kotlin.test.assertNotNull(obj.gamma, "DiamondC (gamma) should be created")
        kotlin.test.assertNotNull(obj.alpha, "DiamondD (alpha) should be created")

        kotlin.test.assertSame(obj.zeta, obj.beta.a, "DiamondB.a should be zeta")
        kotlin.test.assertSame(obj.zeta, obj.gamma.a, "DiamondC.a should be zeta")
        kotlin.test.assertSame(obj.beta, obj.alpha.b, "DiamondD.b should be beta")
        kotlin.test.assertSame(obj.gamma, obj.alpha.c, "DiamondD.c should be gamma")
    }

    // ========== Test 4: Circular dependency detection ==========

    class CircularTestTarget {
        @InjectMockKs
        lateinit var x: NodeX

        @InjectMockKs
        lateinit var y: NodeY
    }

    @Test
    fun circularDependencyWithoutTopologicalSort() {
        val obj = CircularTestTarget()

        val exception =
            kotlin.test.assertFailsWith<io.mockk.MockKException> {
                MockKAnnotations.init(obj)
            }

        kotlin.test.assertTrue(
            exception.message?.contains("No matching constructors found") == true,
            "Error should mention 'No matching constructors found', but was: ${exception.message}",
        )
    }

    @Test
    fun circularDependencyDetection() {
        val obj = CircularTestTarget()

        val exception =
            kotlin.test.assertFailsWith<io.mockk.MockKException> {
                MockKAnnotations.init(obj, useDependencyOrder = true)
            }

        kotlin.test.assertTrue(
            exception.message?.contains("Circular") == true,
            "Error should mention 'Circular', but was: ${exception.message}",
        )
    }
}
