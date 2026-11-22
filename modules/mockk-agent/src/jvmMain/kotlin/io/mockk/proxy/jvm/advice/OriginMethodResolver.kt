package io.mockk.proxy.jvm.advice

import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

/**
 * Recovers the exact `java.lang.reflect.Method` a generated subclass-proxy method implements
 * when Byte Buddy's cached `@Origin Method` constant cannot tell overloads apart (#1093).
 *
 * Byte Buddy materializes the `@Origin Method` argument of an advice by reflecting the method
 * over its name and parameter types alone; the return type never enters the lookup. Java never
 * declares two methods with the same name and parameter types, so the shortcut is invisible
 * there - but Kotlin legally compiles overloads whose erased parameter types are equal while
 * their return types differ (`invoke(FeatureFlag<Boolean>): Boolean` vs
 * `invoke(FeatureFlag<String>): String`), and for such pairs the generated proxy of the second
 * overload ends up carrying the constant of the first one. The proxy methods themselves are
 * correct (each overload is implemented and dispatched by its own descriptor); only the
 * identity handed to the advice is wrong, which corrupts stubbing and verification of the
 * return-differing sibling.
 *
 * The `@Origin String` binding of the same advice does not go through the reflective lookup:
 * its text is written into the generated class per method at type-writing time, so it always
 * describes the method that really carries it. This resolver keeps the correct `Method` for
 * unambiguous classes (the overwhelmingly common case, at the cost of one cached map lookup
 * per call on them) and, for classes that declare at least one name+parameter group with
 * differing return types, recovers the true method by matching the origin text against the
 * declaring hierarchy. If nothing matches, the incoming method is returned unchanged, which
 * is exactly the pre-existing behavior.
 *
 * Public only because the Java-written advice classes in `io.mockk.proxy.jvm.advice.jvm` call
 * into it; it is not part of MockK's agent API.
 */
object OriginMethodResolver {
    /**
     * `Class#isAmbiguous` flags, cached per declaring class: whether any name+parameter group
     * of the class or its whole hierarchy holds methods with differing return types.
     */
    private val ambiguousClasses = ConcurrentHashMap<Class<*>, Boolean>()

    /** Resolved methods, cached per (declaring class, origin text) pair. */
    private val resolved = ConcurrentHashMap<ResolutionKey, Method>()

    fun resolve(
        method: Method,
        origin: String?,
    ): Method {
        if (origin == null) {
            return method
        }

        val declaring = method.declaringClass
        val isAmbiguous =
            ambiguousClasses.computeIfAbsent(declaring) { hasReturnTypeOnlyOverloads(it) }
        if (!isAmbiguous) {
            return method
        }

        return resolved.computeIfAbsent(ResolutionKey(declaring, origin)) {
            recover(declaring, method, origin) ?: method
        }
    }

    private class ResolutionKey(
        val declaring: Class<*>,
        val origin: String,
    )

    /**
     * Whether the class or anything it inherits from declares two methods that share a name and
     * parameter types but differ in the return type. Only such classes can produce a cross-wired
     * `@Origin Method` constant.
     */
    private fun hasReturnTypeOnlyOverloads(clazz: Class<*>): Boolean {
        var current: Class<*>? = clazz
        while (current != null) {
            if (isAmbiguousIn(current.declaredMethods)) {
                return true
            }
            for (intf in current.interfaces) {
                if (hasReturnTypeOnlyOverloads(intf)) {
                    return true
                }
            }
            current = current.superclass
        }
        return false
    }

    private fun isAmbiguousIn(methods: Array<Method>): Boolean {
        val byNameAndParams = methods.groupBy { it.name to it.parameterTypes.toList() }
        return byNameAndParams.any { (_, group) -> group.map { it.returnType }.distinct().size > 1 }
    }

    /**
     * The declaring hierarchy's method whose `Method.toString()` equals the origin text, or,
     * when the rendered text deviates (e.g. over `throws` clauses), the method with the same
     * name, parameter types and normalized return type.
     */
    private fun recover(
        declaring: Class<*>,
        method: Method,
        origin: String,
    ): Method? {
        candidates(declaring).firstOrNull { it.toString() == origin }?.let { return it }

        val marker = declaring.name + "." + method.name + "("
        val markerIndex = origin.indexOf(marker)
        if (markerIndex < 0) {
            return null
        }
        val renderedReturnType = origin.substring(0, markerIndex).trim().substringAfterLast(' ')
        val expectedReturnType = renderedToClassName(renderedReturnType)

        return candidates(declaring).firstOrNull { candidate ->
            candidate.name == method.name &&
                candidate.parameterTypes.contentEquals(method.parameterTypes) &&
                candidate.returnType.name == expectedReturnType
        }
    }

    private fun candidates(declaring: Class<*>): List<Method> {
        val result = mutableListOf<Method>()
        var current: Class<*>? = declaring
        while (current != null) {
            result.addAll(current.declaredMethods)
            current = current.superclass
        }
        return result
    }

    /**
     * `Method.toString()` renders array types as `java.lang.String[]`, while
     * `Class#getName()` returns `[Ljava.lang.String;` for them; primitives render identically
     * in both. This normalizes the rendered spelling to the `getName()` one.
     */
    private fun renderedToClassName(rendered: String): String {
        if (!rendered.endsWith("[]")) {
            return rendered
        }

        val dims = rendered.length - rendered.replace("[]", "").length
        val dimensions = dims / 2
        val element = rendered.removeSuffix("[]".repeat(dimensions))
        val internal =
            when (element) {
                "boolean" -> "Z"
                "byte" -> "B"
                "char" -> "C"
                "short" -> "S"
                "int" -> "I"
                "long" -> "J"
                "float" -> "F"
                "double" -> "D"
                else -> "L$element;"
            }
        return "[".repeat(dimensions) + internal
    }
}
