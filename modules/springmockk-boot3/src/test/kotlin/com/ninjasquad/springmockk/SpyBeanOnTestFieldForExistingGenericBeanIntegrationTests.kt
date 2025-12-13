package com.ninjasquad.springmockk

import com.ninjasquad.springmockk.example.ExampleGenericService
import com.ninjasquad.springmockk.example.ExampleGenericServiceCaller
import com.ninjasquad.springmockk.example.SimpleExampleIntegerGenericService
import com.ninjasquad.springmockk.example.SimpleExampleStringGenericService
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension


/**
 * Test [SpykBean] on a test class field can be used to replace existing beans.
 *
 * @author Phillip Webb
 * @author JB Nizet
 */
@ExtendWith(SpringExtension::class)
class SpyBeanOnTestFieldForExistingGenericBeanIntegrationTests {

    // gh-7625

    @SpykBean
    private lateinit var exampleService: ExampleGenericService<String>

    @Autowired
    private lateinit var caller: ExampleGenericServiceCaller

    @Test
    fun testSpying() {
        assertThat(this.caller.sayGreeting()).isEqualTo("I say 123 simple")
        verify { exampleService.greeting() }
    }

    @Configuration
    @Import(ExampleGenericServiceCaller::class, SimpleExampleIntegerGenericService::class)
    internal class SpyBeanOnTestFieldForExistingBeanConfig {

        @Bean
        fun simpleExampleStringGenericService(): ExampleGenericService<String> {
            // In order to trigger issue we need a method signature that returns the
            // generic type not the actual implementation class
            return SimpleExampleStringGenericService()
        }

    }

}
