package io.mockk.proxy.android

internal class MethodDescriptor(
    signature: String
) {
    private val matchResult = methodPattern.matchEntire(signature)
            ?: throw IllegalArgumentException()

    val className = matchResult.groupValues[1]
    val methodName = matchResult.groupValues[2]
    val methodParamTypes = matchResult.groupValues[3]
        .split(",")
        .dropLastWhile { it.isEmpty() }
        .filter { it != "" }
        .map(Util::nameToType)
        .toTypedArray()

    override fun toString() = "$className#$methodName"

    companion object {
        private val methodPattern = Regex("(.*)#(.*)\\((.*)\\)")
    }
}
