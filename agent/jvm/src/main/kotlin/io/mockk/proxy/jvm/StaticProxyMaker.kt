package io.mockk.proxy.jvm

import io.mockk.agent.*
import io.mockk.proxy.jvm.transformation.InlineInstrumentation
import io.mockk.proxy.jvm.transformation.TransformationRequest
import io.mockk.proxy.jvm.transformation.TransformationType

internal class StaticProxyMaker(
    private val log: MockKAgentLogger,
    private val inliner: InlineInstrumentation?,
    private val staticHandlers: MutableMap<Any, MockKInvocationHandler>
) : MockKStaticProxyMaker {

    override fun staticProxy(
        clazz: Class<*>,
        handler: MockKInvocationHandler
    ): Cancelable<Class<*>> {
        if (inliner == null) {
            throw MockKAgentException(
                "Failed to create static proxy for $clazz.\n" +
                        "Try running VM with MockK Java Agent\n" +
                        "i.e. with -javaagent:mockk-agent.jar option."
            )
        }

        log.debug("Transforming $clazz for static method interception")
        val request = TransformationRequest(setOf(clazz), TransformationType.STATIC)

        val cancellation = inliner.execute(request)

        staticHandlers[clazz] = handler

        return CancelableResult(clazz, cancellation)
            .alsoOnCancel {
                staticHandlers.remove(clazz)
            }
    }
}
