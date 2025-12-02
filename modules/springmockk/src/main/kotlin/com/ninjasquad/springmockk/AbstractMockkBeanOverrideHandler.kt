package com.ninjasquad.springmockk

import org.springframework.beans.factory.config.SingletonBeanRegistry
import org.springframework.core.ResolvableType
import org.springframework.core.style.ToStringCreator
import org.springframework.test.context.bean.override.BeanOverrideHandler
import org.springframework.test.context.bean.override.BeanOverrideStrategy
import java.lang.reflect.Field
import kotlin.reflect.jvm.jvmName

/**
 * Abstract base {@link BeanOverrideHandler} implementation for MockK.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @author Jean-Baptiste Nizet
 */
internal abstract class AbstractMockkBeanOverrideHandler : BeanOverrideHandler {

    internal val clear: MockkClear;

    protected constructor(
        field: Field?,
        beanType: ResolvableType,
        beanName: String?,
        contextName: String,
        strategy: BeanOverrideStrategy,
        clear: MockkClear?
    ) : super(field, beanType, beanName, contextName, strategy) {
        this.clear = clear ?: MockkClear.AFTER
    }

    override fun trackOverrideInstance(
        mock: Any,
        trackingBeanRegistry: SingletonBeanRegistry
    ) {
        getMockkBeans(trackingBeanRegistry).add(mock);
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true;
        }
        return (other is AbstractMockkBeanOverrideHandler && super.equals(other) &&
                this.clear == other.clear);
    }

    override fun hashCode(): Int {
        return super.hashCode() + this.clear.hashCode();
    }

    override fun toString(): String {
        return ToStringCreator(this)
            .append("field", field)
            .append("beanType", beanType)
            .append("beanName", beanName)
            .append("contextName", contextName)
            .append("strategy", strategy)
            .append("clear", clear)
            .toString();
    }

    companion object {
        private fun getMockkBeans(trackingBeanRegistry: SingletonBeanRegistry): MockBeans {
            val beanName = MockBeans::class.jvmName;
            var mockBeans: MockBeans? = null;
            if (trackingBeanRegistry.containsSingleton(beanName)) {
                mockBeans = trackingBeanRegistry.getSingleton(beanName) as MockBeans;
            }
            if (mockBeans == null) {
                mockBeans = MockBeans();
                trackingBeanRegistry.registerSingleton(beanName, mockBeans);
            }
            return mockBeans;
        }
    }
}
