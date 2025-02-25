package io.mockk.springmockk

import io.mockk.every
import io.mockk.springmockk.example.ExampleService
import io.mockk.springmockk.example.ExampleServiceCaller
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension


/**
 * Test [MockkBean] on a field on a `@Configuration` class can be used to
 * inject new mock instances.
 *
 * @author Phillip Webb
 * @author JB Nizet
 */
@ExtendWith(SpringExtension::class)
class MockBeanOnConfigurationFieldForNewBeanIntegrationTests {

    @Autowired
    private lateinit var config: Config

    @Autowired
    private lateinit var caller: ExampleServiceCaller

    @Test
    fun testMocking() {
        every { config.exampleService.greeting() } returns "Boot"
        assertThat(this.caller.sayGreeting()).isEqualTo("I say Boot")
    }

    @Configuration
    @Import(ExampleServiceCaller::class)
    internal class Config {

        @MockkBean
        lateinit var exampleService: ExampleService

    }

}
