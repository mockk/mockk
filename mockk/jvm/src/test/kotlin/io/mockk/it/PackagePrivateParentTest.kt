package io.mockk.it

import io.mockk.mockk
import kotlin.test.Test

internal class InternalCls : PackagePrivateCls()

/**
 * Cannot Mock Class With Package-Private Parent.
 * Verifies issue #119.
 */
class PackagePrivateParentTest {
    @Test
    fun mockClassWithPackagePrivateParent() {
        mockk<InternalCls>(relaxed = true)
    }
}
