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
package com.ninjasquad.springmockk.integration

import com.ninjasquad.springmockk.integration.AbstractMockkBeanAndGenericsIntegrationTests.SomethingImpl
import com.ninjasquad.springmockk.integration.AbstractMockkBeanAndGenericsIntegrationTests.ThingImpl
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Concrete implementation of [AbstractMockkBeanAndGenericsIntegrationTests].
 *
 * @author Madhura Bhave
 * @author Sam Brannen
 * @since 6.2
 */
class MockkBeanAndGenericsIntegrationTests :
    AbstractMockkBeanAndGenericsIntegrationTests<ThingImpl, SomethingImpl>() {
    @Test
    fun MockkBeanShouldResolveConcreteType() {
        assertThat(something).isExactlyInstanceOf(SomethingImpl::class.java)

        every { something.speak() } returns "Hola"
        assertThat(thing.something.speak()).isEqualTo("Hola")
    }
}
