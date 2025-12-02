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
package com.ninjasquad.springmockk.hierarchies

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.ContextHierarchy
import org.springframework.test.context.aot.DisabledInAotMode
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * If the [ApplicationContext] for [ErrorIfContextReloadedConfig] is
 * loaded twice (i.e., not properly cached), either this test class or
 * [ReusedParentConfigV2Tests] will fail when both test classes are run
 * within the same test suite.
 *
 * @author Sam Brannen
 * @since 6.2.6
 */
@ExtendWith(SpringExtension::class)
@ContextHierarchy(
    ContextConfiguration(classes = [ErrorIfContextReloadedConfig::class]),
    ContextConfiguration(classes = [FooService::class], name = "child")
)
@DisabledInAotMode("@ContextHierarchy is not supported in AOT")
class ReusedParentConfigV1Tests {
    @Autowired
    lateinit var sharedConfig: ErrorIfContextReloadedConfig

    @MockkBean(contextName = "child")
    lateinit var fooService: FooService


    @Test
    fun test(context: ApplicationContext) {
        assertThat(context.parent!!.getBeanNamesForType(FooService::class.java)).isEmpty()
        assertThat(context.getBeanNamesForType(FooService::class.java)).hasSize(1)

        every { fooService.foo() } returns "mock"
        assertThat(fooService.foo()).isEqualTo("mock")
    }
}
