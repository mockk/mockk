package io.mockk.proxy.android.test

import org.junit.Assume
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class MinSdk(
    val value: Int,
)

class MinSdkRule : TestRule {
    override fun apply(
        base: Statement,
        description: Description,
    ): Statement =
        object : Statement() {
            override fun evaluate() {
                val annotation = description.getAnnotation(MinSdk::class.java)
                if (annotation != null) {
                    val currentSdk = android.os.Build.VERSION.SDK_INT
                    Assume.assumeTrue(
                        "Test requires SDK ${annotation.value}, but current is $currentSdk",
                        currentSdk >= annotation.value,
                    )
                }
                base.evaluate()
            }
        }
}
