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

import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.BeanCreationException
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.context.support.GenericApplicationContext
import org.springframework.stereotype.Component
import java.util.function.Supplier

/**
 * Tests for [@MockkSpyBean][MockkSpyBean].
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
class MockkSpyBeanConfigurationErrorTests {
    @Test
    fun contextCustomizerCannotBeCreatedWithNoSuchBeanName() {
        val context = GenericApplicationContext()
        context.registerBean("present", String::class.java, Supplier { "example" })
        BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(ByNameSingleLookup::class, context)
        assertThatIllegalStateException()
            .isThrownBy { context.refresh() }
            .withMessage("Unable to wrap bean: there is no bean with name 'beanToSpy' and type java.lang.String (as required by field 'ByNameSingleLookup.example'). If the bean is defined in a @Bean method, make sure the return type is the most specific type possible (for example, the concrete implementation type).")
    }

    @Test
    fun contextCustomizerCannotBeCreatedWithNoSuchBeanType() {
        val context = GenericApplicationContext()
        BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(ByTypeSingleLookup::class, context)
        assertThatIllegalStateException()
            .isThrownBy { context.refresh() }
            .withMessage("Unable to select a bean to wrap: there are no beans of type java.lang.String (as required by field 'ByTypeSingleLookup.example'). If the bean is defined in a @Bean method, make sure the return type is the most specific type possible (for example, the concrete implementation type).")
    }

    @Test
    fun contextCustomizerCannotBeCreatedWithTooManyBeansOfThatType() {
        val context = GenericApplicationContext()
        context.registerBean("bean1", String::class.java, Supplier { "example1" })
        context.registerBean("bean2", String::class.java, Supplier { "example2" })
        BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(ByTypeSingleLookup::class, context)
        assertThatIllegalStateException()
            .isThrownBy { context.refresh() }
            .withMessage("Unable to select a bean to wrap: found 2 beans of type java.lang.String (as required by field 'ByTypeSingleLookup.example'): %s",
                listOf("bean1", "bean2")
            )
    }

    @Test
    fun mockitoSpyBeanCannotSpyOnScopedProxy() {
        val context = AnnotationConfigApplicationContext()
        context.register(MyScopedProxy::class.java)
        BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(ScopedProxyTestCase::class, context)
        context.refresh()

        assertThatExceptionOfType(BeanCreationException::class.java)
            .isThrownBy { context.getBean(MyScopedProxy::class.java) }
            .havingRootCause()
            .isInstanceOf(IllegalStateException::class.java)
            .withMessage(
                "@MockkSpyBean cannot be applied to bean 'myScopedProxy', because it is a Spring AOP proxy with a non-static TargetSource. Perhaps you have attempted to spy on a scoped proxy, which is not supported."
            )
    }

    @Test
    fun mockitoSpyBeanCannotSpyOnSelfInjectionScopedProxy() {
        val context = AnnotationConfigApplicationContext()
        context.register(MySelfInjectionScopedProxy::class.java)
        BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(SelfInjectionScopedProxyTestCase::class, context)

        assertThatExceptionOfType(BeanCreationException::class.java)
            .isThrownBy { context.refresh() }
            .havingRootCause()
            .isInstanceOf(IllegalStateException::class.java)
            .withMessage(
                "@MockkSpyBean cannot be applied to bean 'mySelfInjectionScopedProxy', because it is a Spring AOP proxy with a non-static TargetSource. Perhaps you have attempted to spy on a scoped proxy, which is not supported."
            )
    }


    class ByTypeSingleLookup {
        @MockkSpyBean
        lateinit var example: String
    }

    class ByNameSingleLookup {
        @MockkSpyBean("beanToSpy")
        lateinit var example: String
    }

    class ScopedProxyTestCase {
        @MockkSpyBean
        lateinit var myScopedProxy: MyScopedProxy
    }

    class SelfInjectionScopedProxyTestCase {
        @MockkSpyBean
        lateinit var mySelfInjectionScopedProxy: MySelfInjectionScopedProxy
    }

    @Component("myScopedProxy")
    @Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
    class MyScopedProxy

    @Component("mySelfInjectionScopedProxy")
    @Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
    class MySelfInjectionScopedProxy(self: MySelfInjectionScopedProxy?)
}
