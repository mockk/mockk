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

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.assertj.core.api.ThrowableAssert
import org.assertj.core.api.ThrowingConsumer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.test.context.bean.override.BeanOverrideHandler
import org.springframework.util.ReflectionUtils
import java.lang.reflect.Field
import java.util.function.Consumer

/**
 * Tests for [MockkBeanOverrideProcessor].
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.2
 */
class MockkBeanOverrideProcessorTests {
    private val processor = MockkBeanOverrideProcessor()


    @Nested
    inner class CreateHandlerTests {
        private val field = ReflectionUtils.findField(TestCase::class.java, "number")!!

        @Test
        fun mockAnnotationCreatesMockkBeanOverrideHandler() {
            val annotation = AnnotationUtils.synthesizeAnnotation(MockkBean::class.java)
            val obj = processor.createHandler(annotation, TestCase::class.java, field)

            assertThat(obj).isExactlyInstanceOf(MockkBeanOverrideHandler::class.java)
        }

        @Test
        fun spyAnnotationCreatesMockkSpyBeanOverrideHandler() {
            val annotation = AnnotationUtils.synthesizeAnnotation<MockkSpyBean>(MockkSpyBean::class.java)
            val obj = processor.createHandler(annotation, TestCase::class.java, field)

            assertThat(obj).isExactlyInstanceOf(MockkSpyBeanOverrideHandler::class.java)
        }

        @Test
        fun otherAnnotationThrows() {
            val annotation: Annotation = field.getAnnotation(MockkBeanOverrideProcessorTests.Nullable::class.java)

            assertThatIllegalStateException()
                .isThrownBy {
                    processor.createHandler(
                        annotation,
                        TestCase::class.java,
                        field
                    )
                }
                .withMessage(
                    "Invalid annotation passed to MockkBeanOverrideProcessor: expected either " +
                            "@MockkBean or @MockkSpyBean on field %s.%s", field.getDeclaringClass().getName(),
                    field.getName()
                )
        }

        @Test
        fun typesNotSupportedAtFieldLevel() {
            val field = ReflectionUtils.findField(TestCase::class.java, "typesNotSupported")
            val annotation = field!!.getAnnotation(MockkBean::class.java)

            assertThatIllegalStateException()
                .isThrownBy {
                    processor.createHandler(
                        annotation,
                        TestCase::class.java,
                        field
                    )
                }
                .withMessage("The @MockkBean 'types' attribute must be omitted when declared on a field")
        }
    }

    @Target(AnnotationTarget.FIELD)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Nullable

    class TestCase {
        @MockkBean
        @MockkSpyBean
        @MockkBeanOverrideProcessorTests.Nullable
        var number: Int? = null

        @MockkBean(types = [Int::class])
        lateinit var typesNotSupported: String
    }

    @MockkBean(name = "bogus", types = [Int::class])
    class NameNotSupportedTestCase

    @Nested
    inner class CreateHandlersTests {
        @Test
        fun otherAnnotationThrows() {
            val annotation: Annotation = javaClass.getAnnotation<Nested>(Nested::class.java)

            assertThatIllegalStateException()
                .isThrownBy { processor.createHandlers(annotation, javaClass) }
                .withMessage(
                    "Invalid annotation passed to MockkBeanOverrideProcessor: expected either " +
                            "@MockkBean or @MockkSpyBean on test class %s", javaClass.getName()
                )
        }

        @Nested
        inner class MockkBeanTests {
            @Test
            fun missingTypes() {
                val testClass: Class<*> = MockkBeanTestsClasses.MissingTypesTestCase::class.java
                val annotation = testClass.getAnnotation<MockkBean>(MockkBean::class.java)

                assertThatIllegalStateException()
                    .isThrownBy { processor.createHandlers(annotation, testClass) }
                    .withMessage("The @MockkBean 'types' attribute must not be empty when declared on a class")
            }

            @Test
            fun nameNotSupportedWithMultipleTypes() {
                val testClass: Class<*> = MockkBeanTestsClasses.NameNotSupportedWithMultipleTypesTestCase::class.java
                val annotation = testClass.getAnnotation<MockkBean>(MockkBean::class.java)

                assertThatIllegalStateException()
                    .isThrownBy { processor.createHandlers(annotation, testClass) }
                    .withMessage("The @MockkBean 'name' attribute cannot be used when mocking multiple types")
            }

            @Test
            fun singleMockByType() {
                val testClass: Class<*> = MockkBeanTestsClasses.SingleMockByTypeTestCase::class.java
                val annotation = testClass.getAnnotation<MockkBean>(MockkBean::class.java)
                val handlers = processor.createHandlers(annotation, testClass)

                assertThat(handlers).singleElement()
                    .isInstanceOfSatisfying(
                        MockkBeanOverrideHandler::class.java,
                        Consumer { handler ->
                            assertThat(handler.getField()).isNull()
                            assertThat(handler.getBeanName()).isNull()
                            assertThat(handler.getBeanType().resolve()).isEqualTo(Int::class.java)
                        })
            }

            @Test
            fun singleMockByName() {
                val testClass: Class<*> = MockkBeanTestsClasses.SingleMockByNameTestCase::class.java
                val annotation = testClass.getAnnotation<MockkBean>(MockkBean::class.java)
                val handlers = processor.createHandlers(annotation, testClass)

                assertThat(handlers).singleElement()
                    .isInstanceOfSatisfying(
                        MockkBeanOverrideHandler::class.java,
                        Consumer { handler ->
                            assertThat(handler.getField()).isNull()
                            assertThat(handler.getBeanName()).isEqualTo("enigma")
                            assertThat(handler.getBeanType().resolve()).isEqualTo(Int::class.java)
                        })
            }

            @Test
            fun multipleMocks() {
                val testClass: Class<*> = MockkBeanTestsClasses.MultipleMocksTestCase::class.java
                val annotation = testClass.getAnnotation<MockkBean>(MockkBean::class.java)
                val handlers = processor.createHandlers(annotation, testClass)

                assertThat(handlers).satisfiesExactly(
                    { handler1 ->
                        assertThat(handler1.getField()).isNull()
                        assertThat(handler1.getBeanName()).isNull()
                        assertThat(handler1.getBeanType().resolve()).isEqualTo(Int::class.java)
                    },
                     { handler2 ->
                        assertThat(handler2.getField()).isNull()
                        assertThat(handler2.getBeanName()).isNull()
                        assertThat(handler2.getBeanType().resolve()).isEqualTo(Float::class.java)
                    }
                )
            }
        }

        @Nested
        inner class MockkSpyBeanTests {
            @Test
            fun missingTypes() {
                val testClass: Class<*> = MockkSpyBeanTestsClasses.MissingTypesTestCase::class.java
                val annotation = testClass.getAnnotation<MockkSpyBean>(MockkSpyBean::class.java)

                assertThatIllegalStateException()
                    .isThrownBy { processor.createHandlers(annotation, testClass) }
                    .withMessage("The @MockkSpyBean 'types' attribute must not be empty when declared on a class")
            }

            @Test
            fun nameNotSupportedWithMultipleTypes() {
                val testClass: Class<*> = MockkSpyBeanTestsClasses.NameNotSupportedWithMultipleTypesTestCase::class.java
                val annotation = testClass.getAnnotation<MockkSpyBean>(MockkSpyBean::class.java)

                assertThatIllegalStateException()
                    .isThrownBy { processor.createHandlers(annotation, testClass) }
                    .withMessage("The @MockkSpyBean 'name' attribute cannot be used when mocking multiple types")
            }

            @Test
            fun singleSpyByType() {
                val testClass: Class<*> = MockkSpyBeanTestsClasses.SingleSpyByTypeTestCase::class.java
                val annotation = testClass.getAnnotation<MockkSpyBean>(MockkSpyBean::class.java)
                val handlers = processor.createHandlers(annotation, testClass)

                assertThat(handlers).singleElement()
                    .isInstanceOfSatisfying(
                        MockkSpyBeanOverrideHandler::class.java)
                        { handler ->
                            assertThat(handler.getField()).isNull()
                            assertThat(handler.getBeanName()).isNull()
                            assertThat(handler.getBeanType().resolve()).isEqualTo(Int::class.java)
                        }
            }

            @Test
            fun singleSpyByName() {
                val testClass: Class<*> = MockkSpyBeanTestsClasses.SingleSpyByNameTestCase::class.java
                val annotation = testClass.getAnnotation<MockkSpyBean>(MockkSpyBean::class.java)
                val handlers = processor.createHandlers(annotation, testClass)

                assertThat(handlers).singleElement()
                    .isInstanceOfSatisfying(MockkSpyBeanOverrideHandler::class.java)
                        { handler ->
                            assertThat(handler.getField()).isNull()
                            assertThat(handler.getBeanName()).isEqualTo("enigma")
                            assertThat(handler.getBeanType().resolve()).isEqualTo(Int::class.java)
                        }
            }

            @Test
            fun multipleSpies() {
                val testClass= MockkSpyBeanTestsClasses.MultipleSpiesTestCase::class.java
                val annotation = testClass.getAnnotation(MockkSpyBean::class.java)
                val handlers = processor.createHandlers(annotation, testClass)

                assertThat(handlers).satisfiesExactly(
                    { handler1 ->
                        assertThat(handler1.getField()).isNull()
                        assertThat(handler1.getBeanName()).isNull()
                        assertThat(handler1.getBeanType().resolve()).isEqualTo(Int::class.java)
                    },
                    { handler2 ->
                        assertThat(handler2.getField()).isNull()
                        assertThat(handler2.getBeanName()).isNull()
                        assertThat(handler2.getBeanType().resolve()).isEqualTo(Float::class.java)
                    }
                )
            }


        }
    }

    class MockkBeanTestsClasses {
        @MockkBean
        class MissingTypesTestCase

        @MockkBean(name = "bogus", types = [Int::class, Float::class])
        class NameNotSupportedWithMultipleTypesTestCase

        @MockkBean(types = [Int::class])
        class SingleMockByTypeTestCase

        @MockkBean(name = "enigma", types = [Int::class])
        class SingleMockByNameTestCase

        @MockkBean(types = [Int::class, Float::class])
        class MultipleMocksTestCase
    }

    class MockkSpyBeanTestsClasses {
        @MockkSpyBean
        class MissingTypesTestCase

        @MockkSpyBean(name = "bogus", types = [Int::class, Float::class])
        class NameNotSupportedWithMultipleTypesTestCase

        @MockkSpyBean(types = [Int::class])
        class SingleSpyByTypeTestCase

        @MockkSpyBean(name = "enigma", types = [Int::class])
        class SingleSpyByNameTestCase

        @MockkSpyBean(types = [Int::class, Float::class])
        class MultipleSpiesTestCase
    }
}
