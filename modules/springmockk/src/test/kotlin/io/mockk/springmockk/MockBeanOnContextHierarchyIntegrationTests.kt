package io.mockk.springmockk

import io.mockk.springmockk.MockBeanOnContextHierarchyIntegrationTests.ChildConfig
import io.mockk.springmockk.MockBeanOnContextHierarchyIntegrationTests.ParentConfig
import io.mockk.springmockk.example.ExampleService
import io.mockk.springmockk.example.ExampleServiceCaller
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.getBean
import org.springframework.beans.factory.getBeanNamesForType
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.ContextHierarchy
import org.springframework.test.context.junit.jupiter.SpringExtension


/**
 * Test [MockkBean] can be used with a [ContextHierarchy].
 *
 * @author Phillip Webb
 */
@ExtendWith(SpringExtension::class)
@ContextHierarchy(
    ContextConfiguration(classes = [ParentConfig::class]),
    ContextConfiguration(classes = [ChildConfig::class])
)
class MockBeanOnContextHierarchyIntegrationTests {

    @Autowired
    private lateinit var childConfig: ChildConfig

    @Test
    fun testMocking() {
        val context = this.childConfig.context
        val parentContext = context.parent!!
        assertThat(parentContext.getBeanNamesForType<ExampleService>()).hasSize(1)
        assertThat(parentContext.getBeanNamesForType<ExampleServiceCaller>()).hasSize(0)
        assertThat(context.getBeanNamesForType<ExampleService>()).hasSize(0)
        assertThat(context.getBeanNamesForType<ExampleServiceCaller>()).hasSize(1)
        assertThat(context.getBean<ExampleService>()).isNotNull()
        assertThat(context.getBean<ExampleServiceCaller>()).isNotNull()
    }

    @Configuration
    @MockkBean(ExampleService::class)
    internal class ParentConfig

    @Configuration
    @MockkBean(ExampleServiceCaller::class)
    internal class ChildConfig : ApplicationContextAware {

        lateinit var context: ApplicationContext

        override fun setApplicationContext(applicationContext: ApplicationContext) {
            this.context = applicationContext
        }

    }

}
