package io.mockk.jvm

import io.mockk.Ref

class JvmRef(override val value: Any) : Ref {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Ref) return false
        return value === other.value
    }

    override fun hashCode(): Int = System.identityHashCode(value)
    override fun toString(): String = "Ref(${value::class.simpleName}@${hashCode()})"
}