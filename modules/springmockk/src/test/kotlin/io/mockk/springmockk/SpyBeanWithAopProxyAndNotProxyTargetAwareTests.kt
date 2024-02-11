package io.mockk.springmockk

import io.mockk.MockKException
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatExceptionOfType
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
 * Test [SpykBean] when mixed with Spring AOP.
 *
 * @author Phillip Webb
 * @author JB Nizet
 * @see [5837](https://github.com/spring-projects/spring-boot/issues/5837)
 */
@ExtendWith(SpringExtension::class)
class SpyBeanWithAopProxyAndNotProxyTargetAwareTests {

    @SpykBean
    private lateinit var dateService: DateService

    @Test
    fun verifyShouldUseProxyTarget() {
        this.dateService.getDate(false)
        assertThatExceptionOfType(MockKException::class.java).isThrownBy {
            verify(exactly = 1) { dateService.getDate(false) }
        }
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

        @Cacheable(cacheNames = ["test"])
        fun getDate(arg: Boolean) = System.nanoTime()

    }

}
