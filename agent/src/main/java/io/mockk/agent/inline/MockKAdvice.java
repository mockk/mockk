package io.mockk.agent.inline;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"unused", "UnusedAssignment"})
public class MockKAdvice {
    public static final ThreadLocal<Object> CALL_SELF = new ThreadLocal<Object>();
    public static final Map<Object, MockKMethodHandler> REGISTRY = new ConcurrentHashMap<Object, MockKMethodHandler>();

    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    private static Callable<?> enter(@Advice.This final Object self,
                                     @Advice.Origin final Method origin,
                                     @Advice.AllArguments final Object[] arguments) throws Throwable {

        if (CALL_SELF.get() == self) {
            return null;
        }
        final MockKMethodHandler handler = REGISTRY.get(self);
        if (handler == null) {
            return null;
        }

        return new Call(handler, self, origin, arguments);
    }

    @Advice.OnMethodExit
    private static void exit(@Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object returned,
                             @Advice.Enter Callable<?> mocked) throws Throwable {
        if (mocked != null) {
            returned = mocked.call();
        }
    }

    public static class HashCode {
        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static boolean enter(@Advice.This Object self) {
            return false;
        }

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

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static boolean enter(@Advice.This Object self) {
            return false;
        }

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

    public static class CallOriginal implements Callable<Object> {
        private final Method origin;
        private final Object mock;
        private final Object[] arguments;

        public CallOriginal(Method origin, Object mock, Object[] arguments) {
            this.origin = origin;
            this.mock = mock;
            this.arguments = arguments;
        }

        @Override
        public Object call() throws Exception {
            Object was = CALL_SELF.get();
            CALL_SELF.set(mock);
            try {
                return origin.invoke(mock, arguments);
            } finally {
                CALL_SELF.set(was);
            }
        }
    }

    public static class Call implements Callable<Object> {
        private final MockKMethodHandler handler;
        private final Object self;
        private final Method origin;
        private final Object[] arguments;

        public Call(MockKMethodHandler handler, Object self, Method origin, Object[] arguments) {
            this.handler = handler;
            this.self = self;
            this.origin = origin;
            this.arguments = arguments;
        }

        @Override
        public Object call() throws Exception {
            return handler.invoke(
                    self,
                    origin,
                    new CallOriginal(origin, self, arguments),
                    arguments);
        }
    }
}
