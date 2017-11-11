package io.mockk.agent.inline;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"unused", "UnusedAssignment"})
public class MockKAdvice extends MockKDispatcher {
    public static final ThreadLocal<Object> CALL_SELF = new ThreadLocal<Object>();
    public static final WeakConcurrentHashMap<Object, MockKMethodHandler> REGISTRY = new WeakConcurrentHashMap<Object, MockKMethodHandler>();

    private static final Random RNG = new Random();
    private long id = RNG.nextLong();

    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    private static Callable<?> enter(@Id long id,
                                     @Advice.This final Object self,
                                     @Advice.Origin final Method origin,
                                     @Advice.AllArguments final Object[] arguments) throws Throwable {
        MockKDispatcher dispatcher = MockKDispatcher.get(id, self);
        if (dispatcher == null) {
            return null;
        }
        return dispatcher.handle(self, origin, arguments);

    }

    public long getId() {
        return id;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface Id {
    }

    @Advice.OnMethodExit
    private static void exit(@Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object returned,
                             @Advice.Enter Callable<?> mocked) throws Throwable {
        if (mocked != null) {
            returned = mocked.call();
        }
    }

    @Override
    public Callable<?> handle(Object self, Method origin, Object[] arguments) throws Exception {
        if (self == REGISTRY ||
                self == CALL_SELF) {
            return null;
        }

        final MockKMethodHandler handler = REGISTRY.get(self);
        if (handler == null) {
            return null;
        }

        if (CALL_SELF.get() == self) {
            return null;
        }
        CALL_SELF.set(null);

        return new Call(handler, self, origin, arguments);
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

    static void registerHandler(Object instance, MockKMethodHandler handler) {
        REGISTRY.put(instance, handler);
    }
}
