package io.mockk.proxy.jvm.transformation

import io.mockk.proxy.MockKAgentLogger
import io.mockk.proxy.common.transformation.ClassTransformationSpecMap
import io.mockk.proxy.common.transformation.RetransformInlineInstrumnetation
import java.lang.instrument.Instrumentation

internal class JvmInlineInstrumentation(
    log: MockKAgentLogger,
    specMap: ClassTransformationSpecMap,
    private val instrumentation: Instrumentation
) : RetransformInlineInstrumnetation(log, specMap) {

    override fun retransform(classesToTransform: Collection<Class<*>>) {
        val classesAbleTransform = classesToTransform.filter {
            instrumentation.isModifiableClass(it)
        }.toTypedArray()

        if (classesToTransform.size != classesAbleTransform.size) {
            log.warn(
                "Non instrumentable classes(skipped): " +
                        (classesToTransform - classesAbleTransform).joinToString()
            )
        }

        if (classesAbleTransform.isNotEmpty()) {
            log.trace("Retransforming $classesAbleTransform")
            instrumentation.retransformClasses(*classesAbleTransform)
        }
    }
}
