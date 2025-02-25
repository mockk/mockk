package io.mockk.springmockk

import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component

/**
 * Tests for nested test configuration when the configuration is inherited from the
 * enclosing class (the default behaviour).
 *
 * @author Andy Wilkinson
 * @author JB Nizet
 */
@SpringBootTest(classes = [InheritedNestedTestConfigurationTests.AppConfiguration::class])
@Import(InheritedNestedTestConfigurationTests.ActionPerformer::class)
class InheritedNestedTestConfigurationTests {
    @MockkBean(relaxUnitFun = true)
    lateinit var action: Action

    @Autowired
    lateinit var performer: ActionPerformer

    @Test
    fun mockWasInvokedOnce() {
        this.performer.run()
        verify(exactly = 1) { action.perform() }
    }

    @Test
    fun mockWasInvokedTwice() {
        this.performer.run()
        this.performer.run()
        verify(exactly = 2) { action.perform() }
    }

    @Nested
    inner class InnerTests {

        @Test
        fun mockWasInvokedOnce() {
            performer.run()
            verify(exactly = 1) { action.perform() }
        }

        @Test
        fun mockWasInvokedTwice() {
            performer.run()
            performer.run()
            verify(exactly = 2) { action.perform() }
        }
    }

    @Component
    class ActionPerformer(private val action: Action) {
        fun run() {
            this.action.perform()
        }
    }

    interface Action {
        fun perform()
    }

    @SpringBootConfiguration
    class AppConfiguration
}
