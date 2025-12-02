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
import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.aop.support.AopUtils
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.cache.interceptor.CacheResolver
import org.springframework.cache.interceptor.SimpleCacheResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.util.AopTestUtils

/**
 * Tests for [@MockkSpyBean][MockkSpyBean] used in combination with Spring AOP.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 6.2
 * @see [5837](https://github.com/spring-projects/spring-boot/issues/5837)
 *
 * @see MockkBeanAndSpringAopProxyIntegrationTests
 */
@ExtendWith(SpringExtension::class)
class MockkSpyBeanAndSpringAopProxyIntegrationTests {
    @MockkSpyBean
    lateinit var dateService: DateService


    @BeforeEach
    fun resetCache() {
        // We have to clear the "test" cache before each test. Otherwise, method
        // invocations on the Spring AOP proxy will never make it to the Mockk spy.
        dateService.clearCache()
    }

    /**
     * Stubbing and verification for a Mockk spy that is wrapped in a Spring AOP
     * proxy should always work when performed via the ultimate target of the Spring
     * AOP proxy (i.e., the actual spy instance).
     */
    // We need to run this test at least twice to ensure the Mockk spy can be reused
    // across test method invocations without using @DirtestContext.
    @RepeatedTest(2)
    fun stubAndVerifyOnUltimateTargetOfSpringAopProxy() {
        assertThat(AopUtils.isAopProxy(dateService)).`as`("is Spring AOP proxy").isTrue()
        val spy: DateService = AopTestUtils.getUltimateTargetObject(dateService)

        // Unlike with Mockito, we can't test that the AOP proxy is a spy.
        // It's probably best anyway, since it's not actually a spy. The ultimate target
        // is the spy, that can be stubbed and verifies.
        assertIsSpy(spy, "ultimate target")

        every { spy.getDate(false) } returns 1L
        var date = dateService.getDate(false)
        assertThat(date).isOne()

        every { spy.getDate(false) } returns 2L
        date = dateService.getDate(false)
        assertThat(date).isEqualTo(1L) // 1L instead of 2L, because the AOP proxy caches the original value.

        // Each of the following verifies times(1), because the AOP proxy caches the
        // original value and does not delegate to the spy on subsequent invocations.
        verify(exactly = 1) { spy.getDate(false) }
        verify(exactly = 1) { spy.getDate(eq(false)) }
        verify(exactly = 1) { spy.getDate(any()) }
    }

    /**
     * Verification for a Mockk spy that is wrapped in a Spring AOP proxy should
     * always work when performed via the Spring AOP proxy. However, stubbing
     * does not currently work via the Spring AOP proxy.
     *
     *
     * Consequently, this test method supplies the ultimate target of the Spring
     * AOP proxy to stubbing calls, while supplying the Spring AOP proxy to verification
     * calls.
     *
     * This test is **not** disabled with Mockito, but I don't know of any way of making that work
     * with MockK. And frankly, I find it more logical to sub anf verify on the ultimate target
     * of the proxy anyway.
     */
    // We need to run this test at least twice to ensure the Mockk spy can be reused
    // across test method invocations without using @DirtestContext.
    @Disabled("Disabled until Mockk provides support for transparent verification of a proxied spy") // We need to run this test at least twice to ensure the Mockk spy can be reused
    @RepeatedTest(2)
    fun stubOnUltimateTargetAndVerifyOnSpringAopProxy() {
        assertThat(AopUtils.isAopProxy(dateService)).`as`("is Spring AOP proxy").isTrue()

        val spy: DateService = AopTestUtils.getUltimateTargetObject(dateService)

        // Unlike with Mockito, we can't test that the AOP proxy is a spy.
        // It's probably best anyway, since it's not actually a spy. The ultimate target
        // is the spy, that can be stubbed and verifies.
        assertIsSpy(spy, "Spring AOP proxy ultimate target")

        every { spy.getDate(false) } returns 1L
        var date = dateService.getDate(false)
        assertThat(date).isOne()

        every { spy.getDate(false) } returns 2L
        date = dateService.getDate(false)
        assertThat(date).isEqualTo(1L) // 1L instead of 2L, because the AOP proxy caches the original value.

        // Each of the following verifies times(1), because the AOP proxy caches the
        // original value and does not delegate to the spy on subsequent invocations.
        verify(exactly = 1) { dateService.getDate(false) }
        verify(exactly = 1) { dateService.getDate(eq(false)) }
        verify(exactly = 1) { dateService.getDate(any()) }
    }

    /**
     * Ideally, both stubbing and verification should work transparently when a Mockk
     * spy is wrapped in a Spring AOP proxy. However, Mockk currently does not provide
     * support for transparent stubbing of a proxied spy.
     *
     * This test is disabled with Mockito too.
     */
    @Disabled("Disabled until Mockk provides support for transparent stubbing of a proxied spy") // We need to run this test at least twice to ensure the Mockk spy can be reused
    // across test method invocations without using @DirtestContext.
    @RepeatedTest(2)
    @Throws(Exception::class)
    fun stubAndVerifyDirectlyOnSpringAopProxy() {
        assertThat(AopUtils.isCglibProxy(dateService)).`as`("is Spring AOP CGLIB proxy").isTrue()

        // Unlike with Mockito, we can't test that the AOP proxy is a spy.
        // It's probably best anyway, since it's not actually a spy. The ultimate target
        // is the spy, that can be stubbed and verifies.
        assertIsSpy(AopTestUtils.getUltimateTargetObject(dateService))

        every { dateService.getDate(false) } returns 1
        var date = dateService.getDate(false)
        assertThat(date).isOne()

        every { dateService.getDate(false) } returns 2
        date = dateService.getDate(false)
        assertThat(date).isEqualTo(1L) // 1L instead of 2L, because the AOP proxy caches the original value.

        // Each of the following verifies times(1), because the AOP proxy caches the
        // original value and does not delegate to the spy on subsequent invocations.
        verify(exactly = 1) { dateService.getDate(false) }
        verify(exactly = 1) { dateService.getDate(eq(false)) }
        verify(exactly = 1) { dateService.getDate(any()) }
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

        @CacheEvict(cacheNames = ["test"], allEntries = true)
        open fun clearCache() {
        }
    }
}
