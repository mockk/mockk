package io.mockk;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@AnnotatedJavaCls.Marker
public abstract class AnnotatedJavaCls {
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Marker {
    }

    public String hello() {
        return "hello";
    }
}
