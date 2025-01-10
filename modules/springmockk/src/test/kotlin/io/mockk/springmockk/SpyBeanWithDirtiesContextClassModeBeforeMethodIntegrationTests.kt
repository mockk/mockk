package io.mockk.springmockk

import io.mockk.springmockk.example.ExampleServiceCaller
import io.mockk.springmockk.example.SimpleExampleService
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.ClassMode
import org.springframework.test.context.junit.jupiter.SpringExtension


/**
 * Integration tests for using [SpykBean] with [DirtiesContext] and
 * [ClassMode.BEFORE_EACH_TEST_METHOD].
 *
 * @author Andy Wilkinson
 * @author JB Nizet
 */
@ExtendWith(SpringExtension::class)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
class SpyBeanWithDirtiesContextClassModeBeforeMethodIntegrationTests {

    @SpykBean
    private lateinit var exampleService: SimpleExampleService

    @Autowired
    private lateinit var caller: ExampleServiceCaller

    @Test
    @Throws(Exception::class)
    fun testSpying() {
        this.caller.sayGreeting()
        verify { exampleService.greeting() }
    }

    @Configuration
    @Import(ExampleServiceCaller::class)
    internal class Config

}
