/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ninjasquad.springmockk

import io.mockk.MockK
import io.mockk.MockKGateway
import org.assertj.core.api.Assertions.assertThat

/**
 * Assertions for Mockk mocks and spies.
 *
 * @author Sam Brannen
 * @since 6.2.1
 */
object MockkAssertions {
    fun assertIsMock(obj: Any) {
        assertThat(obj.isMock).`as`("is a Mockk mock").isTrue()
        assertIsNotSpy(obj)
    }

    fun assertIsMock(obj: Any, message: String) {
        assertThat(obj.isMock).`as`("%s is a Mockk mock", message).isTrue()
        assertIsNotSpy(obj, message)
    }

    fun assertIsNotMock(obj: Any) {
        assertThat(obj.isMock).`as`("is a Mockk mock").isFalse()
    }

    fun assertIsNotMock(obj: Any, message: String) {
        assertThat(obj.isMock).`as`("%s is a Mockk mock", message).isFalse()
    }

    fun assertIsSpy(obj: Any) {
        assertThat(obj.isSpy).`as`("is a Mockk spy").isTrue()
    }

    fun assertIsSpy(obj: Any, message: String) {
        assertThat(obj.isSpy).`as`("%s is a Mockk spy", message).isTrue()
    }

    fun assertIsNotSpy(obj: Any) {
        assertThat(obj.isSpy).`as`("is a Mockk spy").isFalse()
    }

    fun assertIsNotSpy(obj: Any, message: String) {
        assertThat(obj.isSpy).`as`("%s is a Mockk spy", message).isFalse()
    }

    fun assertMockName(mock: Any, name: String) {
        assertThat(mock.toString()).contains("(" + name + "#")
    }

    private val <T : Any> T.isSpy: Boolean
        get() = MockK.useImpl { MockKGateway.implementation().mockTypeChecker.isSpy(this) }

    private val <T : Any> T.isMock: Boolean
        get() = MockK.useImpl { MockKGateway.implementation().mockTypeChecker.isRegularMock(this) }
}
