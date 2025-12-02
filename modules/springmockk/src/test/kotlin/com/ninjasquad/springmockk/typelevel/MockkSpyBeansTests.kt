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
package com.ninjasquad.springmockk.typelevel

import com.ninjasquad.springmockk.BeanOverrideTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.ResolvableType
import org.springframework.test.context.bean.override.BeanOverrideHandler
import java.util.stream.Stream

/**
 * Tests for [@MockkSpyBean][MockkSpyBean]
 * declared at the class level, as a repeatable annotation, and via a custom composed
 * annotation.
 *
 * @author Sam Brannen
 * @since 6.2.3
 * @see [gh-34408](https://github.com/spring-projects/spring-framework/issues/34408)
 */
class MockkSpyBeansTests {
    @Test
    fun registrationOrderForTopLevelClass() {
        val mockedServices =
            getRegisteredMockTypes(MockkSpyBeansByTypeIntegrationTests::class.java)
        assertThat(mockedServices).containsExactly(
            Service01::class.java, Service02::class.java, Service03::class.java, Service04::class.java,
            Service05::class.java, Service06::class.java, Service07::class.java
        )
    }

    @Test
    fun registrationOrderForNestedClass() {
        val mockedServices =
            getRegisteredMockTypes(MockkSpyBeansByTypeIntegrationTests.NestedTests::class.java)
        assertThat(mockedServices).containsExactly(
            Service01::class.java, Service02::class.java, Service03::class.java, Service04::class.java,
            Service05::class.java, Service06::class.java, Service07::class.java, Service08::class.java,
            Service09::class.java, Service10::class.java, Service11::class.java, Service12::class.java,
            Service13::class.java
        )
    }


    companion object {
        private fun getRegisteredMockTypes(testClass: Class<*>): Stream<Class<*>> {
            return BeanOverrideTestUtils.findAllHandlers(testClass)
                .stream()
                .map({ obj: BeanOverrideHandler -> obj.getBeanType() })
                .map({ obj: ResolvableType -> obj.getRawClass() })
        }
    }
}
