package io.mockk.external

internal fun <T> link(className: String, builder: () -> T): T? =
        try {
            Class.forName(className)
            builder()
        } catch (ex: ClassNotFoundException) {
            null
        }

internal inline fun <reified T> newInstance(classLoader: ClassLoader) {
    try {
        classLoader.loadClass(T::class.java.name).newInstance()
    } catch (ex: ClassNotFoundException) {
        null
    }
}

