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

    override fun retransform(classes : Array<Class<*>>) {
        agent.requestTransformClasses(classes.filter { !it.isInterface }.toTypedArray())
    }
}
