package io.mockk.springmockk

import io.mockk.every
import io.mockk.springmockk.example.CustomQualifier
import io.mockk.springmockk.example.CustomQualifierExampleService
import io.mockk.springmockk.example.ExampleService
import io.mockk.springmockk.example.ExampleServiceCaller
import io.mockk.springmockk.example.RealExampleService
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit.jupiter.SpringExtension


/**
 * Test [MockBean] on a test class field can be used to replace existing bean while
 * preserving qualifiers.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author JB Nizet
 */
@ExtendWith(SpringExtension::class)
class MockBeanOnTestFieldForExistingBeanWithQualifierIntegrationTests {

    @MockkBean
    @CustomQualifier
    private lateinit var service: ExampleService

    @Autowired
    private lateinit var caller: ExampleServiceCaller

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Test
    fun testMocking() {
        every { service.greeting() } returns "Boot"
        this.caller.sayGreeting()
        verify { service.greeting() }
    }

    @Test
    fun onlyQualifiedBeanIsReplaced() {
        assertThat(this.applicationContext.getBean("service")).isSameAs(this.service)
        val anotherService = this.applicationContext.getBean(
            "anotherService",
            ExampleService::class.java
        )
        assertThat(anotherService.greeting()).isEqualTo("Another")
    }

    @Configuration
    internal class TestConfig {

        @Bean
        fun service(): CustomQualifierExampleService {
            return CustomQualifierExampleService()
        }

        @Bean
        fun anotherService(): ExampleService {
            return RealExampleService("Another")
        }

        @Bean
        fun controller(@CustomQualifier service: ExampleService): ExampleServiceCaller {
            return ExampleServiceCaller(service)
        }

    }

}
