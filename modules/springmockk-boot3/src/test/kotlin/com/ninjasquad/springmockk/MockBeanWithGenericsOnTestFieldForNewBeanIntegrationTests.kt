package com.ninjasquad.springmockk

import com.ninjasquad.springmockk.example.ExampleGenericService
import com.ninjasquad.springmockk.example.ExampleGenericServiceCaller
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension


/**
 * Test [MockkBean] on a test class field can be used to inject new mock instances.
 *
 * @author Phillip Webb
 * @author JB Nizet
 */
@ExtendWith(SpringExtension::class)
class MockBeanWithGenericsOnTestFieldForNewBeanIntegrationTests {

    @MockkBean
    private lateinit var exampleIntegerService: ExampleGenericService<Int>

    @MockkBean
    private lateinit var exampleStringService: ExampleGenericService<String>

    @Autowired
    private lateinit var caller: ExampleGenericServiceCaller

    @Test
    fun testMocking() {
        every { exampleIntegerService.greeting() } returns 200
        every { exampleStringService.greeting() } returns "Boot"
        assertThat(this.caller.sayGreeting()).isEqualTo("I say 200 Boot")
    }

    @Configuration
    @Import(ExampleGenericServiceCaller::class)
    internal class Config

}
