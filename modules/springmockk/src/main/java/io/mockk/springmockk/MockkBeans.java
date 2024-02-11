package io.mockk.springmockk;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation that aggregates several {@link MockkBean} annotations.
 * <p>
 * Can be used natively, declaring several nested {@link MockkBean} annotations. Can also
 * be used in conjunction with Java 8's support for <em>repeatable annotations</em>, where
 * {@link MockkBean} can simply be declared several times on the same
 * {@linkplain ElementType#TYPE type}, implicitly generating this container annotation.
 *
 * @author Phillip Webb
 * @author JB Nizet
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface MockkBeans {

    /**
     * Return the contained {@link MockkBean} annotations.
     *
     * @return the mockk beans
     */
    MockkBean[] value();

}
