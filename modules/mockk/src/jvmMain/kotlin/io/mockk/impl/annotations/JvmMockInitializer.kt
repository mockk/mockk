@file:Suppress("UNCHECKED_CAST")

package io.mockk.impl.annotations

import io.mockk.MockKException
import io.mockk.MockKGateway
import io.mockk.impl.annotations.InjectionHelpers.getAnyIfLateNull
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

class JvmMockInitializer(val gateway: MockKGateway) : MockKGateway.MockInitializer {
    override fun initAnnotatedMocks(
        targets: List<Any>,
        overrideRecordPrivateCalls: Boolean,
        relaxUnitFun: Boolean,
        relaxed: Boolean
    ) {
        for (target in targets) {
            initMock(
                target,
                overrideRecordPrivateCalls,
                relaxUnitFun,
                relaxed
            )
        }
    }

    fun initMock(
        target: Any,
        overrideRecordPrivateCalls: Boolean,
        relaxUnitFun: Boolean,
        relaxed: Boolean
    ) {
        val cls = target::class
        for (property in cls.memberProperties) {
            assignMockK(
                property as KProperty1<Any, Any>,
                target,
                relaxUnitFun,
                relaxed
            )
            assignRelaxedMockK(property, target)

            if (!isAnnotatedWith<InjectMockKs>(property)) {
                assignSpyK(
                    property,
                    target,
                    overrideRecordPrivateCalls
                )
            }
        }

        for (property in cls.memberProperties) {
            property as KProperty1<Any, Any>

            property.annotated<InjectMockKs>(target) { annotation ->
                val mockInjector = MockInjector(
                    target,
                    annotation.lookupType,
                    annotation.injectImmutable,
                    annotation.overrideValues
                )
                val instance = doInjection(property, target, mockInjector)
                convertSpyKAnnotatedToSpy(property, instance, overrideRecordPrivateCalls)
            }
            property.annotated<OverrideMockKs>(target) { annotation ->
                val mockInjector = MockInjector(
                    target,
                    annotation.lookupType,
                    annotation.injectImmutable,
                    true
                )

                doInjection(property, target, mockInjector)
            }
        }
    }

    private fun doInjection(
        property: KProperty1<out Any, Any?>,
        target: Any,
        mockInjector: MockInjector
    ): Any {
        return if (property is KMutableProperty1) {
            val instance = (property as KMutableProperty1<Any, Any?>).getAnyIfLateNull(target)
                    ?: mockInjector.constructorInjection(property.returnType.classifier as KClass<*>)

            mockInjector.propertiesInjection(instance)

            instance
        } else {
            val instance = mockInjector.constructorInjection(property.returnType.classifier as KClass<*>)

            mockInjector.propertiesInjection(instance)

            instance
        }
    }

    private fun convertSpyKAnnotatedToSpy(property: KProperty1<Any, Any>, instance: Any, overrideRecordPrivateCalls: Boolean): Any {
        val spyAnnotation = property.findAnnotation<SpyK>() ?: return instance
        return createSpyK(property, spyAnnotation, instance, overrideRecordPrivateCalls)
    }

    private fun assignSpyK(
        property: KProperty1<Any, Any>,
        target: Any,
        overrideRecordPrivateCalls: Boolean
    ) {
        property.annotated<SpyK>(target) { annotation ->
            val obj = property.get(target)
            createSpyK(property, annotation, obj, overrideRecordPrivateCalls)
        }
    }

    private fun createSpyK(
        property: KProperty1<Any, Any>,
        spyAnnotation: SpyK,
        instance: Any,
        overrideRecordPrivateCalls: Boolean
    ): Any {
        return gateway.mockFactory.spyk(
            null,
            instance,
            overrideName(spyAnnotation.name, property.name),
            moreInterfaces(property),
            spyAnnotation.recordPrivateCalls
                    || overrideRecordPrivateCalls
        )
    }

    private fun assignRelaxedMockK(property: KProperty1<Any, Any>, target: Any) {
        property.annotated<RelaxedMockK>(target) { annotation ->
            val type = property.returnType.classifier as? KClass<*>
                    ?: return@annotated null

            gateway.mockFactory.mockk(
                type,
                overrideName(annotation.name, property.name),
                true,
                moreInterfaces(property),
                relaxUnitFun = false
            )

        }
    }

    private fun assignMockK(
        property: KProperty1<Any, Any>,
        target: Any,
        relaxUnitFun: Boolean,
    relaxed: Boolean
    ) {
        property.annotated<MockK>(target) { annotation ->
            val type = property.returnType.classifier as? KClass<*>
                    ?: return@annotated null

            gateway.mockFactory.mockk(
                type,
                overrideName(annotation.name, property.name),
                annotation.relaxed ||
                        relaxed,
                moreInterfaces(property),
                relaxUnitFun =
                annotation.relaxUnitFun ||
                        relaxUnitFun
            )

        }
    }

    private fun overrideName(annotationName: String, propertyName: String): String =
        annotationName.ifBlank { propertyName }

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
            throw MockKException("Annotation $annotation present on $name read-only property, make it read-write please('lateinit var' or 'var')")
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

    private inline fun <reified T : Annotation>  isAnnotatedWith(property: KProperty<*>) : Boolean {
        val annotation = property.findAnnotation<T>()
        return null != annotation
    }

    companion object {
        private fun moreInterfaces(property: KProperty1<out Any, Any?>) =
            property.annotations
                .filterIsInstance<AdditionalInterface>()
                .map { it.type }
                .toTypedArray()

    }
}
