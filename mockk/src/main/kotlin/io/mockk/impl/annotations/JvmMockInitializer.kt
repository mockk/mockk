package io.mockk.impl.annotations

import io.mockk.MockKException
import io.mockk.MockKGateway
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

class JvmMockInitializer(val gateway: MockKGateway) : MockKGateway.MockInitializer {
    override fun initAnnotatedMocks(targets: List<Any>) {
        for (target in targets) {
            initMock(target)
        }
    }

    @MockK
    fun initMock(target: Any) {
        val cls = target::class
        for (property in cls.memberProperties) {
            assignMockK(property as KProperty1<Any, Any>, target)
            assignRelaxedMockK(property, target)
            assignSpyK(property, target)
        }
    }

    private fun assignSpyK(property: KProperty1<Any, Any>, target: Any) {
        property.annotated<SpyK>(target) { annotation ->
            val obj = property.get(target)

            gateway.mockFactory.spyk(
                null,
                obj,
                overrideName(annotation.name, property.name),
                moreInterfaces(property),
                annotation.recordPrivateCalls
            )
        }
    }

    private fun assignRelaxedMockK(property: KProperty1<Any, Any>, target: Any) {
        property.annotated<RelaxedMockK>(target) { annotation ->
            val type = property.returnType.classifier as? KClass<*>
                    ?: return@annotated null

            gateway.mockFactory.mockk(
                type,
                overrideName(annotation.name, property.name),
                true,
                moreInterfaces(property)
            )

        }
    }

    private fun assignMockK(property: KProperty1<Any, Any>, target: Any) {
        property.annotated<MockK>(target) { annotation ->
            val type = property.returnType.classifier as? KClass<*>
                    ?: return@annotated null

            gateway.mockFactory.mockk(
                type,
                overrideName(annotation.name, property.name),
                false,
                moreInterfaces(property)
            )

        }
    }

    private fun overrideName(annotationName: String, propertyName: String): String {
        return if (annotationName.isBlank()) {
            propertyName
        } else {
            annotationName
        }
    }

    private inline fun <reified T : Annotation> KProperty1<Any, Any>.annotated(
        target: Any,
        block: (T) -> Any?
    ) {
        val annotation = findAnnotation<T>()
                ?: return

        tryMakeAccessible(this)
        if (isAlreadyInitialized(this, target)) return

        val ret = block(annotation)
                ?: return

        if (this !is KMutableProperty1<Any, Any>) {
            throw MockKException("Annotation $annotation present on $name read-only property, make it read-write please('lateinit var' for example)")
        }

        set(target, ret)
    }

    private fun isAlreadyInitialized(property: KProperty1<Any, Any?>, target: Any): Boolean {
        try {
            val value = property.get(target)
                    ?: return false

            return gateway.mockFactory.isMock(value)
        } catch (ex: Exception) {
            return false
        }
    }

    private fun tryMakeAccessible(property: KProperty<*>) {
        try {
            property.isAccessible = true
        } catch (ex: Exception) {
            // skip
        }
    }

    companion object {
        private fun moreInterfaces(property: KProperty1<out Any, Any?>) =
            property.annotations
                .filter { it is AdditionalInterface }
                .map { it as AdditionalInterface }
                .map { it.type }
                .toTypedArray()

    }
}