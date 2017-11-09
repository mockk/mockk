package io.mockk.agent.inline;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public class MockKAdvice {
    @SuppressWarnings("unused")
    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    private static Callable<?> enter(@Advice.This Object mock,
                                     @Advice.Origin Method origin,
                                     @Advice.AllArguments Object[] arguments) throws Throwable {
        return null;
    }

    @SuppressWarnings({"unused", "UnusedAssignment"})
    @Advice.OnMethodExit
    private static void exit(@Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object returned,
                             @Advice.Enter Callable<?> mocked) throws Throwable {
        if (mocked != null) {
            returned = mocked.call();
        }
    }

    public static class HashCode {
        @SuppressWarnings("unused")
        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static boolean enter(@Advice.This Object self) {
            return false;
        }

        @SuppressWarnings({"unused", "UnusedAssignment"})
        @Advice.OnMethodExit
        private static void enter(@Advice.This Object self,
                                  @Advice.Return(readOnly = false) int hashCode,
                                  @Advice.Enter boolean skipped) {
            if (skipped) {
                hashCode = System.identityHashCode(self);
            }
        }
    }

    public static class Equals {

        @SuppressWarnings("unused")
        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static boolean enter(@Advice.This Object self) {
            return false;
        }

        @SuppressWarnings({"unused", "UnusedAssignment"})
        @Advice.OnMethodExit
        private static void enter(@Advice.This Object self,
                                  @Advice.Argument(0) Object other,
                                  @Advice.Return(readOnly = false) boolean equals,
                                  @Advice.Enter boolean skipped) {
            if (skipped) {
                equals = self == other;
            }
        }
    }
}
