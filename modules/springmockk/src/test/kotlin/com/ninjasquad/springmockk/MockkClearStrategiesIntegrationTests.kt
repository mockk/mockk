/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ninjasquad.springmockk

import com.ninjasquad.springmockk.MockkClearStrategiesIntegrationTests.MockVerificationExtension
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * Integration tests for [@MockkBean][MockkBean] fields with different
 * [MockReset] strategies.
 *
 * @author Sam Brannen
 * @since 6.2.1
 * @see MockkClearTestExecutionListenerWithoutMockkAnnotationsIntegrationTests
 * @see MockkClearTestExecutionListenerWithMockkBeanIntegrationTests
 */
// The MockVerificationExtension MUST be registered before the SpringExtension.
@ExtendWith(MockVerificationExtension::class)
@ExtendWith(SpringExtension::class)
@TestMethodOrder(MethodOrderer.MethodName::class)
class MockkClearStrategiesIntegrationTests {
    @MockkBean(name = "puzzleServiceNone", clear = MockkClear.NONE, relaxed = true)
    lateinit var puzzleServiceNone: PuzzleService

    @MockkBean(name = "puzzleServiceBefore", clear = MockkClear.BEFORE, relaxed = true)
    lateinit var puzzleServiceBefore: PuzzleService

    @MockkBean(name = "puzzleServiceAfter", clear = MockkClear.AFTER, relaxed = true)
    lateinit var puzzleServiceAfter: PuzzleService


    @AfterEach
    fun trackStaticReferences() {
        puzzleServiceNoneStaticReference = this.puzzleServiceNone
        puzzleServiceBeforeStaticReference = this.puzzleServiceBefore
        puzzleServiceAfterStaticReference = this.puzzleServiceAfter
    }

    @Test
    fun test001(testInfo: TestInfo) {
        assertThat(puzzleServiceNone.answer).isEmpty()
        assertThat(puzzleServiceBefore.answer).isEmpty()
        assertThat(puzzleServiceAfter.answer).isEmpty()

        stubAndTestMocks(testInfo)
    }

    @Test
    fun test002(testInfo: TestInfo) {
        // Should not have been reset.
        assertThat(puzzleServiceNone.answer).isEqualTo("none - test001")

        // Should have been reset.
        assertThat(puzzleServiceBefore.answer).isEmpty()
        assertThat(puzzleServiceAfter.answer).isEmpty()

        stubAndTestMocks(testInfo)
    }

    private fun stubAndTestMocks(testInfo: TestInfo) {
        val name = testInfo.getTestMethod().get().getName()
        every { puzzleServiceNone.answer } returns "none - " + name
        assertThat(puzzleServiceNone.answer).isEqualTo("none - " + name)

        every { puzzleServiceBefore.answer } returns "before - " + name
        assertThat(puzzleServiceBefore.answer).isEqualTo("before - " + name)

        every { puzzleServiceAfter.answer } returns "after - " + name
        assertThat(puzzleServiceAfter.answer).isEqualTo("after - " + name)
    }

    interface PuzzleService {
        val answer: String
    }

    class MockVerificationExtension : AfterEachCallback {
        override fun afterEach(context: ExtensionContext) {
            val name = context.getRequiredTestMethod().getName()

            // Should not have been reset.
            assertThat(puzzleServiceNoneStaticReference!!.answer).`as`("puzzleServiceNone")
                .isEqualTo("none - " + name)
            assertThat(puzzleServiceBeforeStaticReference!!.answer).`as`("puzzleServiceBefore")
                .isEqualTo("before - " + name)

            // Should have been reset.
            assertThat(puzzleServiceAfterStaticReference!!.answer).`as`("puzzleServiceAfter").isEmpty()
        }
    }

    companion object {
        var puzzleServiceNoneStaticReference: PuzzleService? = null
        var puzzleServiceBeforeStaticReference: PuzzleService? = null
        var puzzleServiceAfterStaticReference: PuzzleService? = null


        @JvmStatic
        @AfterAll
        fun releaseStaticReferences() {
            puzzleServiceNoneStaticReference = null
            puzzleServiceBeforeStaticReference = null
            puzzleServiceAfterStaticReference = null
        }
    }
}
