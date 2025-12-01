package com.ninjasquad.springmockk

import com.ninjasquad.springmockk.example.ExampleServiceCaller
import com.ninjasquad.springmockk.example.SimpleExampleService
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import


/**
 * Config for [SpyBeanOnTestFieldForExistingBeanIntegrationTests] and
 * [SpyBeanOnTestFieldForExistingBeanCacheIntegrationTests]. Extracted to a shared
 * config to trigger caching.
 *
 * @author Phillip Webb
 * @author JB Nizet
 */
@Configuration
@Import(ExampleServiceCaller::class, SimpleExampleService::class)
class SpyBeanOnTestFieldForExistingBeanConfig
