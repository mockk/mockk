package io.mockk.proxy.android

import io.mockk.proxy.*
import io.mockk.proxy.android.transformation.AndroidInlineInstrumentation
import io.mockk.proxy.common.CancelableResult
import io.mockk.proxy.common.transformation.TransformationRequest
import io.mockk.proxy.common.transformation.TransformationType

internal class ConstructorProxyMaker(
    private val inliner: AndroidInlineInstrumentation?,
    private val mocks: MutableMap<Any, MockKInvocationHandler>
) : MockKConstructorProxyMaker {

    override fun constructorProxy(clazz: Class<*>, handler: MockKInvocationHandler): Cancelable<Class<*>> {
        if (inliner == null) {
            throw MockKAgentException("Mocking static is supported starting from Android P")
        }

        val cancellation = inliner.execute(
            TransformationRequest(
                setOf(clazz),
                TransformationType.CONSTRUCTOR
            )
        )

        mocks[clazz] = handler

        return CancelableResult(clazz, cancellation)
            .alsoOnCancel {
                mocks.remove(clazz)
            }
    }
}
