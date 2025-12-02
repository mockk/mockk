package com.ninjasquad.springmockk

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.TestContext
import org.springframework.test.context.support.AbstractTestExecutionListener
import org.springframework.util.ClassUtils


/**
 * `TestExecutionListener` that clears any mock beans that have been marked
 * with a [MockkClear].
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Jean-Baptiste Nizet
 * @see MockkBean
 * @see MockkSpyBean
 */
class MockkClearTestExecutionListener : AbstractTestExecutionListener() {
    /**
     * Returns [ORDER], which ensures that the
     * `MockkClearTestExecutionListener` is ordered after all standard
     * `TestExecutionListener` implementations.
     * @see ORDER
     */
    override fun getOrder(): Int {
        return ORDER
    }

    override fun beforeTestMethod(testContext: TestContext) {
        if (isEnabled) {
            clearMocks(testContext.getApplicationContext(), MockkClear.BEFORE)
        }
    }

    override fun afterTestMethod(testContext: TestContext) {
        if (isEnabled) {
            clearMocks(testContext.getApplicationContext(), MockkClear.AFTER)
        }
    }


    companion object {
        /**
         * The [order][getOrder] value for this listener
         * (`Ordered.LOWEST_PRECEDENCE - 100`).
         */
        private const val ORDER: Int = LOWEST_PRECEDENCE - 100

        private val logger: Log = LogFactory.getLog(MockkClearTestExecutionListener::class.java)

        /**
         * Boolean flag which tracks whether MockK is present in the classpath.
         * @see mockKInitialized
         *
         * @see isEnabled
         */
        private val MOCKK_PRESENT = ClassUtils.isPresent(
            "io.mockk.MockK",
            MockkClearTestExecutionListener::class.java.getClassLoader()
        )

        /**
         * Boolean flag which tracks whether MockK has been successfully initialized
         * in the current environment.
         *
         * Even if [MOCKK_PRESENT] evaluates to `true`, this flag
         * may eventually evaluate to `false`  for example, in a GraalVM
         * native image.
         * @see MOCKK_PRESENT
         * @see isEnabled
         */
        @Volatile
        private var mockKInitialized: Boolean? = null


        private fun clearMocks(applicationContext: ApplicationContext?, clear: MockkClear) {
            if (applicationContext is ConfigurableApplicationContext) {
                clearMocks(applicationContext, clear)
            }
        }

        private fun clearMocks(applicationContext: ConfigurableApplicationContext, clear: MockkClear) {
            val beanFactory = applicationContext.getBeanFactory()
            val beanNames = beanFactory.getBeanDefinitionNames()
            val instantiatedSingletons = beanFactory.getSingletonNames().toSet()
            for (beanName in beanNames) {
                val beanDefinition = beanFactory.getBeanDefinition(beanName)
                if (beanDefinition.isSingleton() && instantiatedSingletons.contains(beanName)) {
                    val bean = getBean(beanFactory, beanName)
                    if (bean != null && clear == MockkClear.get(bean)) {
                        io.mockk.clearMocks(bean)
                    }
                }
            }
            try {
                beanFactory.getBean(MockBeans::class.java).clearAll(clear)
            } catch (ex: NoSuchBeanDefinitionException) {
                // Continue
            }
            if (applicationContext.getParent() != null) {
                clearMocks(applicationContext.getParent(), clear)
            }
        }

        private fun getBean(beanFactory: ConfigurableListableBeanFactory, beanName: String): Any? {
            try {
                if (isStandardBeanOrSingletonFactoryBean(beanFactory, beanName)) {
                    return beanFactory.getBean(beanName)
                }
            } catch (ex: Exception) {
                // Continue
            }
            return beanFactory.getSingleton(beanName)
        }

        private fun isStandardBeanOrSingletonFactoryBean(beanFactory: BeanFactory, beanName: String): Boolean {
            val factoryBeanName = BeanFactory.FACTORY_BEAN_PREFIX + beanName
            if (beanFactory.containsBean(factoryBeanName)) {
                val factoryBean = beanFactory.getBean(factoryBeanName) as FactoryBean<*>
                return factoryBean.isSingleton()
            }
            return true
        }

        private val isEnabled: Boolean
            /**
             * Determine if this listener is enabled in the current environment.
             * @see MOCKK_PRESENT
             * @see mockKInitialized
             */
            get() {
                if (!MOCKK_PRESENT) {
                    return false
                }
                var enabled = mockKInitialized
                if (enabled == null) {
                    try {
                        // Invoke isMock() on a non-null object to initialize core MockK classes
                        // in order to reliably determine if this listener is "enabled" both on the
                        // JVM as well as within a GraalVM native image.
                        "a string is not a mock".isMockOrSpy

                        // If we got this far, we assume MockK is usable in the current environment.
                        enabled = true
                    } catch (ex: Throwable) {
                        enabled = false
                        if (logger.isDebugEnabled()) {
                            logger.debug(
                                "MockkClearTestExecutionListener is disabled in the current environment. See exception for details.",
                                ex
                            )
                        }
                    }
                    mockKInitialized = enabled
                }
                return enabled ?: false
            }
    }
}
