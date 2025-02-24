package io.mockk.springmockk

private const val MULTIPLIER = 31

/**
 * Base class for [MockkDefinition] and [SpykDefinition].
 *
 * @author Phillip Webb
 * @author JB Nizet
 * @see DefinitionsParser
 */
open class Definition(
    val name: String?,
    val clear: MockkClear,
    val qualifier: QualifierDefinition?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Definition) return false

        if (name != other.name) return false
        if (clear != other.clear) return false
        if (qualifier != other.qualifier) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = MULTIPLIER * result + clear.hashCode()
        result = MULTIPLIER * result + (qualifier?.hashCode() ?: 0)
        return result
    }
}
