package com.ninjasquad.springmockk

import com.ninjasquad.springmockk.example.ExampleService
import com.ninjasquad.springmockk.example.ExampleServiceCaller
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension


/**
 * Test [MockkBean] on a test class field can be used to replace existing beans.
 *
 * @author Phillip Webb
 * @author JB Nizet
 * @see MockBeanOnTestFieldForExistingBeanCacheIntegrationTests
 */
@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [MockBeanOnTestFieldForExistingBeanConfig::class])
class MockBeanOnTestFieldForExistingBeanIntegrationTests {

    @MockkBean
    private lateinit var exampleService: ExampleService

    @Autowired
    private lateinit var caller: ExampleServiceCaller

    @Test
    fun testMocking() {
        every { exampleService.greeting() } returns "Boot"
        assertThat(this.caller.sayGreeting()).isEqualTo("I say Boot")
    }

}
