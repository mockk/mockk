package io.mockk.proxy;

import io.mockk.agent.MockKAgentException;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.Assert.*;

public class MockKProxyMakerTest {
    MockKProxyMaker maker;

    static boolean[] executed = new boolean[10];

    ListAppendingHandler handler;

    @Before
    public void setUp() throws Exception {
        Arrays.fill(executed, false);
        handler = new ListAppendingHandler();
        maker = new MockKProxyMaker();
        MockKInstrumentation.init();
        MockKInstrumentation.INSTANCE.enable();
    }

    static class A {
        public void a() {
            executed[0] = true;
        }
    }

    @Test
    public void openClassProxy() throws Exception {
        A proxy = maker.proxy(A.class, new Class[0], handler, true, null);

        proxy.a();

        assertFalse(executed[0]);
        checkProxyHandlerCalled(1, proxy, "a");
    }

    @Test
    public void openClassDisabledInstrumentationProxy() throws Exception {
        MockKInstrumentation.INSTANCE.disable();
        A proxy = maker.proxy(A.class, new Class[0], handler, true, null);

        proxy.a();

        assertFalse(executed[0]);
        checkProxyHandlerCalled(1, proxy, "a");
    }

    @Test
    public void openClassCallOriginalProxy() throws Exception {
        handler.callOriginal = true;
        A proxy = maker.proxy(A.class, new Class[0], handler, true, null);

        proxy.a();

        assertTrue(executed[0]);
        checkProxyHandlerCalled(1, proxy, "a");
    }

    static final class B {
        public void a() {
            executed[0] = true;
        }
    }

    @Test
    public void finalClassProxy() throws Exception {
        B proxy = maker.proxy(B.class, new Class[0], handler, true, null);

        proxy.a();

        assertFalse(executed[0]);
        checkProxyHandlerCalled(1, proxy, "a");
    }

    @Test(expected = MockKAgentException.class)
    public void finalClassDisabledInstrumentationProxy() throws Exception {
        MockKInstrumentation.INSTANCE.disable();
        B proxy = maker.proxy(B.class, new Class[0], handler, true, null);

        proxy.a();

        assertFalse(executed[0]);
        checkProxyHandlerCalled(1, proxy, "a");
    }

    @Test
    public void finalClassCallOriginalProxy() throws Exception {
        handler.callOriginal = true;
        B proxy = maker.proxy(B.class, new Class[0], handler, true, null);

        proxy.a();

        assertTrue(executed[0]);
        checkProxyHandlerCalled(1, proxy, "a");
    }

    interface C {
        void a();
    }

    @Test
    public void interfaceProxy() throws Exception {
        C proxy = maker.proxy(C.class, new Class[0], handler, true, null);

        proxy.a();

        assertFalse(executed[0]);
        checkProxyHandlerCalled(1, proxy, "a");
    }

    @Test
    public void interfaceDisabledInstrumentationProxy() throws Exception {
        MockKInstrumentation.INSTANCE.disable();
        C proxy = maker.proxy(C.class, new Class[0], handler, true, null);

        proxy.a();

        assertFalse(executed[0]);
        checkProxyHandlerCalled(1, proxy, "a");
    }

    static abstract class D {
        abstract void a();
    }

    @Test
    public void abstractClassProxy() throws Exception {
        D proxy = maker.proxy(D.class, new Class[0], handler, true, null);

        proxy.a();

        assertFalse(executed[0]);
        checkProxyHandlerCalled(1, proxy, "a");
    }

    @Test
    public void abstractClassDisabledInstrumentationProxy() throws Exception {
        MockKInstrumentation.INSTANCE.disable();
        D proxy = maker.proxy(D.class, new Class[0], handler, true, null);

        proxy.a();

        assertFalse(executed[0]);
        checkProxyHandlerCalled(1, proxy, "a");
    }

    static class E1 extends A {
        public void a() {
            executed[1] = true;
            super.a();
        }
    }

    @Test
    public void openSubClassProxy1() throws Exception {
        E1 proxy = maker.proxy(E1.class, new Class[0], handler, true, null);

        proxy.a();

        assertFalse(executed[0]);
        assertFalse(executed[1]);
        checkProxyHandlerCalled(1, proxy, "a");
    }

    @Test
    public void openSubClassCallOriginalProxy1() throws Exception {
        handler.callOriginal = true;
        E1 proxy = maker.proxy(E1.class, new Class[0], handler, true, null);

        proxy.a();

        assertTrue(executed[0]);
        assertTrue(executed[1]);
        checkProxyHandlerCalled(1, proxy, "a");
    }

    static class E2 extends A {
        public void a() {
            executed[1] = true;
        }
    }

    @Test
    public void openSubClassProxy2() throws Exception {
        E2 proxy = maker.proxy(E2.class, new Class[0], handler, true, null);

        proxy.a();

        assertFalse(executed[0]);
        assertFalse(executed[1]);
        checkProxyHandlerCalled(1, proxy, "a");
    }

    @Test
    public void openSubClassCallOriginalProxy2() throws Exception {
        handler.callOriginal = true;
        E2 proxy = maker.proxy(E2.class, new Class[0], handler, true, null);

        proxy.a();

        assertFalse(executed[0]);
        assertTrue(executed[1]);
        checkProxyHandlerCalled(1, proxy, "a");
    }

    static class E3 extends A {
        public void a() {
            executed[1] = true;
            b();
        }

        public void b() {
            executed[2] = true;
        }
    }

    @Test
    public void openSubClassProxy3() throws Exception {
        E3 proxy = maker.proxy(E3.class, new Class[0], handler, true, null);

        proxy.a();

        assertFalse(executed[0]);
        assertFalse(executed[1]);
        assertFalse(executed[2]);
        checkProxyHandlerCalled(1, proxy, "a");
    }

    @Test
    public void openSubClassCallOriginalProxy3() throws Exception {
        handler.callOriginal = true;
        E3 proxy = maker.proxy(E3.class, new Class[0], handler, true, null);

        proxy.a();

        assertFalse(executed[0]);
        assertTrue(executed[1]);
        assertTrue(executed[2]);
        checkProxyHandlerCalled(2, proxy, "a");
    }

    static class F extends A {
        public void a() {
            executed[1] = true;
            b();
        }

        public void b() {
            executed[2] = true;
        }
    }

    static class G extends F {
        public void a() {
            executed[3] = true;
            super.a();
        }

        public void b() {
            executed[4] = true;
            super.b();
        }
    }

    @Test
    public void openComplexSubClassProxy() throws Exception {
        G proxy = maker.proxy(G.class, new Class[0], handler, true, null);

        proxy.a();

        assertFalse(executed[0]);
        assertFalse(executed[1]);
        assertFalse(executed[2]);
        assertFalse(executed[3]);
        assertFalse(executed[4]);
        checkProxyHandlerCalled(1, proxy, "a");
    }

    @Test
    public void openComplexSubClassCallOriginalProxy() throws Exception {
        handler.callOriginal = true;
        G proxy = maker.proxy(G.class, new Class[0], handler, true, null);

        proxy.a();

        assertFalse(executed[0]);
        assertTrue(executed[1]);
        assertTrue(executed[2]);
        assertTrue(executed[3]);
        assertTrue(executed[4]);
        checkProxyHandlerCalled(2, proxy, "a");
    }

    static abstract class F1 extends A {
        public void a() {
            executed[1] = true;
            b();
        }

        public void b() {
            executed[2] = true;
        }
    }

    static final class G1 extends F1 {
        public void a() {
            executed[3] = true;
            super.a();
        }

        public void b() {
            executed[4] = true;
            super.b();
        }
    }

    @Test
    public void finalComplexSubClassProxy() throws Exception {
        G1 proxy = maker.proxy(G1.class, new Class[0], handler, true, null);

        proxy.a();

        assertFalse(executed[0]);
        assertFalse(executed[1]);
        assertFalse(executed[2]);
        assertFalse(executed[3]);
        assertFalse(executed[4]);
        checkProxyHandlerCalled(1, proxy, "a");
    }

    @Test
    public void finalComplexSubClassCallOriginalProxy() throws Exception {
        handler.callOriginal = true;
        G1 proxy = maker.proxy(G1.class, new Class[0], handler, true, null);

        proxy.a();

        assertFalse(executed[0]);
        assertTrue(executed[1]);
        assertTrue(executed[2]);
        assertTrue(executed[3]);
        assertTrue(executed[4]);
        checkProxyHandlerCalled(2, proxy, "a");
    }

    static class H {
        static void a() {
            executed[0] = true;
        }
    }

    @Test
    public void staticProxy() throws Exception {
        maker.staticProxy(H.class, handler);

        H.a();

        assertFalse(executed[0]);
        checkProxyHandlerCalled(1, H.class, "a");
    }

    @Test
    public void staticCallOriginalProxy() throws Exception {
        handler.callOriginal = true;
        maker.staticProxy(H.class, handler);

        H.a();

        assertTrue(executed[0]);
        checkProxyHandlerCalled(1, H.class, "a");
    }

    private void checkProxyHandlerCalled(int nTimes, Object proxy, String methodName) {
        assertEquals(nTimes, handler.calls.size());
        Call call = handler.calls.get(0);
        assertSame(proxy, call.self);
        assertSame(methodName, call.method.getName());
        assertSame(0, call.method.getParameterTypes().length);
        assertSame(0, call.args.length);
    }

    private static class ListAppendingHandler implements MockKInvocationHandler {
        boolean callOriginal = false;

        Object returnValue = null;

        List<Call> calls = new ArrayList<Call>();

        @Override
        public Object invocation(Object self,
                                 Method method,
                                 Callable<?> originalCall,
                                 Object[] args) throws Exception {

            calls.add(new Call(self, method, args));
            if (callOriginal) {
                return originalCall.call();
            } else {
                return returnValue;
            }
        }
    }

    private static class Call {
        StackTraceElement[] stackTrace;
        Object self;
        Method method;
        Object[] args;

        public Call(Object self, Method method, Object[] args) {
            this.self = self;
            this.method = method;
            this.args = args;
            this.stackTrace = new Exception().getStackTrace();
        }

        @Override
        public String toString() {
            return "Call{method clazz=" + Arrays.toString(this.stackTrace) + '}';
        }
    }
}
