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
        val injectMockProperties: MutableList<KProperty1<Any, Any>> = mutableListOf()

        for (property in cls.memberProperties) {
            property.annotated<MockK>(target) { annotation ->
                val type = property.returnType.classifier as? KClass<*> ?: return@annotated null

                gateway.mockFactory.mockk(
                    type,
                    overrideName(annotation.name, property.name),
                    false,
                    arrayOf()
                )

            }


            property.annotated<RelaxedMockK>(target) { annotation ->
                val type = property.returnType.classifier as? KClass<*> ?: return@annotated null

                gateway.mockFactory.mockk(
                    type,
                    overrideName(annotation.name, property.name),
                    true,
                    arrayOf()
                )

            }

            property.annotated<SpyK>(target) { annotation ->
                val obj = (property as KProperty1<Any, Any>).get(target)

                gateway.mockFactory.spyk(
                    null,
                    obj,
                    overrideName(annotation.name, property.name),
                    arrayOf()
                )
            }

            if (property.findAnnotation<InjectMockKs>() != null) {
                val injectMockProperty = property as KProperty1<Any, Any>
                injectMockProperties.add(injectMockProperty)
            }
        }

        if (!injectMockProperties.isEmpty()) {
            for (injectMockProperty in injectMockProperties) {
                matchInjectMockMembers(target, injectMockProperty)
            }
        }
    }

    private fun overrideName(annotationName: String, propertyName: String): String {
        return if (annotationName.isBlank()) {
            propertyName
        } else {
            annotationName
        }
    }

    private fun matchInjectMockMembers(target: Any, property: KProperty1<Any, Any>) {
        tryMakeAccessible(property)
        val obj = property.get(target)
        val targetCls = target::class

        // probably optimize this for large tests
        for (objProperty in obj::class.memberProperties) {

            val matchingProperties = targetCls.memberProperties.filter { targetProperty ->
                val targetPropertyType = targetProperty.returnType.classifier as? KClass<*> ?: return@filter false
                val objectPropertyType  = objProperty.returnType.classifier as? KClass<*> ?: return@filter false

                targetPropertyType == objectPropertyType
            }

            if (!matchingProperties.isEmpty()) {
                when (matchingProperties.size) {
                    1 -> {
                        val matchingProperty = (matchingProperties.first() as KProperty1<Any, Any>).get(target)
                        (objProperty as KMutableProperty1<Any,Any>).set(obj, matchingProperty)
                    }
                    else -> {
                        // todo: match by name like mockito for inject mock classes with same multiple types
                    }
                }
            } else {
                throw MockKException("No matching attributes found in class $target \n " +
                        "for @InjectMock field ${property.name}")
            }
        }
    }

    private inline fun <reified T : Annotation> KProperty<*>.annotated(
        target: Any,
        block: (T) -> Any?
    ) {
        val annotation = findAnnotation<T>()
        if (annotation != null) {
            tryMakeAccessible(this)
            val ret = block(annotation)
            if (ret != null) {
                (this as KMutableProperty1<Any, Any>).set(target, ret)
            }
        }
    }

    private fun tryMakeAccessible(property: KProperty<*>) {
        try {
            property.isAccessible = true
        } catch (ex: Exception) {
            // skip
        }
    }
}