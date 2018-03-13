package io.mockk.impl.annotations

import io.mockk.MockKException
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.valueParameters

class MockInjector(val mockHolder: Any) {
    fun propertiesInjection(instance: Any) {
    }

    fun constructorInjection(type: KClass<*>): Any {
        val primary = type.primaryConstructor
        if (primary != null) {
            return injectViaConstructor(primary)
        }

        val firstMatching = findMatchingConstructor(type)
                ?: throw MockKException("No matching constructors found:\n" + type.constructors.joinToString("\n"))

        return injectViaConstructor(firstMatching)
    }

    private fun injectViaConstructor(firstMatching: KFunction<Any>): Any {
        return firstMatching.call(*matchParameters(firstMatching.valueParameters))
    }

    private fun findMatchingConstructor(type: KClass<*>): KFunction<Any>? {
        val sortCriteria = compareBy<KFunction<Any>>({
            -it.parameters.size
        }, {
            it.parameters.map { it.type.toString() }.joinToString(",")
        })

        return type.constructors.sortedWith(sortCriteria)
            .firstOrNull { tryMatchingParameters(it.valueParameters) }
    }

    private fun matchParameters(valueParameters: List<KParameter>): Array<Any?> {
        return arrayOf()
    }

    private fun tryMatchingParameters(parameters: List<KParameter>): Boolean {
        return false
    }
}