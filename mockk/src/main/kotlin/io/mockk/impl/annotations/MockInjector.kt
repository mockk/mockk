package io.mockk.impl.annotations

import io.mockk.MockKException
import kotlin.reflect.*
import kotlin.reflect.KMutableProperty1.Setter
import kotlin.reflect.KProperty1.Getter
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.isAccessible

class MockInjector(
    val mockHolder: Any,
    val injectType: InjectType
) {
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

    fun propertiesInjection(instance: Any) {
        instance::class.declaredMemberProperties.forEach { property ->
            if (
                property !is KMutableProperty1 ||
                property.getAnyIfLateNull(instance) != null
            ) return@forEach

            val newValue = lookupValueByName(property.name, property.returnType.classifier)
                    ?: lookupValueByType(property.returnType.classifier)
                    ?: return@forEach

            property.setAny(instance, newValue)
        }
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
            lookupValueByName(param.name, param.type.classifier)
                    ?: lookupValueByType(param.type.classifier)
                    ?: throw MockKException("Parameter unmatched: $param")
        }.toTypedArray()
    }

    private fun tryMatchingParameters(parameters: List<KParameter>): Boolean {
        return !parameters
            .map { param ->
                lookupValueByName(param.name, param.type.classifier)
                        ?: lookupValueByType(param.type.classifier)
            }
            .any { it == null }
    }

    private fun lookupValueByName(name: String?, type: KClassifier?): Any? {
        if (name == null) return null
        if (type == null) return null
        if (type !is KClass<*>) return null
        if (!injectType.byName) return null

        return mockHolder::class.declaredMemberProperties
            .firstOrNull { it.name == name && isMatchingType(it, type) }
            ?.getAny(mockHolder)
    }

    private fun lookupValueByType(type: KClassifier?): Any? {
        if (type == null) return null
        if (type !is KClass<*>) return null
        if (!injectType.byType) return null

        return mockHolder::class.declaredMemberProperties
            .firstOrNull {
                isMatchingType(it, type)
            }
            ?.getAny(mockHolder)
    }

    private fun isMatchingType(
        it: KProperty1<out Any, Any?>,
        type: KClass<*>
    ): Boolean {
        val propertyType = it.returnType.classifier
        return if (propertyType is KClass<*>) {
            propertyType.isSubclassOf(type)
        } else {
            false
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun KProperty1<*, *>.getAny(obj: Any): Any? {
        isAccessible = true
        return (this.getter as Getter<Any, Any?>)(obj)
    }

    @Suppress("UNCHECKED_CAST")
    private fun KProperty1<*, *>.getAnyIfLateNull(obj: Any): Any? {
        isAccessible = true
        return (this.getter as Getter<Any, Any?>)(obj)
    }

    @Suppress("UNCHECKED_CAST")
    private fun KMutableProperty1<*, *>.setAny(obj: Any, value: Any?) {
        isAccessible = true
        return (this.setter as Setter<Any, Any?>).invoke(obj, value)
    }


    private fun <R> KFunction<R>.constructorToStr(): String {
        return "constructor(" + parameters.map {
            (it.name ?: "<noname arg.>") + " : " + it.type
        }.joinToString(", ") + ")"
    }
}