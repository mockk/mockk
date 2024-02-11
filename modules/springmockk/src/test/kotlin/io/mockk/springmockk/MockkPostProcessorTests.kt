package io.mockk.springmockk

import io.mockk.springmockk.example.ExampleService
import io.mockk.springmockk.example.FailingExampleService
import io.mockk.springmockk.example.RealExampleService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.getBean
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.boot.test.mock.mockito.MockitoPostProcessor
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.util.Assert


/**
 * Test for [MockkPostProcessor]. See also the integration tests.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Andreas Neiser
 * @author JB Nizet
 */
class MockkPostProcessorTests {

    @Test
    fun cannotMockMultipleBeans() {
        val context = AnnotationConfigApplicationContext()
        MockkPostProcessor.register(context)
        context.register(MultipleBeans::class.java)
        assertThatIllegalStateException().isThrownBy { context.refresh() }.withMessageContaining(
            "Unable to register mock bean " + ExampleService::class.java.name
                + " expected a single matching bean to replace "
                + "but found [example1, example2]"
        )
    }

    @Test
    fun cannotMockMultipleQualifiedBeans() {
        val context = AnnotationConfigApplicationContext()
        MockkPostProcessor.register(context)
        context.register(MultipleQualifiedBeans::class.java)
        assertThatIllegalStateException().isThrownBy { context.refresh() }
            .withMessageContaining(
                ("Unable to register mock bean " + ExampleService::class.java.name
                    + " expected a single matching bean to replace "
                    + "but found [example1, example3]")
            )
    }

    @Test
    fun canMockBeanProducedByFactoryBeanWithObjectTypeAttribute() {
        val context = AnnotationConfigApplicationContext()
        MockkPostProcessor.register(context)
        val factoryBeanDefinition = RootBeanDefinition(TestFactoryBean::class.java)
        factoryBeanDefinition.setAttribute(
            FactoryBean.OBJECT_TYPE_ATTRIBUTE,
            SomeInterface::class.java.name
        )
        context.registerBeanDefinition("beanToBeMocked", factoryBeanDefinition)
        context.register(MockedFactoryBean::class.java)
        context.refresh()
        assertThat(context.getBean("beanToBeMocked").isMock).isTrue()
    }

    @Test
    fun canMockPrimaryBean() {
        val context = AnnotationConfigApplicationContext()
        MockkPostProcessor.register(context)
        context.register(MockPrimaryBean::class.java)
        context.refresh()
        assertThat(context.getBean<MockPrimaryBean>().mock.isMock).isTrue()
        assertThat(context.getBean<ExampleService>().isMock).isTrue()
        assertThat(context.getBean<ExampleService>("examplePrimary").isMock).isTrue()
        assertThat(context.getBean<ExampleService>("exampleQualified").isMock).isFalse()
    }

    @Test
    fun canMockQualifiedBeanWithPrimaryBeanPresent() {
        val context = AnnotationConfigApplicationContext()
        MockkPostProcessor.register(context)
        context.register(MockQualifiedBean::class.java)
        context.refresh()
        assertThat(context.getBean<MockQualifiedBean>().mock.isMock).isTrue()
        assertThat(context.getBean<ExampleService>().isMock).isFalse()
        assertThat(context.getBean<ExampleService>("examplePrimary").isMock).isFalse()
        assertThat(context.getBean<ExampleService>("exampleQualified").isMock).isTrue()
    }

    @Test
    fun canSpyPrimaryBean() {
        val context = AnnotationConfigApplicationContext()
        MockkPostProcessor.register(context)
        context.register(SpyPrimaryBean::class.java)
        context.refresh()
        assertThat(context.getBean<SpyPrimaryBean>().spy.isMock).isTrue()
        assertThat(context.getBean<ExampleService>().isMock).isTrue()
        assertThat(context.getBean<ExampleService>("examplePrimary").isMock).isTrue()
        assertThat(context.getBean<ExampleService>("exampleQualified").isMock).isFalse()
    }

    @Test
    fun canSpyQualifiedBeanWithPrimaryBeanPresent() {
        val context = AnnotationConfigApplicationContext()
        MockkPostProcessor.register(context)
        context.register(SpyQualifiedBean::class.java)
        context.refresh()
        assertThat(context.getBean<SpyQualifiedBean>().spy.isMock).isTrue()
        assertThat(context.getBean<ExampleService>().isMock).isFalse()
        assertThat(context.getBean<ExampleService>("examplePrimary").isMock).isFalse()
        assertThat(context.getBean<ExampleService>("exampleQualified").isMock).isTrue()
    }

    @Test
    fun postProcessorShouldNotTriggerEarlyInitialization() {
        val context = AnnotationConfigApplicationContext()
        context.register(FactoryBeanRegisteringPostProcessor::class.java)
        MockitoPostProcessor.register(context)
        context.register(TestBeanFactoryPostProcessor::class.java)
        context.register(EagerInitBean::class.java)
        context.refresh()
    }

    @Configuration
    @MockkBean(SomeInterface::class)
    internal class MockedFactoryBean {

        @Bean
        fun testFactoryBean(): TestFactoryBean {
            return TestFactoryBean()
        }

    }

    @Configuration
    @MockkBean(ExampleService::class)
    internal class MultipleBeans {

        @Bean
        fun example1(): ExampleService {
            return FailingExampleService()
        }

        @Bean
        fun example2(): ExampleService {
            return FailingExampleService()
        }

    }

    @Configuration
    internal class MultipleQualifiedBeans {

        @MockkBean
        @Qualifier("test")
        lateinit var mock: ExampleService

        @Bean
        @Qualifier("test")
        fun example1(): ExampleService {
            return FailingExampleService()
        }

        @Bean
        fun example2(): ExampleService {
            return FailingExampleService()
        }

        @Bean
        @Qualifier("test")
        fun example3(): ExampleService {
            return FailingExampleService()
        }

    }

    @Configuration
    internal class MockPrimaryBean {

        @MockkBean
        lateinit var mock: ExampleService

        @Bean
        @Qualifier("test")
        fun exampleQualified(): ExampleService {
            return RealExampleService("qualified")
        }

        @Bean
        @Primary
        fun examplePrimary(): ExampleService {
            return RealExampleService("primary")
        }

    }

    @Configuration
    internal class MockQualifiedBean {

        @MockkBean
        @Qualifier("test")
        lateinit var mock: ExampleService

        @Bean
        @Qualifier("test")
        fun exampleQualified(): ExampleService {
            return RealExampleService("qualified")
        }

        @Bean
        @Primary
        fun examplePrimary(): ExampleService {
            return RealExampleService("primary")
        }

    }

    @Configuration
    internal class SpyPrimaryBean {

        @SpykBean
        lateinit var spy: ExampleService

        @Bean
        @Qualifier("test")
        fun exampleQualified(): ExampleService {
            return RealExampleService("qualified")
        }

        @Bean
        @Primary
        fun examplePrimary(): ExampleService {
            return RealExampleService("primary")
        }

    }

    @Configuration
    internal class SpyQualifiedBean {

        @SpykBean
        @Qualifier("test")
        lateinit var spy: ExampleService

        @Bean
        @Qualifier("test")
        fun exampleQualified(): ExampleService {
            return RealExampleService("qualified")
        }

        @Bean
        @Primary
        fun examplePrimary(): ExampleService {
            return RealExampleService("primary")
        }

    }

    @Configuration(proxyBeanMethods = false)
    internal class EagerInitBean {

        @MockkBean
        lateinit var service: ExampleService

    }

    internal class TestFactoryBean : FactoryBean<Any> {

        override fun getObject(): Any {
            return TestBean()
        }

        override fun getObjectType(): Class<*>? {
            return null
        }

        override fun isSingleton(): Boolean {
            return true
        }

    }

    internal class FactoryBeanRegisteringPostProcessor : BeanFactoryPostProcessor, Ordered {

        override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
            val beanDefinition = RootBeanDefinition(TestFactoryBean::class.java)
            (beanFactory as BeanDefinitionRegistry).registerBeanDefinition("test", beanDefinition)
        }

        override fun getOrder(): Int {
            return Ordered.HIGHEST_PRECEDENCE
        }
    }

    internal class TestBeanFactoryPostProcessor : BeanFactoryPostProcessor {

        override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
            val cache = ReflectionTestUtils.getField(
                beanFactory,
                "factoryBeanInstanceCache"
            ) as Map<*, *>
            Assert.isTrue(cache.isEmpty(), "Early initialization of factory bean triggered.")
        }
    }

    internal interface SomeInterface

    internal class TestBean : SomeInterface
}
