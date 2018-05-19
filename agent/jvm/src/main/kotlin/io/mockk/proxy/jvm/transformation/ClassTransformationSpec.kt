package io.mockk.proxy.jvm.transformation

data class ClassTransformationSpec(
    val cls: Class<*>,
    val simpleIntercept: Int = 0,
    val staticIntercept: Int = 0,
    val constructorIntercept: Int = 0
) {
    val shouldDoSimpleIntercept: Boolean
        get() = simpleIntercept > 0

    val shouldDoStaticIntercept: Boolean
        get() = staticIntercept > 0

    val shouldDoConstructorIntercept: Boolean
        get() = constructorIntercept > 0

    private data class Categories(val simple: Boolean, val static: Boolean, val constructor: Boolean)

    private fun categories() = Categories(
        shouldDoSimpleIntercept,
        shouldDoStaticIntercept,
        shouldDoConstructorIntercept
    )

    infix fun sameTransforms(other: ClassTransformationSpec) = categories() == other.categories()

    fun isEmpty() =
        simpleIntercept == 0
                && staticIntercept == 0
                && constructorIntercept == 0
}