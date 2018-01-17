package io.mockk.junit;

import org.junit.runner.Runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Deprecated because inlining is implemented and
 * final classes and methods possible to mock through this technique.
 * So class loading transformation is not needed anymore.
 * <p>
 * Inlining is complex feature, so
 * please report any issues to:
 * https://github.com/oleksiyp/mockk/issues
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Deprecated
public @interface ChainedRunWith {
    Class<? extends Runner> value();
}
