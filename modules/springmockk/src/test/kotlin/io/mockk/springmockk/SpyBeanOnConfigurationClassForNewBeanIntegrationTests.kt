package io.mockk.springmockk

import io.mockk.springmockk.example.ExampleServiceCaller
import io.mockk.springmockk.example.SimpleExampleService
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension


/**
 * Test [SpykBean] on a configuration class can be used to inject new spy instances.
 *
 * @author Phillip Webb
 * @author JB Nizet
 */
@ExtendWith(SpringExtension::class)
class SpyBeanOnConfigurationClassForNewBeanIntegrationTests {

    @Autowired
    private lateinit var caller: ExampleServiceCaller

    @Test
    fun testSpying() {
        assertThat(this.caller.sayGreeting()).isEqualTo("I say simple")
        verify { caller.service.greeting() }
    }

    @Configuration
    @SpykBean(SimpleExampleService::class)
    @Import(ExampleServiceCaller::class)
    internal class Config

}
