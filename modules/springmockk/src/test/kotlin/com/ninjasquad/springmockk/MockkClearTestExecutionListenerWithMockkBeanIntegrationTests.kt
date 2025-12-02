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

import io.mockk.every
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Integration tests for [MockkResetTestExecutionListener] with a
 * [@MockkBean][MockkBean] field.
 *
 * @author Sam Brannen
 * @since 6.2
 * @see MockkClearTestExecutionListenerWithoutMockkAnnotationsIntegrationTests
 *
 * @see MockkClearStrategiesIntegrationTests
 */
class MockkClearTestExecutionListenerWithMockkBeanIntegrationTests
    : MockkClearTestExecutionListenerWithoutMockkAnnotationsIntegrationTests() {

    // We declare the following to ensure that MockkClear is also supported with
    // @MockkBean or @MockkSpyBean fields present in the test class.
    @MockkBean
    lateinit var puzzleService: PuzzleService


    // test001() and test002() are in the superclass.
    @Test
    fun test003() {
        every { puzzleService.answer } returns "enigma";
        assertThat(puzzleService.answer).isEqualTo("enigma")
    }


    interface PuzzleService {
        val answer: String?
    }
}
