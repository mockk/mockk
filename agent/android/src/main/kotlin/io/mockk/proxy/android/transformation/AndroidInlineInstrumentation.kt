package io.mockk.proxy.android.transformation

import io.mockk.proxy.MockKAgentLogger
import io.mockk.proxy.android.JvmtiAgent
import io.mockk.proxy.common.transformation.ClassTransformationSpecMap
import io.mockk.proxy.common.transformation.RetransformInlineInstrumnetation

internal class AndroidInlineInstrumentation(
    log: MockKAgentLogger,
    specMap: ClassTransformationSpecMap,
    private val agent: JvmtiAgent
) : RetransformInlineInstrumnetation(log, specMap) {

    override fun retransform(classesToTransform: Collection<Class<*>>) {
        val classes = classesToTransform.filter { !it.isInterface }.toTypedArray()

        if (!classes.isEmpty()) {
            log.trace("Retransforming $classes (skipping interfaces)")
            agent.requestTransformClasses(classes)
        } else {
            log.trace("No need to transform")
        }
    }
}
