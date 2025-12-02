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

import io.mockk.mockk
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ContextConfigurationAttributes
import org.springframework.test.context.ContextCustomizer
import org.springframework.test.context.MergedContextConfiguration
import kotlin.reflect.KClass
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.isAccessible

/**
 * Test utilities for [BeanOverrideContextCustomizer] that are public so
 * that specific bean override implementations can use them.
 *
 * @author Stephane Nicoll
 */
object BeanOverrideContextCustomizerTestUtils {
    private val factory = BeanOverrideContextCustomizerFactoryAdapter.create()

    /**
     * Create a [ContextCustomizer] for the given `testClass`. Return
     * a customizer to handle any use of [BeanOverride] or `null` if
     * the test class does not use them.
     * @param testClass a test class to introspect
     * @return a context customizer for bean override support, or null
     */
    fun createContextCustomizer(testClass: KClass<*>): ContextCustomizer? {
        return factory.createContextCustomizer(
            testClass,
            listOf(ContextConfigurationAttributes(testClass.java))
        )
    }

    /**
     * Customize the given [application][ConfigurableApplicationContext] for the given `testClass`.
     * @param testClass the test to process
     * @param context the context to customize
     */
    fun customizeApplicationContext(testClass: KClass<*>, context: ConfigurableApplicationContext) {
        val contextCustomizer: ContextCustomizer? = createContextCustomizer(testClass)
        contextCustomizer?.customizeContext(context, mockk<MergedContextConfiguration>())
    }
}

class BeanOverrideContextCustomizerFactoryAdapter(private val delegate: Any) {
    fun createContextCustomizer(
        testClass: KClass<*>,
        configAttributes: List<ContextConfigurationAttributes>
    ): ContextCustomizer? {
        val actualCreateContextCustomizer = delegate::class.memberFunctions.first { it.name == "createContextCustomizer" }
        actualCreateContextCustomizer.isAccessible = true
        return actualCreateContextCustomizer.call(delegate, testClass.java, configAttributes) as ContextCustomizer?
    }

    companion object {
        fun create(): BeanOverrideContextCustomizerFactoryAdapter {
            val clazz = Class.forName("org.springframework.test.context.bean.override.BeanOverrideContextCustomizerFactory").kotlin
            val constructor = clazz.constructors.first { it.parameters.isEmpty() }
            constructor.isAccessible = true
            val delegate = constructor.call()
            return BeanOverrideContextCustomizerFactoryAdapter(delegate)
        }
    }
}
