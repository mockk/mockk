package io.mockk.junit;

import org.junit.runner.Runner;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ChainedRunWith {
    Class<? extends Runner> value();
}
