package io.mockk.springmockk

import io.mockk.springmockk.SpyBeanOnTestFieldForExistingCircularBeansIntegrationTests.SpyBeanOnTestFieldForExistingCircularBeansConfig
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension


/**
 * Test [@SpykBean][SpykBean] on a test class field can be used to replace existing
 * beans with circular dependencies.
 *
 * @author Andy Wilkinson
 * @author JB Nizet
 */
@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [SpyBeanOnTestFieldForExistingCircularBeansConfig::class])
internal class SpyBeanOnTestFieldForExistingCircularBeansIntegrationTests {
    @SpykBean
    private lateinit var one: One

    @Autowired
    private lateinit var two: Two

    @Test
    fun beanWithCircularDependenciesCanBeSpied() {
        two.callOne()

        verify { one.someMethod() }
    }

    @Import(One::class, Two::class)
    internal class SpyBeanOnTestFieldForExistingCircularBeansConfig

    internal class One {
        @Autowired
        private lateinit var two: Two

        fun someMethod() {}
    }

    internal class Two {
        @Autowired
        private lateinit var one: One

        fun callOne() {
            one.someMethod()
        }
    }
}
