package com.ninjasquad.springmockk

import com.ninjasquad.springmockk.example.ExampleService
import com.ninjasquad.springmockk.example.ExampleServiceCaller
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension


/**
 * Test [SpykBean] on a test class field can be used to replace existing beans.
 *
 * @author Phillip Webb
 * @author JB Nizet
 * @see SpyBeanOnTestFieldForExistingBeanCacheIntegrationTests
 */
@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [SpyBeanOnTestFieldForExistingBeanConfig::class])
class SpyBeanOnTestFieldForExistingBeanIntegrationTests {

    @SpykBean
    private lateinit var exampleService: ExampleService

    @Autowired
    private lateinit var caller: ExampleServiceCaller

    @Test
    fun testSpying() {
        assertThat(this.caller.sayGreeting()).isEqualTo("I say simple")
        verify { caller.service.greeting() }
    }

}
