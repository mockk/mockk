package com.ninjasquad.springmockk

import org.springframework.core.ResolvableType
import org.springframework.test.context.bean.override.BeanOverrideHandler
import org.springframework.test.context.bean.override.BeanOverrideProcessor
import java.lang.reflect.Field
import kotlin.reflect.KClass


/**
 * [BeanOverrideProcessor] implementation that provides support for
 * [@MockkBean][MockkBean] and [@MockkSpyBean][MockkSpyBean].
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @author Jean-Baptiste Nizet
 *
 * @see MockkBean
 * @see MockkSpyBean
 */
internal class MockkBeanOverrideProcessor : BeanOverrideProcessor {
    override fun createHandler(
        overrideAnnotation: Annotation,
        testClass: Class<*>,
        field: Field
    ): AbstractMockkBeanOverrideHandler {
        if (overrideAnnotation is MockkBean) {
            check(overrideAnnotation.types.size == 0) {
                "The @MockkBean 'types' attribute must be omitted when declared on a field"
            }
            return MockkBeanOverrideHandler(field, ResolvableType.forField(field, testClass), overrideAnnotation)
        } else if (overrideAnnotation is MockkSpyBean) {
            check(overrideAnnotation.types.size == 0) {
                "The @MockkSpyBean 'types' attribute must be omitted when declared on a field"
            }
            return MockkSpyBeanOverrideHandler(field, ResolvableType.forField(field, testClass), overrideAnnotation)
        }
        throw IllegalStateException(
            "Invalid annotation passed to MockkBeanOverrideProcessor: expected either @MockkBean or @MockkSpyBean on field %s.%s"
                .format(field.getDeclaringClass().getName(), field.getName())
        )
    }

    override fun createHandlers(
        overrideAnnotation: Annotation,
        testClass: Class<*>
    ): List<BeanOverrideHandler> {
        if (overrideAnnotation is MockkBean) {
            val types: Array<KClass<*>> = overrideAnnotation.types
            check(types.size > 0) {
                "The @MockkBean 'types' attribute must not be empty when declared on a class"
            }
            check(overrideAnnotation.name.isEmpty() || types.size == 1) {
                "The @MockkBean 'name' attribute cannot be used when mocking multiple types"
            }
            val handlers = mutableListOf<BeanOverrideHandler>()
            for (type in types) {
                handlers.add(MockkBeanOverrideHandler(ResolvableType.forClass(type.java), overrideAnnotation))
            }
            return handlers
        } else if (overrideAnnotation is MockkSpyBean) {
            val types: Array<KClass<*>> = overrideAnnotation.types
            check(types.size > 0) {
                "The @MockkSpyBean 'types' attribute must not be empty when declared on a class"
            }
            check(overrideAnnotation.name.isEmpty() || types.size == 1) {
                "The @MockkSpyBean 'name' attribute cannot be used when mocking multiple types"
            }
            val handlers: MutableList<BeanOverrideHandler> = ArrayList<BeanOverrideHandler>()
            for (type in types) {
                handlers.add(MockkSpyBeanOverrideHandler(ResolvableType.forClass(type.java), overrideAnnotation))
            }
            return handlers
        }
        throw IllegalStateException("Invalid annotation passed to MockkBeanOverrideProcessor: expected either @MockkBean or @MockkSpyBean on test class ${testClass.getName()}")
    }
}
