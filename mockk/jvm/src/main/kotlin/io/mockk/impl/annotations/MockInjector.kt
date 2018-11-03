package io.mockk.impl.annotations

import io.mockk.MockKException
import kotlin.reflect.*
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.valueParameters
import io.mockk.impl.annotations.InjectionHelpers.getAnyIfLateNull
import io.mockk.impl.annotations.InjectionHelpers.setAny
import io.mockk.impl.annotations.InjectionHelpers.setImmutableAny

class MockInjector(
    val mockHolder: Any,
    val lookupType: InjectionLookupType,
    val injectImmutable: Boolean,
    val overrideValues: Boolean
) {
    fun constructorInjection(type: KClass<*>): Any {
        val firstMatching = findMatchingConstructor(type)
                ?: throw MockKException("No matching constructors found:\n" + type.constructors.joinToString("\n") { it.constructorToStr() })

        return injectViaConstructor(firstMatching)
    }

    fun propertiesInjection(instance: Any) {
        instance::class.memberProperties.forEach { property ->
            val isMutable = property is KMutableProperty1

            if (!injectImmutable && !isMutable) return@forEach
            if (!overrideValues) {
                if (property.getAnyIfLateNull(instance) != null) return@forEach
            }

            val newValue = lookupValueByName(property.name, property.returnType.classifier)
                    ?: lookupValueByType(property.returnType.classifier)
                    ?: return@forEach

            if (injectImmutable && !isMutable) {
                property.setImmutableAny(instance, newValue)
            } else {
                (property as KMutableProperty1<*, *>).setAny(instance, newValue)
            }
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
        if (!lookupType.byName) return null

        return mockHolder::class.memberProperties
            .firstOrNull { it.name == name && isMatchingType(it, type) }
            ?.getAnyIfLateNull(mockHolder)
    }

    private fun lookupValueByType(type: KClassifier?): Any? {
        if (type == null) return null
        if (type !is KClass<*>) return null
        if (!lookupType.byType) return null

        return mockHolder::class.memberProperties
            .firstOrNull {
                isMatchingType(it, type)
            }
            ?.getAnyIfLateNull(mockHolder)
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

    private fun <R> KFunction<R>.constructorToStr(): String {
        return "constructor(" + parameters.map {
            (it.name ?: "<noname arg.>") + " : " + it.type + " = " + lookupToStr(it)
        }.joinToString(", ") + ")"
    }

    private fun lookupToStr(param: KParameter): String {
        return (lookupValueByName(param.name, param.type.classifier)
                ?: lookupValueByType(param.type.classifier)
                ?: "<not able to lookup>")
            .toString()
    }
}

