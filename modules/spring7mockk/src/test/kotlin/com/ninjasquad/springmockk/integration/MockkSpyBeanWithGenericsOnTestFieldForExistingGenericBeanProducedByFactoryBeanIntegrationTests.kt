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

import com.ninjasquad.springmockk.MockkAssertions.assertIsSpy
import com.ninjasquad.springmockk.MockkSpyBean
import com.ninjasquad.springmockk.example.ExampleGenericService
import com.ninjasquad.springmockk.example.StringExampleGenericService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.ResolvableType
import org.springframework.core.type.AnnotationMetadata
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

/**
 * Tests that [@MockkSpyBean][MockkSpyBean] on a field with generics can
 * be used to replace an existing bean with matching generics that's produced by a
 * [FactoryBean] that's programmatically registered via an
 * [ImportBeanDefinitionRegistrar].
 *
 * @author Andy Wilkinson
 * @author Sam Brannen
 * @since 6.2
 * @see MockkSpyBeanWithGenericsOnTestFieldForExistingGenericBeanIntegrationTests
 */
@SpringJUnitConfig
class MockkSpyBeanWithGenericsOnTestFieldForExistingGenericBeanProducedByFactoryBeanIntegrationTests {
    @MockkSpyBean("exampleService")
    lateinit var exampleService: ExampleGenericService<String>


    @Test
    fun testSpying() {
        assertIsSpy(exampleService)

        assertThat(exampleService).isInstanceOf(StringExampleGenericService::class.java)
    }


    @Configuration(proxyBeanMethods = false)
    @Import(FactoryBeanRegistrar::class)
    class Config

    class FactoryBeanRegistrar : ImportBeanDefinitionRegistrar {
        override fun registerBeanDefinitions(
            importingClassMetadata: AnnotationMetadata,
            registry: BeanDefinitionRegistry
        ) {
            val definition = RootBeanDefinition(ExampleGenericServiceFactoryBean::class.java)
            val targetType = ResolvableType.forClassWithGenerics(
                ExampleGenericServiceFactoryBean::class.java, Any::class.java, ExampleGenericService::class.java
            )
            definition.setTargetType(targetType)
            registry.registerBeanDefinition("exampleService", definition)
        }
    }

    class ExampleGenericServiceFactoryBean<T, U : ExampleGenericService<T>> : FactoryBean<U> {
        @Suppress("UNCHECKED_CAST")
        override fun getObject() = StringExampleGenericService("Enigma") as U
        override fun getObjectType() = ExampleGenericService::class.java
    }
}
