package com.ninjasquad.springmockk

import org.springframework.aop.TargetSource
import org.springframework.aop.framework.Advised
import org.springframework.aop.support.AopUtils
import org.springframework.util.ClassUtils


/**
 * A [MockResolver] for testing Spring applications with MockK.
 *
 * Unlike its Mockito counterpart, it doesn't actually resolve anything.
 * It's juste there to reject illegal spy targets.
 *
 * @author Sam Brannen
 * @author Andy Wilkinson
 * @author Juergen Hoeller
 * @author Jean-Baptiste Nizet
 */
internal class SpringMockResolver {
    companion object {
        val SPRING_AOP_PRESENT: Boolean = ClassUtils.isPresent(
            "org.springframework.aop.framework.Advised", SpringMockResolver::class.java.getClassLoader()
        )

        /**
         * Reject the supplied bean if it is not a supported candidate to spy on.
         *
         * Specifically, this method ensures that the bean is not a Spring AOP proxy
         * with a non-static [TargetSource].
         * @param beanName the name of the bean to spy on
         * @param bean the bean to spy on
         * @see getUltimateTargetObject
         */
        fun rejectUnsupportedSpyTarget(beanName: String?, bean: Any?) {
            if (SPRING_AOP_PRESENT) {
                check(!(AopUtils.isAopProxy(bean) && bean is Advised && !bean.getTargetSource().isStatic())) {
                    "@MockkSpyBean cannot be applied to bean '$beanName', because it is a Spring AOP proxy with a non-static TargetSource. Perhaps you have attempted to spy on a scoped proxy, which is not supported."
                }
            }
        }
    }
}
