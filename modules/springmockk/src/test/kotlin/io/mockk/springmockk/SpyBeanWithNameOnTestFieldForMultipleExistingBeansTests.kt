package io.mockk.springmockk

import io.mockk.springmockk.example.SimpleExampleStringGenericService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit.jupiter.SpringExtension


/**
 * Test [SpykBean] on a test class field can be used to inject a spy instance when
 * there are multiple candidates and one is chosen using the name attribute.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author JB Nizet
 */
@ExtendWith(SpringExtension::class)
class SpyBeanWithNameOnTestFieldForMultipleExistingBeansTests {

    @SpykBean(name = "two")
    private lateinit var spy: SimpleExampleStringGenericService

    @Test
    fun testSpying() {
        assertThat(spy.isMock).isTrue()
        assertThat(spy.toString()).contains("two")
    }

    @Configuration
    internal class Config {

        @Bean
        fun one(): SimpleExampleStringGenericService {
            return SimpleExampleStringGenericService("one")
        }

        @Bean
        fun two(): SimpleExampleStringGenericService {
            return SimpleExampleStringGenericService("two")
        }

    }

}
