package io.mockk.impl.restrict

import io.mockk.MockKSettings
import io.mockk.impl.annotations.MockkRestricted
import io.mockk.impl.annotations.MockkRestrictedMode
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.lang.reflect.Method

class MockingRestrictedExtension : BeforeEachCallback {

    override fun beforeEach(context: ExtensionContext) {
        val testMethod: Method? = context.testMethod.orElse(null)
        val testClass = context.testClass.orElse(null)

        MockKSettings.setDisallowMockingRestrictedClasses(false)
        RestrictedMockClasses.resetUserDefinedRestrictedTypes()

        val annotation = testMethod?.getAnnotation(MockkRestricted::class.java)
            ?: testClass?.getAnnotation(MockkRestricted::class.java)

        annotation?.let {
            when (it.mode) {
                MockkRestrictedMode.EXCEPTION -> MockKSettings.setDisallowMockingRestrictedClasses(true)
                MockkRestrictedMode.WARN -> MockKSettings.setDisallowMockingRestrictedClasses(false)
            }

            val restrictedClasses = it.restricted.map { clazz -> clazz.java }
            RestrictedMockClasses.setUserDefinedRestrictedTypes(restrictedClasses)
        }
    }
}

