package io.mockk.it

import io.mockk.MockKGateway
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.proxy.common.transformation.ClassTransformationSpec
import io.mockk.proxy.common.transformation.ClassTransformationSpecMap
import io.mockk.proxy.common.transformation.RetransformInlineInstrumentation
import io.mockk.unmockkAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.Serializable
import java.util.AbstractMap
import java.util.HashMap

/**
 * Reproducer for [issue #1289](https://github.com/mockk/mockk/issues/1289):
 * `unmockkAll` cannot clear all mocked classes.
 *
 * Recording `every { MockObj.getValues() }` for a method that returns a collection makes
 * MockK build temporary recording mocks and a permanent child mock for the return type
 * (`java.util.HashMap`). Each of them retransforms the whole `HashMap` hierarchy
 * (`HashMap`, `Map`, `Cloneable`, `Serializable`, `AbstractMap`, `Object`), but neither
 * the temporary mocks nor the orphaned child mock ever had their transformation request
 * cancelled. The transformation spec registry therefore kept records for JDK classes
 * after `unmockkAll`, which leaks memory and makes subsequent tests slow.
 *
 * The assertion is a before/after delta on the registry: other tests running in the same
 * JVM may legitimately hold live mocks of these JDK classes, so the test requires that
 * this scenario adds nothing and leaves nothing behind, not that the registry is
 * globally empty.
 */
class Issue1289UnmockkAllSpecMapLeakTest {
    object MockObj {
        fun getValues() = hashMapOf(1 to 1, 2 to 2)
    }

    private val issueClasses: List<Class<*>> =
        listOf(
            HashMap::class.java,
            Map::class.java,
            Cloneable::class.java,
            Serializable::class.java,
            AbstractMap::class.java,
            MockObj::class.java,
        )

    @Test
    fun `unmockkAll leaves no transformation specs behind for the recorded classes`() {
        val before = transformationSpecs()

        mockkObject(MockObj)
        every { MockObj.getValues() } returns hashMapOf(1 to 1, 2 to 2, 3 to 3)
        assertEquals(hashMapOf(1 to 1, 2 to 2, 3 to 3), MockObj.getValues())
        unmockkAll()

        val after = transformationSpecs()
        for (cls in issueClasses) {
            assertEquals(
                before[cls],
                after[cls],
                "$cls still holds a transformation spec after unmockkAll; " +
                    "the agent transformation request was never cancelled",
            )
        }
    }

    @Test
    fun `chained stubbing through a collection return type still records`() {
        mockkObject(MockObj)
        every { MockObj.getValues()[1] } returns 42
        assertEquals(42, MockObj.getValues()[1])
        unmockkAll()
    }

    /**
     * Reads the agent's [ClassTransformationSpecMap] via reflection, walking
     * gateway -> agent factory -> proxy maker -> inline instrumentation -> spec map.
     */
    private fun transformationSpecs(): Map<Class<*>, ClassTransformationSpec> {
        val gateway = MockKGateway.implementation()

        val agentFactoryField = gateway.javaClass.getDeclaredField("agentFactory")
        agentFactoryField.isAccessible = true
        val agentFactory = agentFactoryField.get(gateway)

        val proxyMakerField = agentFactory.javaClass.getDeclaredField("jvmProxyMaker")
        proxyMakerField.isAccessible = true
        val proxyMaker = proxyMakerField.get(agentFactory)

        val inlinerField = proxyMaker.javaClass.getDeclaredField("inliner")
        inlinerField.isAccessible = true
        val inliner = inlinerField.get(proxyMaker)

        val specMapField = RetransformInlineInstrumentation::class.java.getDeclaredField("specMap")
        specMapField.isAccessible = true
        val specMap = specMapField.get(inliner) as ClassTransformationSpecMap

        val classSpecsField = ClassTransformationSpecMap::class.java.getDeclaredField("classSpecs")
        classSpecsField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val classSpecs = classSpecsField.get(specMap) as Map<Class<*>, ClassTransformationSpec>

        return classSpecs.toMap()
    }
}
