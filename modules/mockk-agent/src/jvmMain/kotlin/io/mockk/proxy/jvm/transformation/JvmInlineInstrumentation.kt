package io.mockk.proxy.jvm.transformation

import io.mockk.proxy.MockKAgentLogger
import io.mockk.proxy.common.transformation.ClassTransformationSpecMap
import io.mockk.proxy.common.transformation.RetransformInlineInstrumentation
import java.lang.instrument.Instrumentation

internal class JvmInlineInstrumentation(
    log: MockKAgentLogger,
    specMap: ClassTransformationSpecMap,
    private val instrumentation: Instrumentation
) : RetransformInlineInstrumentation(log, specMap) {

    override fun retransform(classesToTransform: Collection<Class<*>>) {
        val classesAbleTransform = classesToTransform.filter {
            instrumentation.isModifiableClass(it)
        }.toTypedArray()

        if (classesToTransform.size != classesAbleTransform.size) {
            val nonInstrumentable = classesToTransform - classesAbleTransform.toSet()
            log.warn(
                "Non instrumentable classes(skipped): ${nonInstrumentable.joinToString()}"
            )
        }

        if (classesAbleTransform.isNotEmpty()) {
            log.trace("Retransforming classes ${classesAbleTransform.joinToString { it.name }}")
            instrumentation.retransformClasses(*classesAbleTransform)
        }
    }
}
