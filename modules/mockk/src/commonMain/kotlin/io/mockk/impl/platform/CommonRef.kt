package io.mockk.impl.platform

import io.mockk.InternalPlatformDsl
import io.mockk.impl.InternalPlatform
import io.mockk.impl.Ref

class CommonRef(override val value: Any) : Ref {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Ref) return false
        return value === other.value
    }

    override fun hashCode(): Int =
        if (InternalPlatform.isPassedByValue(value::class)) {
            value.hashCode()
        } else {
            InternalPlatformDsl.identityHashCode(value)
        }

    override fun toString(): String = "Ref(${value::class.simpleName}@${InternalPlatform.hkd(value)})"
}
