package io.mockk.impl

import io.mockk.InternalPlatformDsl
import io.mockk.MockKException
import io.mockk.StackElement
import io.mockk.impl.platform.CommonIdentityHashMapOf
import io.mockk.impl.platform.CommonRef
import io.mockk.impl.platform.JvmWeakConcurrentMap
import java.lang.ref.WeakReference
import java.lang.reflect.Modifier
import java.util.*
import java.util.Collections.synchronizedList
import kotlin.reflect.KClass
import kotlin.reflect.full.cast

actual object InternalPlatform {
    actual fun time(): Long = System.nanoTime()

    actual fun ref(obj: Any): Ref = CommonRef(obj)

    actual fun hkd(obj: Any): String = Integer.toHexString(InternalPlatformDsl.identityHashCode(obj))

    actual fun isPassedByValue(cls: KClass<*>): Boolean {
        return when (cls) {
            java.lang.Boolean::class -> true
            java.lang.Byte::class -> true
            java.lang.Short::class -> true
            java.lang.Character::class -> true
            java.lang.Integer::class -> true
            java.lang.Long::class -> true
            java.lang.Float::class -> true
            java.lang.Double::class -> true
            java.lang.String::class -> true
            else -> false
        }
    }

    actual fun <K, V> MutableMap<K, V>.customComputeIfAbsent(key: K, valueFunc: (K) -> V): V {
        val value = get(key)
        return if (value == null) {
            val newValue = valueFunc(key)
            put(key, newValue)
            newValue
        } else {
            value
        }
    }


    actual fun <K, V> weakMap(): MutableMap<K, V> = JvmWeakConcurrentMap()

    actual fun <K, V> identityMap(): MutableMap<K, V> = CommonIdentityHashMapOf()

    actual fun <T> synchronizedMutableList(): MutableList<T> {
        return synchronizedList(mutableListOf<T>())
    }

    actual fun <K, V> synchronizedMutableMap(): MutableMap<K, V> = Collections.synchronizedMap(hashMapOf())

    actual fun packRef(arg: Any?): Any? {
        return if (arg == null || isPassedByValue(arg::class))
            arg
        else
            ref(arg)
    }

    actual fun prettifyRecordingException(ex: Throwable): Throwable {
        return when {
            ex is ClassCastException ->
                MockKException(
                    when {
                        ex.message == null ->
                            "Class cast exception happened.\n" +
                                    "WARN: 'message' property in ClassCastException provided by JVM is null, autohinting is not possible. \n" +
                                    "This is most probably happening due to Java optimization enabled. \n" +
                                    "You can use `hint` before call or use -XX:-OmitStackTraceInFastThrow to disable this optimization behaviour and make autohiniting work. \n" +
                                    "For example in gradle use: \n" +
                                    "\n" +
                                    "test {\n" +
                                    "   jvmArgs '-XX:-OmitStackTraceInFastThrow'\n" +
                                    "}"
                        else -> "Class cast exception happened.\n" +
                                "Probably type information was erased.\n" +
                                "In this case use `hint` before call to specify " +
                                "exact return type of a method.\n"
                    }, ex
                )

            ex is NoClassDefFoundError &&
                    ex.message?.contains("kotlinx/coroutines/") ?: false ->
                MockKException(
                    "Add coroutines support artifact 'org.jetbrains.kotlinx:kotlinx-coroutines-core' to your project ",
                    ex
                )

            else -> ex
        }
    }

    actual fun <T : Any> copyFields(to: T, from: T) {
        fun copy(to: Any, from: Any, cls: Class<*>) {
            for (field in cls.declaredFields) {
                if (Modifier.isStatic(field.modifiers)) {
                    continue
                }
                if (isRunningAndroidInstrumentationTest() && field.name.startsWith("shadow$")) {
                    continue
                }
                InternalPlatformDsl.makeAccessible(field)
                val value = field.get(from)
                field.set(to, value)
            }
            if (cls.superclass != null) {
                copy(to, from, cls.superclass)
            }
        }
        copy(to, from, from::class.java)
    }

    actual fun captureStackTrace(): () -> List<StackElement> {
        val ex = Exception("Stack trace")
        return {
            val stack = ex.stackTrace ?: arrayOf<StackTraceElement>()
            stack.map {
                StackElement(
                    it.className ?: "-",
                    it.fileName ?: "-",
                    it.methodName ?: "-",
                    it.lineNumber,
                    it.isNativeMethod
                )
            }
        }
    }

    actual fun weakRef(value: Any): WeakRef {
        val weakRef = WeakReference<Any>(value)
        return object : WeakRef {
            override val value: Any?
                get() = weakRef.get()
        }
    }

    actual fun multiNotifier(): MultiNotifier = JvmMultiNotifier()

    inline fun <reified T : Any> loadPlugin(className: String, msg: String = "") =
        try {
            T::class.cast(Class.forName(className).newInstance())
        } catch (ex: Exception) {
            throw MockKException("Failed to load plugin. $className $msg", ex)
        }


    fun isRunningAndroidInstrumentationTest(): Boolean {
        return System.getProperty("java.vendor", "")
            .toLowerCase(Locale.US)
            .contains("android")
    }

}
