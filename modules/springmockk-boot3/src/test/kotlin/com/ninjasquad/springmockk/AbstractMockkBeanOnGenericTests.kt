package com.ninjasquad.springmockk
import com.ninjasquad.springmockk.AbstractMockkBeanOnGenericTests.Something
import com.ninjasquad.springmockk.AbstractMockkBeanOnGenericTests.TestConfiguration
import com.ninjasquad.springmockk.AbstractMockkBeanOnGenericTests.Thing
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


/**
 * Tests for [MockkBean] with abstract class and generics.
 *
 * @author Madhura Bhave
 * @author JB Nizet
 */
@SpringBootTest(classes = [TestConfiguration::class])
abstract class AbstractMockkBeanOnGenericTests<T : Thing<U>, U : Something> {

    @Autowired
    private lateinit var thing: T

    @MockkBean
    private lateinit var something: U

    @Test
    fun mockkBeanShouldResolveConcreteType() {
        assertThat(something).isInstanceOf(SomethingImpl::class.java)
    }

    abstract class Thing<T : Something> {
        @Autowired
        lateinit var something: T
    }

    class SomethingImpl : Something()

    class ThingImpl : Thing<SomethingImpl>()

    open class Something

    @Configuration
    class TestConfiguration {
        @Bean
        fun thing(): ThingImpl {
            return ThingImpl()
        }
    }
}
