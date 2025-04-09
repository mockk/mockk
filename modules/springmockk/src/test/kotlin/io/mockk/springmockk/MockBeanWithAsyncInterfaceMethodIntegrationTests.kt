package io.mockk.springmockk

import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.test.context.junit.jupiter.SpringExtension


/**
 * Tests for a mock bean where the mocked interface has an async method.
 *
 * @author Andy Wilkinson
 * @author JB Nizet
 */
@ExtendWith(SpringExtension::class)
class MockBeanWithAsyncInterfaceMethodIntegrationTests {

    @MockkBean
    private lateinit var transformer: Transformer

    @Autowired
    private lateinit var service: MyService

    @Test
    fun mockedMethodsAreNotAsync() {
        every { transformer.transform("foo") } returns "bar"
        assertThat(this.service.transform("foo")).isEqualTo("bar")
    }

    internal interface Transformer {

        @Async
        fun transform(input: String): String

    }

    internal class MyService(val transformer: Transformer) {

        fun transform(input: String): String {
            return this.transformer.transform(input)
        }

    }

    @Configuration
    @EnableAsync
    internal class MyConfiguration {

        @Bean
        fun myService(transformer: Transformer): MyService {
            return MyService(transformer)
        }

    }

}
