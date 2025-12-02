package com.ninjasquad.springmockk

import com.ninjasquad.springmockk.example.ExampleGenericStringServiceCaller
import com.ninjasquad.springmockk.example.SimpleExampleStringGenericService
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.junit.jupiter.SpringExtension


/**
 * Test [SpykBean] on a test class field can be used to inject a spy instance when
 * there are multiple candidates and one is primary.
 *
 * @author Phillip Webb
 * @author JB Nizet
 */
@ExtendWith(SpringExtension::class)
class SpyBeanOnTestFieldForMultipleExistingBeansWithOnePrimaryIntegrationTests {

    @SpykBean
    private lateinit var spy: SimpleExampleStringGenericService

    @Autowired
    private lateinit var caller: ExampleGenericStringServiceCaller


    @Test
    fun testSpying() {
        assertThat(this.caller.sayGreeting()).isEqualTo("I say two")
        assertThat(this.spy.toString()).contains("two")
        verify { spy.greeting() }
    }

    @Configuration
    @Import(ExampleGenericStringServiceCaller::class)
    internal class Config {

        @Bean
        fun one(): SimpleExampleStringGenericService {
            return SimpleExampleStringGenericService("one")
        }

        @Bean
        @Primary
        fun two(): SimpleExampleStringGenericService {
            return SimpleExampleStringGenericService("two")
        }

    }

}
