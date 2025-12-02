package com.ninjasquad.springmockk

import com.ninjasquad.springmockk.example.*
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit.jupiter.SpringExtension


/**
 * Test [SpykBean] on a test class field can be used to replace existing bean while
 * preserving qualifiers.
 *
 * @author Andreas Neiser
 * @author JB Nizet
 */
@ExtendWith(SpringExtension::class)
class SpyBeanOnTestFieldForExistingBeanWithQualifierIntegrationTests {

    @SpykBean
    @CustomQualifier
    private lateinit var service: ExampleService

    @Autowired
    private lateinit var caller: ExampleServiceCaller

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Test
    @Throws(Exception::class)
    fun testMocking() {
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
