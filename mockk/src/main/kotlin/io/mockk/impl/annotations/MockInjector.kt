package io.mockk.impl.annotations

import io.mockk.MockKException
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.valueParameters

class MockInjector(
    val mockHolder: Any,
    val injectType: InjectType
) {
    fun propertiesInjection(instance: Any) {
    }

    fun constructorInjection(type: KClass<*>): Any {
        val primary = type.primaryConstructor
        if (primary != null) {
            if (!tryMatchingParameters(primary.valueParameters)) {
                throw MockKException("Not able to match parameters for primary constructor: ${primary.constructorToStr()}")
            }

            return injectViaConstructor(primary)
        }

        val firstMatching = findMatchingConstructor(type)
                ?: throw MockKException("No matching constructors found:\n" + type.constructors.joinToString("\n") { it.constructorToStr() })

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

    private fun matchParameters(parameters: List<KParameter>): Array<Any> {
        return parameters.map { param ->
            return@map lookupValueByName(param)
                    ?: lookupValueByType(param)
                    ?: throw MockKException("Parameter unmatched: $param")
        }.toTypedArray()
    }

    private fun lookupValueByName(param: KParameter): Any? {
        if (!injectType.byName) return null

        return mockHolder::class.declaredMemberProperties
            .firstOrNull { it.name == param.name }
            ?.call(mockHolder)
    }

    private fun lookupValueByType(param: KParameter): Any? {
        if (!injectType.byType) return null

        return mockHolder::class.declaredMemberProperties
            .firstOrNull { (param.type.classifier as KClass<*>).isSubclassOf(it.returnType.classifier as KClass<*>) }
            ?.call(mockHolder)
    }


    private fun tryMatchingParameters(parameters: List<KParameter>): Boolean {
        return !parameters
            .map { param -> lookupValueByName(param) ?: lookupValueByType(param) }
            .any { it == null }
    }

    private fun <R> KFunction<R>.constructorToStr(): String {
        return "constructor(" + parameters.map { it.name + " : " + it.type }.joinToString(", ") + ")"
    }
}