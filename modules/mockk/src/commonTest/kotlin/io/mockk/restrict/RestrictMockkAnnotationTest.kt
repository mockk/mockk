package io.mockk.restrict

import io.mockk.MockKException
import io.mockk.impl.restrict.MockkValidator
import io.mockk.impl.restrict.RestrictMockkConfiguration
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.lang.annotation.Inherited
import kotlin.test.assertEquals

class RestrictMockkAnnotationTest {
    @Test
    fun `when class carries restricted annotation should throw exception`() {
        val config =
            mapOf(
                "mockk.throwExceptionOnBadMock" to "true",
                "mockk.restrictedClassAnnotations" to "io.mockk.restrict.DoNotMock",
            )

        val testValidator =
            MockkValidator(
                RestrictMockkConfiguration(TestPropertiesLoader(config)),
            )

        assertThrows<MockKException> {
            mockk<AnnotatedFoo>(mockValidator = testValidator)
        }
    }

    @Test
    fun `when add custom restricted annotations should throw exception`() {
        val config =
            mapOf(
                "mockk.throwExceptionOnBadMock" to "true",
                "mockk.restrictedClassAnnotations" to "io.mockk.restrict.DoNotMock, io.mockk.restrict.DoNotMockEither",
            )

        val testValidator =
            MockkValidator(
                RestrictMockkConfiguration(TestPropertiesLoader(config)),
            )

        assertThrows<MockKException> {
            mockk<AnnotatedFoo>(mockValidator = testValidator)
        }

        assertThrows<MockKException> {
            mockk<AnnotatedBar>(mockValidator = testValidator)
        }
    }

    @Test
    fun `when restricted annotated interface should throw exception`() {
        val config =
            mapOf(
                "mockk.throwExceptionOnBadMock" to "true",
                "mockk.restrictedClassAnnotations" to "io.mockk.restrict.DoNotMock",
            )

        val testValidator =
            MockkValidator(
                RestrictMockkConfiguration(TestPropertiesLoader(config)),
            )

        assertThrows<MockKException> {
            mockk<AnnotatedProvider>(mockValidator = testValidator)
        }
    }

    @Test
    fun `when class does not carry restricted annotation should not throw exception`() {
        val config =
            mapOf(
                "mockk.throwExceptionOnBadMock" to "true",
                "mockk.restrictedClassAnnotations" to "io.mockk.restrict.DoNotMock",
            )

        val testValidator =
            MockkValidator(
                RestrictMockkConfiguration(TestPropertiesLoader(config)),
            )

        assertDoesNotThrow {
            mockk<PlainFoo>(mockValidator = testValidator)
        }
    }

    @Test
    fun `when restricted annotation is not inherited should not restrict subclass`() {
        val config =
            mapOf(
                "mockk.throwExceptionOnBadMock" to "true",
                "mockk.restrictedClassAnnotations" to "io.mockk.restrict.DoNotMock",
            )

        val testValidator =
            MockkValidator(
                RestrictMockkConfiguration(TestPropertiesLoader(config)),
            )

        assertDoesNotThrow {
            mockk<AnnotatedFooChild>(mockValidator = testValidator)
        }
    }

    @Test
    fun `when restricted annotation is inherited should restrict subclass`() {
        val config =
            mapOf(
                "mockk.throwExceptionOnBadMock" to "true",
                "mockk.restrictedClassAnnotations" to "io.mockk.restrict.InheritedDoNotMock",
            )

        val testValidator =
            MockkValidator(
                RestrictMockkConfiguration(TestPropertiesLoader(config)),
            )

        assertThrows<MockKException> {
            mockk<InheritedAnnotatedFoo>(mockValidator = testValidator)
        }

        assertThrows<MockKException> {
            mockk<InheritedAnnotatedFooChild>(mockValidator = testValidator)
        }
    }

    @Test
    fun `when throwExceptionOnBadMock is false should not throw exception for annotated class`() {
        val config =
            mapOf(
                "mockk.throwExceptionOnBadMock" to "false",
                "mockk.restrictedClassAnnotations" to "io.mockk.restrict.DoNotMock",
            )

        val testValidator =
            MockkValidator(
                RestrictMockkConfiguration(TestPropertiesLoader(config)),
            )

        assertDoesNotThrow {
            mockk<AnnotatedFoo>(mockValidator = testValidator)
        }
    }

    @Test
    fun `when restrictedClassAnnotations property is set should be parsed into configuration`() {
        val config =
            mapOf(
                "mockk.restrictedClassAnnotations" to
                    " io.mockk.restrict.DoNotMock ,, io.mockk.restrict.DoNotMockEither, ",
            )

        val configuration =
            RestrictMockkConfiguration(TestPropertiesLoader(config))

        assertEquals(
            setOf("io.mockk.restrict.DoNotMock", "io.mockk.restrict.DoNotMockEither"),
            configuration.restrictedAnnotations,
        )
    }

    @Test
    fun `when restrictedClassAnnotations property is missing should default to empty set`() {
        val configuration = RestrictMockkConfiguration(TestPropertiesLoader(emptyMap()))

        assertEquals(emptySet(), configuration.restrictedAnnotations)
    }

    @Test
    fun `when restricted annotation class cannot be resolved should not throw exception`() {
        val config =
            mapOf(
                "mockk.throwExceptionOnBadMock" to "true",
                "mockk.restrictedClassAnnotations" to "io.mockk.restrict.DoesNotExist",
            )

        val testValidator =
            MockkValidator(
                RestrictMockkConfiguration(TestPropertiesLoader(config)),
            )

        assertDoesNotThrow {
            mockk<PlainFoo>(mockValidator = testValidator)
        }
    }

    @Test
    fun `when unresolvable annotation is listed next to a resolvable one should still restrict`() {
        val config =
            mapOf(
                "mockk.throwExceptionOnBadMock" to "true",
                "mockk.restrictedClassAnnotations" to
                    "io.mockk.restrict.DoesNotExist, io.mockk.restrict.DoNotMock",
            )

        val testValidator =
            MockkValidator(
                RestrictMockkConfiguration(TestPropertiesLoader(config)),
            )

        assertThrows<MockKException> {
            mockk<AnnotatedFoo>(mockValidator = testValidator)
        }

        assertDoesNotThrow {
            mockk<PlainFoo>(mockValidator = testValidator)
        }
    }
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class DoNotMock

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class DoNotMockEither

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Inherited
annotation class InheritedDoNotMock

@DoNotMock
open class AnnotatedFoo

@DoNotMockEither
class AnnotatedBar

class PlainFoo

class AnnotatedFooChild : AnnotatedFoo()

@InheritedDoNotMock
open class InheritedAnnotatedFoo

class InheritedAnnotatedFooChild : InheritedAnnotatedFoo()

@DoNotMock
interface AnnotatedProvider
