package io.mockk.impl.recording

import io.mockk.MethodDescription

object WasNotCalled {
    val method = MethodDescription(
        name = "wasNot Called",
        returnType = Unit::class,
        returnTypeNullable = false,
        returnsUnit = true,
        returnsNothing = false,
        isSuspend = false,
        isFnCall = false,
        declaringClass = Unit::class,
        paramTypes = listOf(),
        varArgsArg = -1,
        privateCall = false
    )
}
