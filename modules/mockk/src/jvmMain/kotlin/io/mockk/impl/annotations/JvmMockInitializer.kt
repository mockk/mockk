@file:Suppress("UNCHECKED_CAST")

package io.mockk.impl.annotations

import io.mockk.MockKException
import io.mockk.MockKGateway
import io.mockk.impl.annotations.InjectionHelpers.getAnyIfLateNull
import io.mockk.impl.annotations.InjectionHelpers.getConstructorParameterTypes
import io.mockk.impl.annotations.InjectionHelpers.getReturnTypeKClass
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

class JvmMockInitializer(
    val gateway: MockKGateway,
) : MockKGateway.MockInitializer {
    override fun initAnnotatedMocks(
        targets: List<Any>,
        overrideRecordPrivateCalls: Boolean,
        relaxUnitFun: Boolean,
        relaxed: Boolean,
    ) {
        for (target in targets) {
            initMock(
                target,
                overrideRecordPrivateCalls,
                relaxUnitFun,
                relaxed,
            )
        }
    }

    fun initMock(
        target: Any,
        overrideRecordPrivateCalls: Boolean,
        relaxUnitFun: Boolean,
        relaxed: Boolean,
    ) {
        val cls = target::class
        for (property in cls.memberProperties) {
            assignMockK(
                property as KProperty1<Any, Any>,
                target,
                relaxUnitFun,
                relaxed,
            )
            assignRelaxedMockK(property, target)

            if (!isAnnotatedWith<InjectMockKs>(property)) {
                assignSpyK(
                    property,
                    target,
                    overrideRecordPrivateCalls,
                )
            }
        }

        // Collect @InjectMockKs properties and sort by dependency order
        val injectMockKsProperties =
            cls.memberProperties
                .map { it as KProperty1<Any, Any> }
                .filter { it.findAnnotation<InjectMockKs>() != null }

        val sortedProperties = sortByDependencyOrder(injectMockKsProperties)

        for (property in sortedProperties) {
            property.annotated<InjectMockKs>(target) { annotation ->
                val mockInjector =
                    MockInjector(
                        target,
                        annotation.lookupType,
                        annotation.injectImmutable,
                        annotation.overrideValues,
                    )
                val instance = doInjection(property, target, mockInjector)
                convertSpyKAnnotatedToSpy(property, instance, overrideRecordPrivateCalls)
            }
        }

        for (property in cls.memberProperties) {
            property as KProperty1<Any, Any>

            property.annotated<OverrideMockKs>(target) { annotation ->
                val mockInjector =
                    MockInjector(
                        target,
                        annotation.lookupType,
                        annotation.injectImmutable,
                        true,
                    )

                doInjection(property, target, mockInjector)
            }
        }
    }

    /**
     * Sorts @InjectMockKs properties by dependency order using topological sort.
     * Properties with no dependencies on other @InjectMockKs types are processed first.
     */
    private fun sortByDependencyOrder(properties: List<KProperty1<Any, Any>>): List<KProperty1<Any, Any>> {
        if (properties.size <= 1) return properties

        val dependencies = buildDependencyGraph(properties)
        return topologicalSort(properties, dependencies)
    }

    private fun buildDependencyGraph(properties: List<KProperty1<Any, Any>>): Map<KProperty1<Any, Any>, Set<KProperty1<Any, Any>>> {
        val typeToProperty =
            properties
                .mapNotNull { prop -> prop.getReturnTypeKClass()?.let { it to prop } }
                .toMap()

        return properties.associateWith { property ->
            val clazz = property.getReturnTypeKClass() ?: return@associateWith emptySet()

            clazz
                .getConstructorParameterTypes()
                .mapNotNull { paramType -> typeToProperty[paramType] }
                .toSet()
        }
    }

    /**
     * Performs topological sort using Kahn's algorithm.
     * @throws MockKException if circular dependency is detected.
     */
    private fun topologicalSort(
        properties: List<KProperty1<Any, Any>>,
        dependencies: Map<KProperty1<Any, Any>, Set<KProperty1<Any, Any>>>,
    ): List<KProperty1<Any, Any>> {
        val inDegree =
            properties
                .associateWith { prop ->
                    dependencies[prop]?.size ?: 0
                }.toMutableMap()

        val result = mutableListOf<KProperty1<Any, Any>>()
        val queue = ArrayDeque(properties.filter { inDegree[it] == 0 })

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            result.add(current)

            for (prop in properties) {
                if (dependencies[prop]?.contains(current) == true) {
                    val newDegree = inDegree.getValue(prop) - 1
                    inDegree[prop] = newDegree
                    if (newDegree == 0) {
                        queue.add(prop)
                    }
                }
            }
        }

        if (result.size != properties.size) {
            val circular = properties.filter { it !in result }
            throw MockKException(
                "Circular dependency detected in @InjectMockKs: ${circular.map { it.name }}",
            )
        }

        return result
    }

    private fun doInjection(
        property: KProperty1<out Any, Any?>,
        target: Any,
        mockInjector: MockInjector,
    ): Any =
        if (property is KMutableProperty1) {
            val instance =
                (property as KMutableProperty1<Any, Any?>).getAnyIfLateNull(target)
                    ?: mockInjector.constructorInjection(property.returnType.classifier as KClass<*>)

            mockInjector.propertiesInjection(instance)

            instance
        } else {
            val instance = mockInjector.constructorInjection(property.returnType.classifier as KClass<*>)

            mockInjector.propertiesInjection(instance)

            instance
        }

    private fun convertSpyKAnnotatedToSpy(
        property: KProperty1<Any, Any>,
        instance: Any,
        overrideRecordPrivateCalls: Boolean,
    ): Any {
        val spyAnnotation = property.findAnnotation<SpyK>() ?: return instance
        return createSpyK(property, spyAnnotation, instance, overrideRecordPrivateCalls)
    }

    private fun assignSpyK(
        property: KProperty1<Any, Any>,
        target: Any,
        overrideRecordPrivateCalls: Boolean,
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
        overrideRecordPrivateCalls: Boolean,
    ): Any =
        gateway.mockFactory.spyk(
            null,
            instance,
            overrideName(spyAnnotation.name, property.name),
            moreInterfaces(property),
            spyAnnotation.recordPrivateCalls ||
                overrideRecordPrivateCalls,
        )

    private fun assignRelaxedMockK(
        property: KProperty1<Any, Any>,
        target: Any,
    ) {
        property.annotated<RelaxedMockK>(target) { annotation ->
            val type = property.getReturnTypeKClass() ?: return@annotated null

            gateway.mockFactory.mockk(
                type,
                overrideName(annotation.name, property.name),
                true,
                moreInterfaces(property),
                relaxUnitFun = false,
            )
        }
    }

    private fun assignMockK(
        property: KProperty1<Any, Any>,
        target: Any,
        relaxUnitFun: Boolean,
        relaxed: Boolean,
    ) {
        property.annotated<MockK>(target) { annotation ->
            val type = property.getReturnTypeKClass() ?: return@annotated null

            gateway.mockFactory.mockk(
                type,
                overrideName(annotation.name, property.name),
                annotation.relaxed ||
                    relaxed,
                moreInterfaces(property),
                relaxUnitFun =
                    annotation.relaxUnitFun ||
                        relaxUnitFun,
            )
        }
    }

    private fun overrideName(
        annotationName: String,
        propertyName: String,
    ): String = annotationName.ifBlank { propertyName }

    private inline fun <reified T : Annotation> KProperty1<Any, Any>.annotated(
        target: Any,
        block: (T) -> Any?,
    ) {
        val annotation =
            findAnnotation<T>()
                ?: return

        tryMakeAccessible(this)
        if (isAlreadyInitialized(this, target)) return

        val ret =
            block(annotation)
                ?: return

        if (this !is KMutableProperty1<Any, Any>) {
            throw MockKException(
                "Annotation $annotation present on $name read-only property, make it read-write please('lateinit var' or 'var')",
            )
        }

        set(target, ret)
    }

    private fun isAlreadyInitialized(
        property: KProperty1<Any, Any?>,
        target: Any,
    ): Boolean {
        try {
            val value =
                property.get(target)
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

    private inline fun <reified T : Annotation> isAnnotatedWith(property: KProperty<*>): Boolean {
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
