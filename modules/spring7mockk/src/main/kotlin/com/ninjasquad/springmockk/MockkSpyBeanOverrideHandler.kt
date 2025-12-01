package com.ninjasquad.springmockk

import io.mockk.spyk
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.core.ResolvableType
import org.springframework.test.context.bean.override.BeanOverrideStrategy
import org.springframework.util.Assert
import org.springframework.util.StringUtils
import java.lang.reflect.Field

/**
 * [BeanOverrideHandler][org.springframework.test.context.bean.override.BeanOverrideHandler] implementation
 * for MockK `spy` support.
 *
 * @author Phillip Webb
 * @author Simon Baslé
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @author Jean-Baptiste Nizet
 */
internal class MockkSpyBeanOverrideHandler(field: Field?, typeToSpy: ResolvableType, spyBean: MockkSpyBean) :
    AbstractMockkBeanOverrideHandler(
        field,
        typeToSpy,
        (if (StringUtils.hasText(spyBean.name)) spyBean.name else null),
        spyBean.contextName,
        BeanOverrideStrategy.WRAP,
        spyBean.clear
    ) {

    constructor(typeToSpy: ResolvableType, spyBean: MockkSpyBean) : this(null, typeToSpy, spyBean)

    protected override fun createOverrideInstance(
        beanName: String,
        existingBeanDefinition: BeanDefinition?,
        existingBeanInstance: Any?
    ): Any {
        checkNotNull(existingBeanInstance) {
            "@MockkSpyBean requires an existing bean instance for bean " + beanName
        }

        return createSpy(beanName, existingBeanInstance)
    }

    private fun createSpy(name: String, instance: Any?): Any {
        SpringMockResolver.rejectUnsupportedSpyTarget(name, instance)
        val resolvedTypeToOverride: Class<*>? = getBeanType().resolve()
        checkNotNull(resolvedTypeToOverride) { "Failed to resolve type to override" }
        checkNotNull(instance)
        Assert.isInstanceOf(resolvedTypeToOverride, instance)

        if (instance.isMockOrSpy) {
            return instance
        }

        // Spring has a special case for JDK proxies here
        // But the code used for proxies in Spring Boot wouldn't be usable here anyway, because it relies on a mocked
        // class with default answers delegating to an instance, but MockK doesn't have such a thing AFAIK.

        return spyk<Any>(name = name, objToCopy = instance).clear(this.clear)
    }
}
