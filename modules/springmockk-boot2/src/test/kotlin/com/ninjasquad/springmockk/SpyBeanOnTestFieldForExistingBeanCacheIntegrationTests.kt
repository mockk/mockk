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
 * Test [SpykBean] on a test class field can be used to replace existing beans when
 * the context is cached. This test is identical to
 * [SpyBeanOnTestFieldForExistingBeanIntegrationTests] so one of them should trigger
 * application context caching.
 *
 * @author Phillip Webb
 * @author JB Nizet
 * @see SpyBeanOnTestFieldForExistingBeanIntegrationTests
 */
@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [SpyBeanOnTestFieldForExistingBeanConfig::class])
class SpyBeanOnTestFieldForExistingBeanCacheIntegrationTests {

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
