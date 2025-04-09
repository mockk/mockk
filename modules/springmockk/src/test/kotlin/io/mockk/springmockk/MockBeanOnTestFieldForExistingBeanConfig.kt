package io.mockk.springmockk

import io.mockk.springmockk.example.ExampleServiceCaller
import io.mockk.springmockk.example.FailingExampleService
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import


/**
 * Config for [MockBeanOnTestFieldForExistingBeanIntegrationTests] and
 * [MockBeanOnTestFieldForExistingBeanCacheIntegrationTests]. Extracted to a shared
 * config to trigger caching.
 *
 * @author Phillip Webb
 * @author JB Nizet
 */
@Configuration
@Import(ExampleServiceCaller::class, FailingExampleService::class)
class MockBeanOnTestFieldForExistingBeanConfig
