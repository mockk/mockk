/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.mockk.proxy.android

import io.mockk.agent.*
import io.mockk.proxy.android.transformation.InlineInstrumentation
import io.mockk.proxy.android.transformation.TransformationRequest
import io.mockk.proxy.android.transformation.TransformationType

internal class StaticProxyMaker(
    private val inliner: InlineInstrumentation?,
    private val mocks: MutableMap<Any, MockKInvocationHandler>
) : MockKStaticProxyMaker {

    override fun staticProxy(clazz: Class<*>, handler: MockKInvocationHandler): Cancelable<Class<*>> {
        if (inliner == null) {
            throw MockKAgentException("Mocking static is supported starting from Android P")
        }

        val cancellation = inliner.execute(
            TransformationRequest(
                setOf(clazz),
                TransformationType.STATIC
            )
        )

        mocks[clazz] = handler

        return CancelableResult(clazz, cancellation)
            .alsoOnCancel {
                mocks.remove(clazz)
            }
    }
}
