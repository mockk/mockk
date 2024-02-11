package io.mockk.springmockk

import io.mockk.spyk
import org.springframework.core.ResolvableType
import org.springframework.core.style.ToStringCreator
import org.springframework.util.Assert

private const val MULTIPLIER = 31

/**
 * A complete definition that can be used to create a MockK spy.
 *
 * @author Phillip Webb
 * @author JB Nizet
 */
class SpykDefinition(
    name: String? = null,
    val typeToSpy: ResolvableType,
    clear: MockkClear = MockkClear.AFTER,
    qualifier: QualifierDefinition? = null
) : Definition(name, clear, qualifier) {

    fun <T: Any> createSpy(instance: T): T {
        return createSpy(name, instance)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> createSpy(name: String?, instance: T): T {
        requireNotNull(instance) { "Instance must not be null" }
        val resolvedType = typeToSpy.resolve()
        check(resolvedType != null) { "${typeToSpy} cannot be resolved" }
        Assert.isInstanceOf(resolvedType, instance)
        if (instance.isMock) {
            return instance
        }

        // Spring Boot has a special case for JDK proxies here, introduced in commit
        // https://github.com/spring-projects/spring-boot/commit/c8c784bd5ca86faaaecdf2371aa35cf98c62efc5#
        // But the test coming with this commit passed fine with SpringMockK, without introducing any change
        // and the code used for proxies in Spring Boot wouldn't be usable here anyway, because it relies on a mocked
        // class with default answers delegating to an instance, but MockK doesn't have such a thing AFAIK.

        return spyk<Any>(name = name, objToCopy = instance).clear(this.clear) as T
    }

    override fun toString(): String {
        return ToStringCreator(this).append("name", name)
            .append("typeToSpy", this.typeToSpy)
            .append("clear", clear)
            .toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpykDefinition) return false
        if (!super.equals(other)) return false

        if ((typeToSpy as Any) != other.typeToSpy) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = MULTIPLIER * result + typeToSpy.hashCode()
        return result
    }
}
