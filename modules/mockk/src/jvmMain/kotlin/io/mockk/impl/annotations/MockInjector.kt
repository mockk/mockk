package io.mockk.impl.annotations

import io.mockk.MockKException
import io.mockk.impl.annotations.InjectionHelpers.getAnyIfLateNull
import io.mockk.impl.annotations.InjectionHelpers.setAny
import io.mockk.impl.annotations.InjectionHelpers.setImmutableAny
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.valueParameters

class MockInjector(
    val mockHolder: Any,
    val lookupType: InjectionLookupType,
    val injectImmutable: Boolean,
    val overrideValues: Boolean,
) {
    private companion object {
        private val sortCriteria =
            compareBy<KFunction<Any>>(
                { -it.parameters.size },
                { fn -> fn.parameters.joinToString(",") { it.type.toString() } },
            )
    }

    fun constructorInjection(type: KClass<*>): Any {
        val firstMatching =
            findMatchingConstructor(type)
                ?: throw MockKException(
                    "No matching constructors found:\n" + type.constructors.joinToString("\n") { it.constructorToStr() },
                )

        return injectViaConstructor(firstMatching)
    }

    fun propertiesInjection(instance: Any) {
        instance::class.memberProperties.forEach { property ->
            val isMutable = property is KMutableProperty1

            if (!injectImmutable && !isMutable) return@forEach
            if (!overrideValues) {
                if (property.getAnyIfLateNull(instance) != null) return@forEach
            }

            val newValue =
                lookupValueByName(property.name, property.returnType.classifier)
                    ?: lookupValueByType(property.returnType.classifier)
                    ?: return@forEach

            if (injectImmutable && !isMutable) {
                property.setImmutableAny(instance, newValue)
            } else {
                (property as KMutableProperty1<*, *>).setAny(instance, newValue)
            }
        }
    }

    private fun injectViaConstructor(firstMatching: KFunction<Any>): Any =
        firstMatching.valueParameters
            .fold(mutableMapOf<KParameter, Any>()) { acc, cur ->
                acc.apply {
                    matchParameter(cur)?.let { this[cur] = it }
                }
            }.let { firstMatching.callBy(it) }

    private fun findMatchingConstructor(type: KClass<*>): KFunction<Any>? =
        type.constructors
            .sortedWith(sortCriteria)
            .firstOrNull { tryMatchingParameters(it.valueParameters) }

    private fun matchParameter(param: KParameter): Any? =
        lookupValueByName(param.name, param.type.classifier)
            ?: lookupListValues(param)
            ?: lookupValueByType(param.type.classifier)
            ?: if (param.isOptional) null else throw MockKException("Parameter unmatched: $param")

    private fun tryMatchingParameters(parameters: List<KParameter>): Boolean =
        parameters.all { param ->
            lookupValueByName(param.name, param.type.classifier) != null ||
                lookupListValues(param) != null ||
                lookupValueByType(param.type.classifier) != null ||
                param.isOptional
        }

    private fun lookupValueByName(
        name: String?,
        type: KClassifier?,
    ): Any? {
        if (name == null) return null
        if (type == null) return null
        if (type !is KClass<*>) return null
        if (!lookupType.byName) return null

        return mockHolder::class
            .memberProperties
            .firstOrNull { it.name == name && isMatchingType(it, type) }
            ?.getAnyIfLateNull(mockHolder)
    }

    private fun lookupValueByType(type: KClassifier?): Any? {
        if (type == null) return null
        if (type !is KClass<*>) return null
        if (!lookupType.byType) return null

        return mockHolder::class
            .memberProperties
            .firstOrNull {
                isMatchingType(it, type)
            }?.getAnyIfLateNull(mockHolder)
    }

    private fun isMatchingType(
        it: KProperty1<out Any, Any?>,
        type: KClass<*>,
    ): Boolean {
        val propertyType = it.returnType.classifier
        return if (propertyType is KClass<*>) {
            propertyType.isSubclassOf(type)
        } else {
            false
        }
    }

    private fun isListType(classifier: KClassifier?): Boolean =
        classifier == List::class

    private fun listElementType(param: KParameter): KClass<*>? {
        val typeArg = param.type.arguments.firstOrNull()?.type?.classifier
        return typeArg as? KClass<*>
    }

    private fun lookupListValues(param: KParameter): List<Any>? {
        if (!isListType(param.type.classifier)) return null
        if (!lookupType.byType) return null

        val elementType = listElementType(param) ?: return null

        val mocks = mockHolder::class
            .memberProperties
            .filter { isMatchingType(it, elementType) }
            .mapNotNull { it.getAnyIfLateNull(mockHolder) }

        return mocks.takeIf { it.isNotEmpty() }
    }

    private fun <R> KFunction<R>.constructorToStr(): String {
        val joinedParameters =
            parameters.joinToString(", ") {
                (it.name ?: "<noname arg.>") + " : " + it.type + " = " + lookupToStr(it)
            }
        return "constructor($joinedParameters)"
    }

    private fun lookupToStr(param: KParameter): String =
        (
            lookupValueByName(param.name, param.type.classifier)
                ?: lookupValueByType(param.type.classifier)
                ?: "<not able to lookup>"
        ).toString()
}
