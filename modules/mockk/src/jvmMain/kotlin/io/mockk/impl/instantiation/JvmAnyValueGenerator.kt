package io.mockk.impl.instantiation

import kotlin.reflect.KClass

open class JvmAnyValueGenerator(
    private val voidInstance: Any,
) : AnyValueGenerator() {
    override fun anyValue(
        cls: KClass<*>,
        isNullable: Boolean,
        orInstantiateVia: () -> Any?,
    ): Any? {
        // For nullable value classes return null directly instead of creating an Objenesis instance.
        // Objenesis zero-initializes the underlying field (e.g. 0L for Long-backed value classes),
        // and maybeUnboxValueForMethodReturn then returns the instance rather than null because
        // unbox-impl() is non-null. This causes the recording state to treat it as a live mock,
        // triggering unnecessary child-mock creation that fails for value classes.
        if (isNullable && runCatching { cls.isValue }.getOrDefault(false)) return null

        return when (cls) {
            Void.TYPE.kotlin -> voidInstance
            Void::class -> voidInstance

            java.lang.Boolean::class -> false
            java.lang.Byte::class -> 0.toByte()
            java.lang.Short::class -> 0.toShort()
            java.lang.Character::class -> 0.toChar()
            java.lang.Integer::class -> 0
            java.lang.Long::class -> 0L
            java.lang.Float::class -> 0.0F
            java.lang.Double::class -> 0.0
            java.lang.Class::class -> Object::class.java

            java.util.List::class -> List<Any>(0) {}
            java.util.Map::class -> HashMap<Any, Any>()
            java.util.Set::class -> HashSet<Any>()
            java.util.ArrayList::class -> ArrayList<Any>()
            java.util.HashMap::class -> HashMap<Any, Any>()
            java.util.HashSet::class -> HashSet<Any>()

            // Optional types are final JDK classes: without a dedicated case they fall
            // through to a mocked/instantiated proxy that fails on any interaction
            // (e.g. Optional.get() throws ClassCastException), so a relaxed mock hands
            // back an empty one. They are matched by runtime name and created
            // reflectively because java.util.Optional exists only from API 24 up: a
            // class-literal reference anywhere in this method fails resolution on
            // API 21 devices with NoClassDefFoundError.
            else -> {
                val orFallback: () -> Any? = {
                    if (cls.java.isArray) {
                        java.lang.reflect.Array
                            .newInstance(cls.java.componentType, 0)
                    } else {
                        orInstantiateVia()
                    }
                }
                when (cls.java.name) {
                    "java.util.Optional" -> jdkStaticEmpty(cls.java.name)
                    "java.util.OptionalInt" -> jdkStaticEmpty(cls.java.name)
                    "java.util.OptionalLong" -> jdkStaticEmpty(cls.java.name)
                    "java.util.OptionalDouble" -> jdkStaticEmpty(cls.java.name)
                    "kotlinx.datetime.Instant" ->
                        kotlinxDatetimeInstantEpoch(cls.java)
                            ?: super.anyValue(cls, isNullable, orFallback)
                    else -> super.anyValue(cls, isNullable, orFallback)
                }
            }
        }
    }

    private fun jdkStaticEmpty(name: String): Any? = Class.forName(name).getMethod("empty").invoke(null)

    /**
     * kotlinx-datetime 0.5.x-0.6.x models `kotlinx.datetime.Instant` on JVM as a final class
     * wrapping a `java.time.Instant` behind an internal constructor. Without a dedicated case the
     * relaxed answer hands back a child mock of it, and that mock is built without running the
     * constructor, so its internal `value` stays null. Passing such a mock to real java.time
     * machinery breaks with ClassCastExceptions (issue #1220), so a relaxed mock hands back a
     * real epoch instance instead, mirroring how the Optional cases above hand back empty ones.
     *
     * The instance is built reflectively because MockK does not depend on kotlinx-datetime. The
     * constructor takes a plain `java.time.Instant`, so no other kotlinx-datetime classes have to
     * resolve: the companion object references kotlinx-serialization and its members may be
     * unusable when kotlinx-serialization-core is off the classpath. kotlinx-datetime 0.7+ turned
     * `Instant` into a value class over a primitive, and those returns are answered through their
     * underlying type, never reaching this branch. Any reflective failure falls back to the
     * default behavior.
     */
    private fun kotlinxDatetimeInstantEpoch(cls: Class<*>): Any? =
        runCatching {
            val constructor = cls.getDeclaredConstructor(java.time.Instant::class.java)
            constructor.isAccessible = true
            constructor.newInstance(java.time.Instant.EPOCH)
        }.getOrNull()
}
