package io.mockk.springmockk

import org.springframework.test.context.ContextConfigurationAttributes
import org.springframework.test.context.ContextCustomizer
import org.springframework.test.context.ContextCustomizerFactory
import org.springframework.test.context.TestContextAnnotationUtils

/**
 * A {@link ContextCustomizerFactory} to add MockK support.
 *
 * @author Phillip Webb
 * @author JB Nizet
 */
class MockkContextCustomizerFactory : ContextCustomizerFactory {
    override fun createContextCustomizer(
        testClass: Class<*>,
        configAttributes: List<ContextConfigurationAttributes>
    ): ContextCustomizer {
        // We gather the explicit mock definitions here since they form part of the
        // MergedContextConfiguration key. Different mocks need to have a different key.
        val parser = DefinitionsParser()
        parseDefinitions(testClass, parser)
        return MockkContextCustomizer(parser.parsedDefinitions)
    }

    private fun parseDefinitions(testClass: Class<*>, parser: DefinitionsParser) {
        parser.parse(testClass)
        if (TestContextAnnotationUtils.searchEnclosingClass(testClass)) {
            parseDefinitions(testClass.enclosingClass, parser)
        }
    }
}
