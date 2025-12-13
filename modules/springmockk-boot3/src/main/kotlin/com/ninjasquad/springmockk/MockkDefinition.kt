package com.ninjasquad.springmockk

import io.mockk.mockkClass
import org.springframework.core.ResolvableType
import org.springframework.core.style.ToStringCreator
import java.util.*
import kotlin.reflect.KClass

private const val MULTIPLER = 31

/**
 * A complete definition that can be used to create a MockK mock.
 *
 * @author Phillip Webb
 * @author JB Nizet
 */
class MockkDefinition(
    name: String? = null,
    val typeToMock: ResolvableType,
    extraInterfaces: Array<KClass<*>> = emptyArray(),
    clear: MockkClear = MockkClear.AFTER,
    val relaxed: Boolean = false,
    val relaxUnitFun: Boolean = false,
    qualifier: QualifierDefinition? = null
) : Definition(name, clear, qualifier) {

    val extraInterfaces: Set<KClass<*>> = Collections.unmodifiableSet(LinkedHashSet(extraInterfaces.toList()))

    fun <T: Any> createMock(): T {
        return createMock<T>(name)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> createMock(name: String?): T {
        val resolvedType = typeToMock.resolve()
        check(resolvedType != null) { "${typeToMock} cannot be resolved" }
        return mockkClass(
            type = resolvedType.kotlin as KClass<T>,
            name = name,
            moreInterfaces = extraInterfaces.toTypedArray(),
            relaxed = relaxed,
            relaxUnitFun = relaxUnitFun
        ).clear(this.clear)
    }

    override fun toString(): String {
        return ToStringCreator(this).append("name", this.name)
            .append("typeToMock", this.typeToMock)
            .append("extraInterfaces", this.extraInterfaces)
            .append("clear", clear).toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MockkDefinition) return false
        if (!super.equals(other)) return false

        if ((this.typeToMock as Any) != other.typeToMock) return false
        if (extraInterfaces != other.extraInterfaces) return false
        if (relaxed != other.relaxed) return false
        if (relaxUnitFun != other.relaxUnitFun) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = MULTIPLER * result + typeToMock.hashCode()
        result = MULTIPLER * result + extraInterfaces.hashCode()
        result = MULTIPLER * result + relaxed.hashCode()
        result = MULTIPLER * result + relaxUnitFun.hashCode()
        return result
    }
}
