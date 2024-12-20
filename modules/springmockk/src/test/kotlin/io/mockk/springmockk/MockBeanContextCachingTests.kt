package io.mockk.springmockk

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.BootstrapContext
import org.springframework.test.context.MergedContextConfiguration
import org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate
import org.springframework.test.context.cache.DefaultContextCache
import org.springframework.test.util.ReflectionTestUtils


/**
 * Tests for application context caching when using [@MockBean][MockBean].
 *
 * @author Andy Wilkinson
 */
internal class MockBeanContextCachingTests {

    private val contextCache = DefaultContextCache()

    private val delegate = DefaultCacheAwareContextLoaderDelegate(
        contextCache
    )

    @Suppress("UNCHECKED_CAST")
    @AfterEach
    fun clearCache() {
        val contexts = ReflectionTestUtils
            .getField(
                contextCache,
                "contextMap"
            ) as Map<MergedContextConfiguration, ApplicationContext>
        for (context in contexts.values) {
            if (context is ConfigurableApplicationContext) {
                context.close()
            }
        }
        contextCache.clear()
    }

    @Test
    fun whenThereIsANormalBeanAndAMockBeanThenTwoContextsAreCreated() {
        bootstrapContext(TestClass::class.java)
        assertThat(contextCache.size()).isEqualTo(1)
        bootstrapContext(MockedBeanTestClass::class.java)
        assertThat(contextCache.size()).isEqualTo(2)
    }

    @Test
    fun whenThereIsTheSameMockedBeanInEachTestClassThenOneContextIsCreated() {
        bootstrapContext(MockedBeanTestClass::class.java)
        assertThat(contextCache.size()).isEqualTo(1)
        bootstrapContext(AnotherMockedBeanTestClass::class.java)
        assertThat(contextCache.size()).isEqualTo(1)
    }

    private fun bootstrapContext(theTestClass: Class<*>) {
        val bootstrapper = SpringBootTestContextBootstrapper()
        val bootstrapContext: BootstrapContext = mockk<BootstrapContext> {
            every { testClass } returns theTestClass
        }
        bootstrapper.bootstrapContext = bootstrapContext
        every { bootstrapContext.cacheAwareContextLoaderDelegate } returns delegate
        val testContext = bootstrapper.buildTestContext()
        testContext.applicationContext
    }

    @SpringBootTest(classes = [TestConfiguration::class])
    internal class TestClass

    @SpringBootTest(classes = [TestConfiguration::class])
    internal class MockedBeanTestClass {
        @MockkBean
        private lateinit var testBean: TestBean
    }

    @SpringBootTest(classes = [TestConfiguration::class])
    internal class AnotherMockedBeanTestClass {
        @MockkBean
        private lateinit var testBean: TestBean
    }

    @Configuration
    internal class TestConfiguration {
        @Bean
        fun testBean(): TestBean {
            return TestBean()
        }
    }

    internal class TestBean
}
