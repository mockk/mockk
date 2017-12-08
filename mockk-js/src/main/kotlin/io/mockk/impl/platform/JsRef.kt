package io.mockk.impl.platform

import io.mockk.Ref

internal class JsRef(override val value: Any) : Ref {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false

        other as JsRef

        return value === other.value
    }

    override fun hashCode(): Int {
        // TODO add JS id to each object
        return 1
    }
}

