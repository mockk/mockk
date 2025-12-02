package com.ninjasquad.springmockk

import com.ninjasquad.springmockk.MockBeanOnContextHierarchyIntegrationTests.ParentConfig
import com.ninjasquad.springmockk.SpyBeanOnContextHierarchyIntegrationTests.ChildConfig
import com.ninjasquad.springmockk.example.ExampleService
import com.ninjasquad.springmockk.example.ExampleServiceCaller
import com.ninjasquad.springmockk.example.SimpleExampleService
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
 * Test [SpykBean] can be used with a [ContextHierarchy].
 *
 * @author Phillip Webb
 * @author JB Nizet
 */
@ExtendWith(SpringExtension::class)
@ContextHierarchy(
    ContextConfiguration(classes = [ParentConfig::class]),
    ContextConfiguration(classes = [ChildConfig::class])
)
class SpyBeanOnContextHierarchyIntegrationTests {

    @Autowired
    private lateinit var childConfig: ChildConfig

    @Test
    fun testSpying() {
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
    @SpykBean(SimpleExampleService::class)
    internal class ParentConfig

    @Configuration
    @SpykBean(ExampleServiceCaller::class)
    internal class ChildConfig : ApplicationContextAware {

        lateinit var context: ApplicationContext

        override fun setApplicationContext(applicationContext: ApplicationContext) {
            this.context = applicationContext
        }

    }

}
