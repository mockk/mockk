package com.ninjasquad.springmockk

import com.ninjasquad.springmockk.example.ExampleServiceCaller
import com.ninjasquad.springmockk.example.SimpleExampleService
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension


/**
 * Test [SpykBean] on a field on a `@Configuration` class can be used to inject
 * new spy instances.
 *
 * @author Phillip Webb
 * @author JB Nizet
 */
@ExtendWith(SpringExtension::class)
class SpyBeanOnConfigurationFieldForNewBeanIntegrationTests {

    @Autowired
    private lateinit var config: Config

    @Autowired
    private lateinit var caller: ExampleServiceCaller

    @Test
    fun testSpying() {
        assertThat(this.caller.sayGreeting()).isEqualTo("I say simple")
        verify { config.exampleService.greeting() }
    }

    @Configuration
    @Import(ExampleServiceCaller::class)
    internal class Config {

        @SpykBean
        lateinit var exampleService: SimpleExampleService

    }

}
