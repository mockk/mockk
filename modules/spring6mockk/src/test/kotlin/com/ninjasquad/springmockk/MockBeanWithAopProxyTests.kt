package com.ninjasquad.springmockk

import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.cache.interceptor.CacheResolver
import org.springframework.cache.interceptor.SimpleCacheResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Service
import org.springframework.test.context.junit.jupiter.SpringExtension


/**
 * Test [MockkBean] when mixed with Spring AOP.
 *
 * @author Phillip Webb
 * @author JB Nizet
 * @see [5837](https://github.com/spring-projects/spring-boot/issues/5837)
 */
@ExtendWith(SpringExtension::class)
class MockBeanWithAopProxyTests {

    @MockkBean
    private lateinit var dateService: DateService

    @Test
    fun verifyShouldUseProxyTarget() {
        every { dateService.getDate(false) } returns 1L
        val d1 = this.dateService.getDate(false)
        assertThat(d1).isEqualTo(1L)
        every { dateService.getDate(false) } returns 2L
        val d2 = this.dateService.getDate(false)
        assertThat(d2).isEqualTo(2L)
        verify(exactly = 2) { dateService.getDate(false) }
        verify(exactly = 2) { dateService.getDate(eq(false)) }
        verify(exactly = 2) { dateService.getDate(any<Boolean>()) }
    }

    @Configuration
    @EnableCaching(proxyTargetClass = true)
    @Import(DateService::class)
    internal class Config {

        @Bean
        fun cacheResolver(cacheManager: CacheManager): CacheResolver {
            val resolver = SimpleCacheResolver()
            resolver.cacheManager = cacheManager
            return resolver
        }

        @Bean
        fun cacheManager(): ConcurrentMapCacheManager {
            val cacheManager = ConcurrentMapCacheManager()
            cacheManager.setCacheNames(listOf("test"))
            return cacheManager
        }

    }

    @Service
    internal class DateService {

        @Cacheable(cacheNames = arrayOf("test"))
        fun getDate(argument: Boolean): Long {
            return System.nanoTime()
        }

    }

}
