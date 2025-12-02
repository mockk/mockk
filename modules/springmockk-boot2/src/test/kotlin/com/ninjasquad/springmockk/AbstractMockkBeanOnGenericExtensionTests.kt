package com.ninjasquad.springmockk

import com.ninjasquad.springmockk.AbstractMockkBeanOnGenericTests.SomethingImpl
import com.ninjasquad.springmockk.AbstractMockkBeanOnGenericTests.ThingImpl

/**
 * Concrete implementation of [AbstractMockkBeanOnGenericTests].
 *
 * @author Madhura Bhave
 * @author JB Nizet
 */
class AbstractMockkBeanOnGenericExtensionTests : AbstractMockkBeanOnGenericTests<ThingImpl, SomethingImpl>()
