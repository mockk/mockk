package com.ninjasquad.springmockk

import org.springframework.test.context.bean.override.BeanOverrideHandler
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.staticFunctions
import kotlin.reflect.jvm.isAccessible


/**
 * Test utilities for Bean Overrides.
 *
 * @author Sam Brannen
 * @since 6.2.2
 */
object BeanOverrideTestUtils {

    fun findHandlers(testClass: Class<*>): List<BeanOverrideHandler> {
        return BeanOverrideHandler.forTestClass(testClass)
    }

    fun findAllHandlers(testClass: Class<*>): List<BeanOverrideHandler> {
        val findAllHandlers: KFunction<*> = BeanOverrideHandler::class.staticFunctions.first { it.name == "findAllHandlers" }
        findAllHandlers.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return findAllHandlers.call(testClass) as List<BeanOverrideHandler>
    }

}
