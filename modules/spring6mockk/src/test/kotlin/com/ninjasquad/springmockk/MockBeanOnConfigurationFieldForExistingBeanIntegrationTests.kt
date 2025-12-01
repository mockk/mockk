package com.ninjasquad.springmockk

import com.ninjasquad.springmockk.example.ExampleService
import com.ninjasquad.springmockk.example.ExampleServiceCaller
import com.ninjasquad.springmockk.example.FailingExampleService
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension


/**
 * Test [MockkBean] on a field on a `@Configuration` class can be used to
 * replace existing beans.
 *
 * @author Phillip Webb
 */
@ExtendWith(SpringExtension::class)
class MockBeanOnConfigurationFieldForExistingBeanIntegrationTests {

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
    @Import(ExampleServiceCaller::class, FailingExampleService::class)
    internal class Config {

        @MockkBean
        lateinit var exampleService: ExampleService

    }

}
