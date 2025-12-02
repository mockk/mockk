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

import com.ninjasquad.springmockk.MockkAssertions.assertIsSpy
import com.ninjasquad.springmockk.MockkSpyBean
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

/**
 * Integration tests for [@MockkSpyBeans][MockkSpyBeans] and
 * [@MockkSpyBean][MockkSpyBean] declared "by type" at the class level,
 * as a repeatable annotation, and via a custom composed annotation.
 *
 * @author Sam Brannen
 * @since 6.2.3
 * @see [gh-34408](https://github.com/spring-projects/spring-framework/issues/34408)
 *
 * @see MockkSpyBeansByNameIntegrationTests
 */
@SpringJUnitConfig
@MockkSpyBean(types = [Service04::class, Service05::class])
@SharedSpies // Intentionally declared between local @MockkSpyBean declarations
@MockkSpyBean(types = [Service06::class])
class MockkSpyBeansByTypeIntegrationTests : SpyTestInterface01 {
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

    @MockkSpyBean
    lateinit var service07: Service07


    @BeforeEach
    fun configureSpies() {
        every { service01.greeting() } returns "spy 01"
        every { service02.greeting() } returns "spy 02"
        every { service03.greeting() } returns "spy 03"
        every { service04.greeting() } returns "spy 04"
        every { service05.greeting() } returns "spy 05"
        every { service06.greeting() } returns "spy 06"
        every { service07.greeting() } returns "spy 07"
    }

    @Test
    fun checkSpies() {
        assertIsSpy(service01, "service01")
        assertIsSpy(service02, "service02")
        assertIsSpy(service03, "service03")
        assertIsSpy(service04, "service04")
        assertIsSpy(service05, "service05")
        assertIsSpy(service06, "service06")
        assertIsSpy(service07, "service07")

        assertThat(service01.greeting()).isEqualTo("spy 01")
        assertThat(service02.greeting()).isEqualTo("spy 02")
        assertThat(service03.greeting()).isEqualTo("spy 03")
        assertThat(service04.greeting()).isEqualTo("spy 04")
        assertThat(service05.greeting()).isEqualTo("spy 05")
        assertThat(service06.greeting()).isEqualTo("spy 06")
        assertThat(service07.greeting()).isEqualTo("spy 07")
    }


    @MockkSpyBean(types = [Service09::class])
    open inner class BaseTestCase : SpyTestInterface08 {
        @Autowired
        lateinit var service08: Service08

        @Autowired
        lateinit var service09: Service09

        @MockkSpyBean
        lateinit var service10: Service10
    }

    @Nested
    @MockkSpyBean(types = [Service12::class])
    inner class NestedTests : BaseTestCase(), SpyTestInterface11 {
        @Autowired
        lateinit var service11: Service11

        @Autowired
        lateinit var service12: Service12

        @MockkSpyBean
        lateinit var service13: Service13


        @BeforeEach
        fun configureSpies() {
            every { service08.greeting() } returns "spy 08"
            every { service09.greeting() } returns "spy 09"
            every { service10.greeting() } returns "spy 10"
            every { service11.greeting() } returns "spy 11"
            every { service12.greeting() } returns "spy 12"
            every { service13.greeting() } returns "spy 13"
        }

        @Test
        fun checkSpies() {
            assertIsSpy(service01, "service01")
            assertIsSpy(service02, "service02")
            assertIsSpy(service03, "service03")
            assertIsSpy(service04, "service04")
            assertIsSpy(service05, "service05")
            assertIsSpy(service06, "service06")
            assertIsSpy(service07, "service07")
            assertIsSpy(service08, "service08")
            assertIsSpy(service09, "service09")
            assertIsSpy(service10, "service10")
            assertIsSpy(service11, "service11")
            assertIsSpy(service12, "service12")
            assertIsSpy(service13, "service13")

            assertThat(service01.greeting()).isEqualTo("spy 01")
            assertThat(service02.greeting()).isEqualTo("spy 02")
            assertThat(service03.greeting()).isEqualTo("spy 03")
            assertThat(service04.greeting()).isEqualTo("spy 04")
            assertThat(service05.greeting()).isEqualTo("spy 05")
            assertThat(service06.greeting()).isEqualTo("spy 06")
            assertThat(service07.greeting()).isEqualTo("spy 07")
            assertThat(service08.greeting()).isEqualTo("spy 08")
            assertThat(service09.greeting()).isEqualTo("spy 09")
            assertThat(service10.greeting()).isEqualTo("spy 10")
            assertThat(service11.greeting()).isEqualTo("spy 11")
            assertThat(service12.greeting()).isEqualTo("spy 12")
            assertThat(service13.greeting()).isEqualTo("spy 13")
        }
    }


    @Configuration
    class Config {
        @Bean
        fun service01(): Service01 {
            return object : Service01 {
                override fun greeting(): String {
                    return "prod 1"
                }
            }
        }

        @Bean
        fun service02(): Service02 {
            return object : Service02 {
                override fun greeting(): String {
                    return "prod 2"
                }
            }
        }

        @Bean
        fun service03(): Service03 {
            return object : Service03 {
                override fun greeting(): String {
                    return "prod 3"
                }
            }
        }

        @Bean
        fun service04(): Service04 {
            return object : Service04 {
                override fun greeting(): String {
                    return "prod 4"
                }
            }
        }

        @Bean
        fun service05(): Service05 {
            return object : Service05 {
                override fun greeting(): String {
                    return "prod 5"
                }
            }
        }

        @Bean
        fun service06(): Service06 {
            return object : Service06 {
                override fun greeting(): String {
                    return "prod 6"
                }
            }
        }

        @Bean
        fun service07(): Service07 {
            return object : Service07 {
                override fun greeting(): String {
                    return "prod 7"
                }
            }
        }

        @Bean
        fun service08(): Service08 {
            return object : Service08 {
                override fun greeting(): String {
                    return "prod 8"
                }
            }
        }

        @Bean
        fun service09(): Service09 {
            return object : Service09 {
                override fun greeting(): String {
                    return "prod 9"
                }
            }
        }

        @Bean
        fun service10(): Service10 {
            return object : Service10 {
                override fun greeting(): String {
                    return "prod 10"
                }
            }
        }

        @Bean
        fun service11(): Service11 {
            return object : Service11 {
                override fun greeting(): String {
                    return "prod 11"
                }
            }
        }

        @Bean
        fun service12(): Service12 {
            return object : Service12 {
                override fun greeting(): String {
                    return "prod 12"
                }
            }
        }

        @Bean
        fun service13(): Service13 {
            return object : Service13 {
                override fun greeting(): String {
                    return "prod 13"
                }
            }
        }
    }
}
