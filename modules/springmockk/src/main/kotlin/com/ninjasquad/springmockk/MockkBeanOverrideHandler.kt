package com.ninjasquad.springmockk

import io.mockk.mockkClass
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.core.ResolvableType
import org.springframework.core.style.ToStringCreator
import org.springframework.test.context.bean.override.BeanOverrideStrategy
import java.lang.reflect.Field
import java.util.*
import kotlin.reflect.KClass


/**
 * [BeanOverrideHandler][org.springframework.test.context.bean.override.BeanOverrideHandler] implementation
 * for MockK `mock` support.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @author Jean-Baptiste Nizet
 */
internal class MockkBeanOverrideHandler private constructor(
    field: Field?,
    typeToMock: ResolvableType,
    beanName: String?,
    contextName: String,
    strategy: BeanOverrideStrategy,
    clear: MockkClear?,
    extraInterfaces: Array<KClass<*>>,
    val relaxed: Boolean,
    val relaxUnitFun: Boolean
) : AbstractMockkBeanOverrideHandler(field, typeToMock, beanName, contextName, strategy, clear) {

    /**
     * Return the extra interfaces.
     * @return the extra interfaces or an empty set
     */
    internal val extraInterfaces: Set<KClass<*>> = extraInterfaces.toSet()

    internal constructor(typeToMock: ResolvableType, mockkBean: MockkBean) : this(null, typeToMock, mockkBean)

    internal constructor(field: Field?, typeToMock: ResolvableType, mockkBean: MockkBean) : this(
        field,
        typeToMock,
        if (!mockkBean.name.isBlank()) mockkBean.name else null,
        mockkBean.contextName,
        if (mockkBean.enforceOverride) BeanOverrideStrategy.REPLACE else BeanOverrideStrategy.REPLACE_OR_CREATE,
        mockkBean.clear,
        mockkBean.extraInterfaces,
        mockkBean.relaxed,
        mockkBean.relaxUnitFun
    )

    protected override fun createOverrideInstance(
        beanName: String,
        existingBeanDefinition: BeanDefinition?,
        existingBeanInstance: Any?
    ): Any {
        return createMock(beanName)
    }

    private fun <T> createMock(name: String): T {
        val targetType = beanType.resolve()
        check(targetType != null) { "${beanType} cannot be resolved" }
        @Suppress("UNCHECKED_CAST")
        return mockkClass(
            type = targetType.kotlin,
            name = name,
            moreInterfaces = extraInterfaces.toTypedArray(),
            relaxed = relaxed,
            relaxUnitFun = relaxUnitFun
        ).clear(this.clear) as T
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (javaClass != other?.javaClass) return false
        other as MockkBeanOverrideHandler

        return super.equals(other) &&
                this.relaxed == other.relaxed && this.relaxUnitFun == other.relaxUnitFun &&
                this.extraInterfaces == other.extraInterfaces
    }

    override fun hashCode(): Int {
        return super.hashCode() + Objects.hash(this.extraInterfaces, this.relaxed, this.relaxUnitFun)
    }

    override fun toString(): String {
        return ToStringCreator(this)
            .append("field", field)
            .append("beanType", beanType)
            .append("beanName", beanName)
            .append("contextName", contextName)
            .append("strategy", strategy)
            .append("clear", clear)
            .append("extraInterfaces", extraInterfaces)
            .append("relaxed", relaxed)
            .append("relaxUnitFun", relaxUnitFun)
            .toString()
    }
}
