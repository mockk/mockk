package io.mockk.impl.annotations

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
                    arrayOf(),
                    annotation.recordPrivateCalls
                )
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