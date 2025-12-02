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

import com.ninjasquad.springmockk.MockkAssertions.assertIsMock
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.aop.support.AopUtils
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.cache.interceptor.CacheResolver
import org.springframework.cache.interceptor.SimpleCacheResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * Tests for [@MockkBean][MockkBean] used in combination with Spring AOP.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 6.2
 * @see [5837](https://github.com/spring-projects/spring-boot/issues/5837)
 *
 * @see MockkSpyBeanAndSpringAopProxyIntegrationTests
 */
@ExtendWith(SpringExtension::class)
class MockkBeanAndSpringAopProxyIntegrationTests {
    @MockkBean
    lateinit var dateService: DateService


    /**
     * Since the `BeanOverrideBeanFactoryPostProcessor` always registers a
     * manual singleton for a `@MockkBean` mock, the mock that ends up
     * in the application context should not be proxied by Spring AOP (since
     * BeanPostProcessors are never applied to manually registered singletons).
     *
     *
     * In other words, this test effectively verifies that the mock is a
     * standard Mockk mock which does **not** have
     * [@Cacheable][Cacheable] applied to it.
     */
    @RepeatedTest(2)
    fun mockShouldNotBeAnAopProxy() {
        assertThat(AopUtils.isAopProxy(dateService)).`as`("is Spring AOP proxy").isFalse()
        assertIsMock(dateService)

        every { dateService.getDate(false) } returns 1L
        var date = dateService.getDate(false)
        assertThat(date).isOne()

        every { dateService.getDate(false) } returns 2L
        date = dateService.getDate(false)
        assertThat(date).isEqualTo(2L)

        verify(exactly = 2) { dateService.getDate(false) }
        verify(exactly = 2) { dateService.getDate(eq(false)) }
        verify(exactly = 2) { dateService.getDate(any()) }
    }


    @Configuration(proxyBeanMethods = false)
    @EnableCaching(proxyTargetClass = true)
    @Import(DateService::class)
    class Config {
        @Bean
        fun cacheResolver(cacheManager: CacheManager): CacheResolver {
            return SimpleCacheResolver(cacheManager)
        }

        @Bean
        fun cacheManager(): ConcurrentMapCacheManager {
            return ConcurrentMapCacheManager("test")
        }
    }

    open class DateService {
        @Cacheable("test")
        open fun getDate(argument: Boolean): Long {
            return System.nanoTime()
        }
    }
}
