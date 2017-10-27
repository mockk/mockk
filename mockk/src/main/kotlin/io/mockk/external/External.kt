package io.mockk.external

internal fun <T> link(className: String, builder: () -> T): T? =
        try {
            Class.forName(className)
            builder()
        } catch (ex: ClassNotFoundException) {
            null
        }

