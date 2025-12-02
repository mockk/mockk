package com.ninjasquad.springmockk

import com.ninjasquad.springmockk.example.ExampleService
import com.ninjasquad.springmockk.example.ExampleServiceCaller
import com.ninjasquad.springmockk.example.FailingExampleService
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.*
import org.springframework.test.context.junit.jupiter.SpringExtension


/**
 * Test [MockkBean] when used in combination with scoped proxy targets.
 *
 * @author Phillip Webb
 * @author JB Nizet
 * @see [gh-5724](https://github.com/spring-projects/spring-boot/issues/5724)
 */
@ExtendWith(SpringExtension::class)
class MockBeanOnScopedProxyTests {

    @MockkBean
    private lateinit var exampleService: ExampleService

    @Autowired
    private lateinit var caller: ExampleServiceCaller

    @Test
    fun testMocking() {
        every { caller.service.greeting() } returns "Boot"
        assertThat(this.caller.sayGreeting()).isEqualTo("I say Boot")
    }

    @Configuration
    @Import(ExampleServiceCaller::class)
    internal class Config {

        @Bean
        @Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
        fun exampleService(): ExampleService {
            return FailingExampleService()
        }

    }

}
