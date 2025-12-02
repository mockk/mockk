package com.ninjasquad.springmockk

import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ContextCustomizer
import org.springframework.test.context.MergedContextConfiguration

/**
 * A {@link ContextCustomizer} to add MockK support.
 *
 * @author Phillip Webb
 * @author JB Nizet
 */
data class MockkContextCustomizer(private val definitions: Set<Definition>) : ContextCustomizer {

    override fun customizeContext(
        context: ConfigurableApplicationContext,
        mergedContextConfiguration: MergedContextConfiguration
    ) {
        if (context is BeanDefinitionRegistry) {
            MockkPostProcessor.register(
                registry = context as BeanDefinitionRegistry,
                definitions = this.definitions
            )
        }
    }
}
