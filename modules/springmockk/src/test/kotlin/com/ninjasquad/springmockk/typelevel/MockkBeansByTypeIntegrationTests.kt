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
package com.ninjasquad.springmockk.typelevel

import com.ninjasquad.springmockk.MockkAssertions.assertIsMock
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

/**
 * Integration tests for [@MockkBeans][MockkBeans] and
 * [@MockkBean][MockkBean] declared "by type" at the class level, as a
 * repeatable annotation, and via a custom composed annotation.
 *
 * @author Sam Brannen
 * @since 6.2.2
 * @see [gh-33925](https://github.com/spring-projects/spring-framework/issues/33925)
 *
 * @see MockkBeansByNameIntegrationTests
 */
@SpringJUnitConfig
@MockkBean(types = [Service04::class, Service05::class])
@SharedMocks // Intentionally declared between local @MockkBean declarations
@MockkBean(types = [Service06::class])
class MockkBeansByTypeIntegrationTests : MockTestInterface01 {
    @Autowired
    lateinit var service01: Service01

    @Autowired
    lateinit var service02: Service02

    @Autowired
    lateinit var service03: Service03

    @Autowired
    lateinit var service04: Service04

    @Autowired
    lateinit var service05: Service05

    @Autowired
    lateinit var service06: Service06

    @MockkBean
    lateinit var service07: Service07


    @BeforeEach
    fun configureMocks() {
        every { service01.greeting() } returns "mock 01"
        every { service02.greeting() } returns "mock 02"
        every { service03.greeting() } returns "mock 03"
        every { service04.greeting() } returns "mock 04"
        every { service05.greeting() } returns "mock 05"
        every { service06.greeting() } returns "mock 06"
        every { service07.greeting() } returns "mock 07"
    }

    @Test
    fun checkMocks() {
        assertIsMock(service01, "service01")
        assertIsMock(service02, "service02")
        assertIsMock(service03, "service03")
        assertIsMock(service04, "service04")
        assertIsMock(service05, "service05")
        assertIsMock(service06, "service06")
        assertIsMock(service07, "service07")

        assertThat(service01.greeting()).isEqualTo("mock 01")
        assertThat(service02.greeting()).isEqualTo("mock 02")
        assertThat(service03.greeting()).isEqualTo("mock 03")
        assertThat(service04.greeting()).isEqualTo("mock 04")
        assertThat(service05.greeting()).isEqualTo("mock 05")
        assertThat(service06.greeting()).isEqualTo("mock 06")
        assertThat(service07.greeting()).isEqualTo("mock 07")
    }


    @MockkBean(types = [Service09::class])
    open inner class BaseTestCase : MockTestInterface08 {
        @Autowired
        lateinit var service08: Service08

        @Autowired
        lateinit var service09: Service09

        @MockkBean
        lateinit var service10: Service10
    }

    @Nested
    @MockkBean(types = [Service12::class])
    inner class NestedTests : BaseTestCase(), MockTestInterface11 {
        @Autowired
        lateinit var service11: Service11

        @Autowired
        lateinit var service12: Service12

        @MockkBean
        lateinit var service13: Service13


        @BeforeEach
        fun configureMocks() {
            every { service08.greeting() } returns "mock 08"
            every { service09.greeting() } returns "mock 09"
            every { service10.greeting() } returns "mock 10"
            every { service11.greeting() } returns "mock 11"
            every { service12.greeting() } returns "mock 12"
            every { service13.greeting() } returns "mock 13"
        }

        @Test
        fun checkMocks() {
            assertIsMock(service01, "service01")
            assertIsMock(service02, "service02")
            assertIsMock(service03, "service03")
            assertIsMock(service04, "service04")
            assertIsMock(service05, "service05")
            assertIsMock(service06, "service06")
            assertIsMock(service07, "service07")
            assertIsMock(service08, "service08")
            assertIsMock(service09, "service09")
            assertIsMock(service10, "service10")
            assertIsMock(service11, "service11")
            assertIsMock(service12, "service12")
            assertIsMock(service13, "service13")

            assertThat(service01.greeting()).isEqualTo("mock 01")
            assertThat(service02.greeting()).isEqualTo("mock 02")
            assertThat(service03.greeting()).isEqualTo("mock 03")
            assertThat(service04.greeting()).isEqualTo("mock 04")
            assertThat(service05.greeting()).isEqualTo("mock 05")
            assertThat(service06.greeting()).isEqualTo("mock 06")
            assertThat(service07.greeting()).isEqualTo("mock 07")
            assertThat(service08.greeting()).isEqualTo("mock 08")
            assertThat(service09.greeting()).isEqualTo("mock 09")
            assertThat(service10.greeting()).isEqualTo("mock 10")
            assertThat(service11.greeting()).isEqualTo("mock 11")
            assertThat(service12.greeting()).isEqualTo("mock 12")
            assertThat(service13.greeting()).isEqualTo("mock 13")
        }
    }
}
