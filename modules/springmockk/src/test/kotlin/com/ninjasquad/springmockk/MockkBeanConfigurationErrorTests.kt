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

import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test
import org.springframework.context.support.GenericApplicationContext
import java.util.function.Supplier

/**
 * Tests for [@MockkBean][MockkBean].
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
class MockkBeanConfigurationErrorTests {
    @Test
    fun cannotOverrideBeanByNameWithNoSuchBeanName() {
        val context = GenericApplicationContext()
        context.registerBean("anotherBean", String::class.java, Supplier { "example" })
        BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(FailureByNameLookup::class, context)
        assertThatIllegalStateException()
            .isThrownBy { context.refresh() }
            .withMessage(
                "Unable to replace bean: there is no bean with name 'beanToOverride' and type java.lang.String (as required by field 'FailureByNameLookup.example'). If the bean is defined in a @Bean method, make sure the return type is the most specific type possible (for example, the concrete implementation type)."
            )
    }

    @Test
    fun cannotOverrideBeanByNameWithBeanOfWrongType() {
        val context = GenericApplicationContext()
        context.registerBean("beanToOverride", Int::class.java, Supplier { 42 })
        BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(FailureByNameLookup::class, context)
        assertThatIllegalStateException()
            .isThrownBy { context.refresh() }
            .withMessage("Unable to replace bean: there is no bean with name 'beanToOverride' and type java.lang.String (as required by field 'FailureByNameLookup.example'). If the bean is defined in a @Bean method, make sure the return type is the most specific type possible (for example, the concrete implementation type).")
    }

    @Test
    fun cannotOverrideBeanByTypeWithNoSuchBeanType() {
        val context = GenericApplicationContext()
        BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(FailureByTypeLookup::class, context)
        assertThatIllegalStateException()
            .isThrownBy { context.refresh() }
            .withMessage("Unable to override bean: there are no beans of type java.lang.String (as required by field 'FailureByTypeLookup.example'). If the bean is defined in a @Bean method, make sure the return type is the most specific type possible (for example, the concrete implementation type).")
    }

    @Test
    fun cannotOverrideBeanByTypeWithTooManyBeansOfThatType() {
        val context = GenericApplicationContext()
        context.registerBean("bean1", String::class.java, Supplier { "example1" })
        context.registerBean("bean2", String::class.java, Supplier { "example2" })
        BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(FailureByTypeLookup::class, context)
        assertThatIllegalStateException()
            .isThrownBy { context.refresh() }
            .withMessage("Unable to select a bean to override: found 2 beans of type java.lang.String (as required by field 'FailureByTypeLookup.example'): %s",
                listOf("bean1", "bean2")
            )
    }


    class FailureByTypeLookup {
        @MockkBean(enforceOverride = true)
        lateinit var example: String
    }

    class FailureByNameLookup {
        @MockkBean(name = "beanToOverride", enforceOverride = true)
        lateinit var example: String
    }
}
