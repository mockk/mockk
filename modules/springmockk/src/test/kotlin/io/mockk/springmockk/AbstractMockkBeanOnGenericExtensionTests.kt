package io.mockk.springmockk

import io.mockk.springmockk.AbstractMockkBeanOnGenericTests.SomethingImpl
import io.mockk.springmockk.AbstractMockkBeanOnGenericTests.ThingImpl

/**
 * Concrete implementation of [AbstractMockkBeanOnGenericTests].
 *
 * @author Madhura Bhave
 * @author JB Nizet
 */
class AbstractMockkBeanOnGenericExtensionTests : AbstractMockkBeanOnGenericTests<ThingImpl, SomethingImpl>()
