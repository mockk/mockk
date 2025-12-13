package com.ninjasquad.springmockk;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation that aggregates several {@link SpykBean} annotations.
 * <p>
 * Can be used natively, declaring several nested {@link SpykBean} annotations. Can also be
 * used in conjunction with Java 8's support for <em>repeatable annotations</em>, where
 * {@link SpykBean} can simply be declared several times on the same
 * {@linkplain ElementType#TYPE type}, implicitly generating this container annotation.
 *
 * @author Phillip Webb
 * @author JB Nizet
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface SpykBeans {

	/**
	 * Return the contained {@link SpykBean} annotations.
	 * @return the spy beans
	 */
	SpykBean[] value();
}
